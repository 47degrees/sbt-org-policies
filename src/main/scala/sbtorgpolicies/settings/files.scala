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
import sbtorgpolicies.io.ReplaceTextEngine.ProcessedFile
import sbtorgpolicies.io._
import sbtorgpolicies.templates.FileType
import sbtorgpolicies.templates.utils._

import scala.util.matching.Regex

trait files {

  val orgFilesSettings = Seq(
    orgUpdateDocFilesSetting := List(baseDirectory.value / "docs", baseDirectory.value / "README.md"),
    orgUpdateDocFilesCommitSetting := true,
    orgUpdateDocFilesReplacementsSetting := Map("\"\\d+\\.\\d+\\.\\d+\"(-SNAPSHOT)?" -> ("\"" + version.value + "\""))
  )

  val orgFilesTasks =
    Seq(
      orgCreateFiles := Def.task {
        onlyRootUnitTask(baseDirectory.value, (baseDirectory in LocalRootProject).value, streams.value.log) {
          createPolicyFiles(
            baseDir = (baseDirectory in LocalRootProject).value,
            templatesDir = orgTemplatesDirectorySetting.value,
            targetDir = orgTargetDirectorySetting.value,
            isSnapshot = version.value.endsWith("-SNAPSHOT"),
            fileTypes = orgEnforcedFilesSetting.value,
            log = streams.value.log
          )
          (): Unit
        }
      }.value,
      orgUpdateDocFiles := Def.task {
        onlyRootUnitTask(baseDirectory.value, (baseDirectory in LocalRootProject).value, streams.value.log) {

          val taskStreams: TaskStreams = streams.value
          val baseDir: File       = (baseDirectory in LocalRootProject).value
          val isSnapshot: Boolean = version.value.endsWith("-SNAPSHOT")

          val policyFiles: List[File] = createPolicyFiles(
            baseDir = baseDir,
            templatesDir = orgTemplatesDirectorySetting.value,
            targetDir = orgTargetDirectorySetting.value,
            isSnapshot = isSnapshot,
            fileTypes = orgEnforcedFilesSetting.value,
            log = taskStreams.log
          )

          val replacements: Map[String, String] = orgUpdateDocFilesReplacementsSetting.value
          val updateDocFiles: List[File] = orgUpdateDocFilesSetting.value

          val modifiedDocFiles: List[File] = if (!isSnapshot) {
            val replaceTextEngine      = new ReplaceTextEngine
            val blockTitle: String     = "Replace"
            val startBlockRegex: Regex = markdownComment(blockTitle, scape = true).r
            val endBlockRegex: Regex   = markdownComment(blockTitle, start = false, scape = true).r

            val isFileSupported: (File) => Boolean = file => {
              file.getName.indexOf('.') < 0 || file.getName.endsWith(".md")
            }

            val replaced: List[ProcessedFile] = replaceTextEngine.replaceBlocks(
              startBlockRegex,
              endBlockRegex,
              replacements,
              updateDocFiles,
              isFileSupported) match {
              case Left(e) =>
                taskStreams.log.error(s"Error updating policy files")
                e.printStackTrace()
                Nil
              case Right(l) => l
            }

            val errorFiles = replaced.filter(_.status.failure).map(_.file.getAbsolutePath)
            if (errorFiles.nonEmpty) {
              taskStreams.log.warn(printList("The following files where processed with errors:", errorFiles))
            }

            replaced.filter(f => f.status.success && f.status.modified).map(_.file)
          } else Nil

          val allFiles: List[File] = (policyFiles ++ modifiedDocFiles).map(_.getAbsolutePath).distinct.map(file)
          val updateDocFilesCommitSetting: Boolean = orgUpdateDocFilesCommitSetting.value
          val commitMessage: String = orgCommitMessageSetting.value
          val commitBranch: String = orgCommitBranchSetting.value
          val ghOps: GitHubOps = orgGithubOpsSetting.value

          if (allFiles.nonEmpty) {
            if (updateDocFilesCommitSetting) {
              taskStreams.log.info(printList("Committing files", allFiles.map(_.getAbsolutePath)))

              ghOps.commitFiles(
                baseDir = baseDir,
                branch = commitBranch,
                message = s"$commitMessage [ci skip]",
                files = allFiles
              ) match {
                case Right(Some(_)) =>
                  taskStreams.log.info("Files committed successfully")
                case Right(None) =>
                  taskStreams.log.info("No changes detected in docs and policy files. Skipping commit")
                case Left(e) =>
                  taskStreams.log.error(s"Error committing files")
                  e.printStackTrace()
              }
            } else taskStreams.log.info("orgUpdateDocFilesCommitSetting set to `false`. Skipping commit")
          } else {
            taskStreams.log.info("No files to be committed")
          }
        }
      }.value
    )

  private[this] def createPolicyFiles(
      baseDir: File,
      templatesDir: File,
      targetDir: File,
      isSnapshot: Boolean,
      fileTypes: List[FileType],
      log: Logger): List[File] = {
    val fh = new FileHelper

    val enforcedFiles = fileTypes.filter(ft => !ft.finalVersionOnly || !isSnapshot)

    (for {
      _         <- fh.createResources(templatesDir, targetDir)
      fileTypes <- fh.checkOrgFiles(baseDir, targetDir, enforcedFiles)
    } yield fileTypes) match {
      case Right(modifiedFileTypes) =>
        log.info(printList("The following files where created and/or modified:", modifiedFileTypes.map(_.outputPath)))
        modifiedFileTypes.map(f => baseDir / f.outputPath)
      case Left(e) =>
        log.error(s"Error creating files")
        e.printStackTrace()
        Nil
    }
  }
}
