package sbtorgpolicies.settings

import sbt.Keys.{packageOptions, version}
import sbt.Package.ManifestAttributes
import sbt.{ProcessLogger, Project, Setting, State}
import sbtorgpolicies.github.GitHubOps
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.{Git, Utilities, Vcs}

trait release extends keys with bashKeys {
  import Utilities._

  lazy val orgInquireVersions: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val releaseV = extracted.get(version)
    val nextFunc = extracted.get(releaseNextVersion)

    val nextV = nextFunc(releaseV)

    st.put(versions, (releaseV, nextV))
  }

  lazy val orgTagRelease: ReleaseStep = { st: State =>
    val ghOps: GitHubOps = st.extract.get(orgGithubOps)

    def findTag(tag: String): Option[String] = {
      if (ghOps.fetchReference(s"tags/$tag").isRight) {
        sys.error("Tag [%s] already exists. Aborting release!" format tag)
        None
      } else {
        Some(tag)
      }
    }

    val (tagState, tag)         = st.extract.runTask(releaseTagName, st)
    val (commentState, comment) = st.extract.runTask(releaseTagComment, tagState)
    val tagToUse                = findTag(tag)
    val branch                  = st.extract.get(orgCommitBranchSetting)
    tagToUse.foreach(ghOps.createTagHeadCommit(branch, _, comment))

    tagToUse map (t =>
      reapply(
        Seq[Setting[_]](
          packageOptions += ManifestAttributes("Vcs-Release-Tag" -> t)
        ),
        commentState)) getOrElse commentState
  }

  lazy val orgPushChanges: ReleaseStep = ReleaseStep(orgPushChangesAction, orgCheckUpstream)

  private[this] lazy val orgPushChangesAction = { st: State =>
    val vc = vcs(st)
    if (vc.hasUpstream) {
      val processLogger: ProcessLogger = if (vc.isInstanceOf[Git]) {
        vc.stdErrorToStdOut(st.log)
      } else st.log
      vc.pushChanges !! processLogger
    } else {
      st.log.info("Changes were NOT pushed, because no upstream branch is configured for the local branch [%s]" format vcs(
        st).currentBranch)
    }
    st
  }

  private[this] lazy val orgCheckUpstream = { st: State =>
    if (!vcs(st).hasUpstream) {
      sys.error(
        "No tracking branch is set up. Either configure a remote tracking branch, or remove the pushChanges release part.")
    }

    st.log.info("Checking remote [%s] ..." format vcs(st).trackingRemote)
    if (vcs(st).checkRemote(vcs(st).trackingRemote) ! st.log != 0) {
      sys.error("Aborting the release!")
    }

    if (vcs(st).isBehindRemote) {
      sys.error("Merge the upstream commits and run `release` again.")
    }
    st
  }

  private[this] def vcs(st: State): Vcs =
    st.extract
      .get(releaseVcs)
      .getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))

}
