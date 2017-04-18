/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.syntax.either._
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.io._
import sbtorgpolicies.templates.utils._

import scala.util.matching.Regex

trait files {

  val orgFilesSettings = Seq(
    orgUpdateDocFilesSetting := List(baseDirectory.value / "docs", baseDirectory.value / "README.md"),
    orgUpdateDocFilesCommitSetting := true,
    orgUpdateDocFilesReplacementsSetting := Map("\"\\d+\\.\\d+\\.\\d+\"" -> ("\"" + version.value + "\""))
  )

  val orgFilesTasks =
    Seq(
      orgCreateFiles := Def.task {
        onlyRootUnitTask(baseDirectory.value, (baseDirectory in LocalRootProject).value, streams.value.log) {
          val fh = new FileHelper

          val buildV     = version.value
          val isSnapshot = buildV.endsWith("-SNAPSHOT")

          val enforcedFiles = orgEnforcedFilesSetting.value.filter(ft => !ft.finalVersionOnly || !isSnapshot)

          (for {
            _ <- fh.createResources(orgTemplatesDirectorySetting.value, orgTargetDirectorySetting.value)
            _ <- fh.checkOrgFiles(baseDirectory.value, orgTargetDirectorySetting.value, enforcedFiles)
          } yield ()) match {
            case Right(_) =>
              streams.value.log.info(
                printList("The following files where created and/or modified:", enforcedFiles.map(_.outputPath)))
            case Left(e) =>
              streams.value.log.error(s"Error creating files")
              e.printStackTrace()
          }
        }
      }.value,
      orgUpdateDocFiles := Def.task {
        onlyRootUnitTask(baseDirectory.value, (baseDirectory in LocalRootProject).value, streams.value.log) {
          val replaceTextEngine      = new ReplaceTextEngine
          val blockTitle: String     = "Replace"
          val startBlockRegex: Regex = markdownComment(blockTitle, scape = true).r
          val endBlockRegex: Regex   = markdownComment(blockTitle, start = false, scape = true).r

          val isFileSupported: (File) => Boolean = file => {
            file.getName.indexOf('.') < 0 || file.getName.endsWith(".md")
          }

          val replaced = replaceTextEngine.replaceBlocks(
            startBlockRegex,
            endBlockRegex,
            orgUpdateDocFilesReplacementsSetting.value,
            orgUpdateDocFilesSetting.value,
            isFileSupported)

          val errorFiles = replaced.filter(_.status.failure).map(_.file.getAbsolutePath)
          if (errorFiles.nonEmpty) {
            streams.value.log.warn(printList("The following files where processed with errors:", errorFiles))
          }

          val modified = replaced.filter(f => f.status.success && f.status.modified).map(_.file)
          if (modified.nonEmpty) {
            streams.value.log.info(printList("The following files where modified:", modified.map(_.getAbsolutePath)))
            if (orgUpdateDocFilesCommitSetting.value) {
              streams.value.log.info("Committing files")
              val ghOps: GitHubOps = orgGithubOpsSetting.value
              ghOps.commitFiles(
                branch = orgCommitBranchSetting.value,
                message = s"${orgCommitMessageSetting.value} [ci skip]",
                files = modified
              ) match {
                case Right(Some(_)) =>
                  streams.value.log.info("Docs files committed successfully")
                case Right(None) =>
                  streams.value.log.info("No changes detected in docs files. Skipping commit")
                case Left(e) =>
                  streams.value.log.error(s"Error committing files")
                  e.printStackTrace()
              }
            }
          }

          if (errorFiles.isEmpty && modified.isEmpty) {
            streams.value.log.info("No files updated")
          }
        }
      }.value
    )
}
