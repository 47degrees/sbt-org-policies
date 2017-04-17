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
import sbtorgpolicies.io._
import sbtorgpolicies.templates.utils._

import scala.util.matching.Regex

trait files {

  val orgFilesSettings = Seq(
    orgUpdateDocFilesSetting := List(baseDirectory.value / "docs"),
    orgUpdateDocFilesReplacementsSetting := Map.empty
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

          val modified = replaced.filter(f => f.status.success && f.status.modified).map(_.file.getAbsolutePath)
          if (modified.nonEmpty) {
            streams.value.log.info(printList("The following files where modified:", modified))
          }

          if (errorFiles.isEmpty && modified.isEmpty) {
            streams.value.log.info("No files updated")
          }
        }
      }.value
    )
}
