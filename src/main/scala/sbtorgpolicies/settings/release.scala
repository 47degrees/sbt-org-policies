/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtorgpolicies.settings

import cats.effect.IO
import org.joda.time.{DateTime, DateTimeZone}
import sbt.Keys.{baseDirectory, packageOptions, version}
import sbt.Package.ManifestAttributes
import sbt.{File, LocalRootProject, Project, Setting, State}
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.io.FileHelper
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.templates._
import sbtrelease.ReleasePlugin.autoImport.ReleaseKeys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.{Utilities, Vcs}
import github4s.domain.Ref

trait release {
  import Utilities._

  val orgVersionCommitMessage: String = "Setting version"

  lazy val orgInitialVcsChecks: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val hasUntrackedFiles = vcs(st).hasUntrackedFiles
    val hasModifiedFiles  = vcs(st).hasModifiedFiles
    if (hasModifiedFiles) sys.error("Aborting release: unstaged modified files")
    if (hasUntrackedFiles && !extracted.get(releaseIgnoreUntrackedFiles)) {
      sys.error(
        "Aborting release: untracked files. Remove them or specify 'releaseIgnoreUntrackedFiles := true' in settings"
      )
    }

    st.log.info("Starting release process off commit: " + vcs(st).currentHash)
    st
  }

  lazy val orgInquireVersions: ReleaseStep = { st: State =>
    val extracted = Project.extract(st)

    val releaseV = extracted.get(version)
    val nextFunc = extracted.runTask(releaseNextVersion, st)._2

    val nextV = nextFunc(releaseV)

    st.put(versions, (releaseV, nextV))
  }

  lazy val orgTagRelease: ReleaseStep = { st: State =>
    val ghOps: GitHubOps[IO] = st.extract.get(orgGithubOpsSetting)

    def checkTagDoesNotExist(tag: String): Unit = {
      // This returns a list of tags that start with the given tag name.
      // We have to filter to get only the exact tag we are looking for.
      val refs = ghOps.fetchReference(s"tags/$tag").value.unsafeRunSync()
      if (refs.fold[List[Ref]](_ => Nil, _.filter(_.ref.endsWith(tag))).nonEmpty) {
        sys.error("Tag [%s] already exists. Aborting release!" format tag)
      }
    }

    val (tagState, tag)            = st.extract.runTask(releaseTagName, st)
    val (commentState, tagComment) = st.extract.runTask(releaseTagComment, tagState)
    checkTagDoesNotExist(tag)
    val branch = st.extract.get(orgCommitBranchSetting)
    val file   = st.extract.get(releaseVersionFile)

    val releaseDescription =
      ghOps
        .latestPullRequests(branch, file.getName, orgVersionCommitMessage)
        .value
        .unsafeRunSync() match {
        case Right(Nil) => s"* $tagComment"
        case Right(list) =>
          list map { pr =>
            val prTitle = pr.title.replace(s" (#${pr.number})", "")
            s"* $prTitle ([#${pr.number}](${pr.html_url}))"
          } mkString "\n"
        case Left(e) =>
          e.printStackTrace()
          sys.error(
            "Tag release process couldn't fetch the pull request list from Github. Aborting release!"
          )
      }

    ghOps.createTagRelease(branch, tag, tagComment, releaseDescription)

    reapply(
      Seq[Setting[_]](
        releaseTagComment := releaseDescription,
        packageOptions += ManifestAttributes("Vcs-Release-Tag" -> tag)
      ),
      commentState
    )
  }

  lazy val orgUpdateChangeLog: ReleaseStep = { st: State =>
    val ghOps: GitHubOps[IO] = st.extract.get(orgGithubOpsSetting)
    val fh                   = new FileHelper

    val (_, comment)    = st.extract.runTask(releaseTagComment, st)
    val branch          = st.extract.get(orgCommitBranchSetting)
    val commitMessage   = st.extract.get(orgCommitMessageSetting)
    val baseDir         = st.extract.get(baseDirectory in LocalRootProject)
    val orgTemplatesDir = st.extract.get(orgTemplatesDirectorySetting)
    val orgTargetDir    = st.extract.get(orgTargetDirectorySetting)

    val vs = st
      .get(versions)
      .getOrElse(
        sys.error("No versions are set! Was this release part executed before inquireVersions?")
      )

    (for {
      _ <- fh.createResources(orgTemplatesDir, orgTargetDir)
      fileType = ChangelogFileType(DateTime.now(DateTimeZone.UTC), vs._1, comment)
      _ <- fh.checkOrgFiles(baseDir, orgTargetDir, List(fileType))
      maybeRef <- ghOps
        .commitFiles(
          baseDir = baseDir,
          branch = branch,
          message = s"$commitMessage [ci skip]",
          files = List(new File(baseDir, fileType.outputPath))
        )
        .value
        .unsafeRunSync()
    } yield maybeRef) match {
      case Right(Some(_)) =>
        st.log.info("Update Change Log was finished successfully")
      case Right(None) =>
        st.log.info(s"No changes detected in Changelog file. Skipping commit")
      case Left(e) =>
        e.printStackTrace()
        sys.error(s"Error updating Changelog file")
    }

    st
  }

  lazy val orgCommitNextVersion: ReleaseStep = { st: State =>
    val ghOps: GitHubOps[IO] = st.extract.get(orgGithubOpsSetting)
    val file                 = st.extract.get(releaseVersionFile)
    val branch               = st.extract.get(orgCommitBranchSetting)
    val baseDir              = st.extract.get(baseDirectory in LocalRootProject)

    val vs = st
      .get(versions)
      .getOrElse(
        sys.error("No versions are set! Was this release part executed before inquireVersions?")
      )

    val commitMessage = s"$orgVersionCommitMessage to ${vs._2}"

    ghOps.commitFiles(baseDir, branch, commitMessage, List(file)).value.unsafeRunSync() match {
      case Right(Some(_)) =>
        st.log.info("Next version was committed successfully")
      case Right(None) =>
        st.log.info("No changes detected in version file. Skipping commit")
      case Left(e) =>
        e.printStackTrace()
        sys.error(s"Error committing next version")
    }

    st
  }

  lazy val orgPostRelease: ReleaseStep = { st: State =>
    if (sbtorgpolicies.utils.getEnvVar("TRAVIS").isEmpty) {
      st.log.warn(
        "No Travis Environment detected, please be sure you revert " +
          "your local changes and fetch the latest remote changes"
      )
    }
    st
  }

  private[this] def vcs(st: State): Vcs =
    st.extract
      .get(releaseVcs)
      .getOrElse(
        sys.error("Aborting release. Working directory is not a repository of a recognized VCS.")
      )

}
