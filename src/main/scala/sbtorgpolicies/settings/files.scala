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
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.io._
import sbtorgpolicies.model._
import sbtorgpolicies.templates._

trait filesKeys {

  val orgCreateFiles: TaskKey[Unit] = taskKey[Unit]("Task to created the different files")

  val orgCreateContributorsFile: TaskKey[Unit] =
    taskKey[Unit]("Task for fetching the contributors from GitHub and creating a new SBT file with the list")

  val orgTargetDirectory: SettingKey[File] =
    SettingKey[File]("orgTargetDirectory", "Where sbt-org-policies output goes")

  val orgEnforcedFiles: SettingKey[List[FileType]] =
    settingKey[List[FileType]]("List of files the plugin must exists and should be created by the plugin")

}

trait files extends filesKeys with templatesKeys {

  def orgFilesDefaultSettings(
      gh: SettingKey[GitHubSettings],
      maintainers: SettingKey[List[Dev]],
      contributors: SettingKey[List[Dev]]) = Seq(
    orgTargetDirectory := resourceManaged.value / "org-policies",
    orgEnforcedFiles := List(
      LicenseFileType(gh.value),
      ContributingFileType(gh.value),
      AuthorsFileType(gh.value, maintainers.value, contributors.value))
  )

  def orgFilesTasks(
      gh: SettingKey[GitHubSettings],
      maintainers: SettingKey[List[Dev]],
      ghToken: SettingKey[String]) = Seq(
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

    }.value,
    orgCreateContributorsFile := Def.task {
      val fh    = new FileHelper
      val token = if (ghToken.value.isEmpty) None else Some(ghToken.value)
      val ghOps = new GitHubOps(gh.value.organization, gh.value.project, token)

      (for {
        list <- ghOps.fetchContributors
        maintainersIds = maintainers.value.map(_.id)
        filteredDevs = list
          .map(user => Dev(user.login, user.name, user.blog))
          .filterNot(dev => maintainersIds.contains(dev.id))
        _ <- fh.createResources(orgTemplatesDirectory.value, orgTargetDirectory.value)
        _ <- fh
          .checkOrgFiles(baseDirectory.value, orgTargetDirectory.value, List(ContributorsSBTFileType(filteredDevs)))
      } yield ()) match {
        case Right(_) => streams.value.log.info("contributors file created successfully")
        case Left(e) =>
          streams.value.log.error(s"Error creating contributors file")
          e.printStackTrace()
      }
    }.value
  )
}
