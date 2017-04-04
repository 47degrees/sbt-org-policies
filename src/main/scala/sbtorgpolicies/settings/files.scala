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
import sbtorgpolicies.io._
import sbtorgpolicies.model._
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.templates._

trait files {

  val orgFilesTasks =
    Seq(
      orgCreateFiles := Def.task {
        val fh = new FileHelper

        (for {
          _ <- fh.createResources(orgTemplatesDirectorySetting.value, orgTargetDirectorySetting.value)
          _ <- fh.checkOrgFiles(baseDirectory.value, orgTargetDirectorySetting.value, orgEnforcedFilesSetting.value)
        } yield ()) match {
          case Right(_) => streams.value.log.info("Over-writable files have been created successfully")
          case Left(e) =>
            streams.value.log.error(s"Error creating files")
            e.printStackTrace()
        }

      }.value,
      orgCreateContributorsFile := Def.task {
        val fh    = new FileHelper
        val ghOps = orgGithubOpsSetting.value

        (for {
          list <- ghOps.fetchContributors
          maintainersIds = orgMaintainersSetting.value.map(_.id)
          filteredDevs = list
            .map(user => Dev(user.login, user.name, user.blog))
            .filterNot(dev => maintainersIds.contains(dev.id))
          _ <- fh.createResources(orgTemplatesDirectorySetting.value, orgTargetDirectorySetting.value)
          _ <- fh
            .checkOrgFiles(
              baseDirectory.value,
              orgTargetDirectorySetting.value,
              List(ContributorsSBTFileType(filteredDevs)))
        } yield ()) match {
          case Right(_) => streams.value.log.info("contributors file created successfully")
          case Left(e) =>
            streams.value.log.error(s"Error creating contributors file")
            e.printStackTrace()
        }
      }.value
    )
}
