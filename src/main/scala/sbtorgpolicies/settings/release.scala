package sbtorgpolicies.settings

import sbt.Keys.version
import sbt.{ProcessLogger, Project, SimpleReader, State}
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.{Git, Utilities, Vcs}

trait release {
  import Utilities._

  private def vcs(st: State): Vcs =
    st.extract
      .get(releaseVcs)
      .getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))

  lazy val orgInquireVersions: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val useDefs  = st.get(useDefaults).getOrElse(false)
    val releaseV = extracted.get(version)

    val nextFunc = extracted.get(releaseNextVersion)
    val nextV    = nextFunc(releaseV)

    st.put(versions, (releaseV, nextV))
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

}
