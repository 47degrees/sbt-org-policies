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
import sbtorgpolicies.GitHubSettings
import sbtorgpolicies.io._

trait filesKeys {

  val orgCreateFiles: TaskKey[Unit] = taskKey[Unit]("Task to created the different files")

  val orgTemplatesDirectory: SettingKey[File] = settingKey[File](
    "Optional. Directory where are placed the different templates it'll be used. " +
      "By default, it'll be the resourcesDirectory + '/org/templates'")

  val orgTargetDirectory: SettingKey[File] =
    SettingKey[File]("orgTargetDirectory", "Where sbt-org-policies output goes")

  val orgEnforcedFiles: SettingKey[List[FileType]] =
    settingKey[List[FileType]]("List of files the plugin must exists and should be created by the plugin")

}

trait files extends filesKeys {

  def orgFilesDefaultSettings(gh: SettingKey[GitHubSettings]) = Seq(
    orgTargetDirectory := resourceManaged.value / "org-policies",
    orgTemplatesDirectory := (resourceDirectory in Compile).value / "org" / "templates",
    orgEnforcedFiles := List(LicenseFileType(gh.value), ContributingFileType(gh.value))
  )

  lazy val orgFilesTasks = Seq(
    orgCreateFiles := Def.task {
      val fh = new FileHelper

      (for {
        _ <- fh.createResources(orgTemplatesDirectory.value, orgTargetDirectory.value)
        _ <- fh.checkOrgFiles(baseDirectory.value, orgTargetDirectory.value, orgEnforcedFiles.value)
      } yield ()) match {
        case Right(_) => streams.value.log.info("Over-writable files have been created successfully")
        case Left(e) =>
          streams.value.log.error(s"Error creating files")
          e.printStackTrace()
      }

    }.value
  )
}
