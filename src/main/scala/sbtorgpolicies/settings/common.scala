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
import sbtorgpolicies.model._

trait common {

  val orgCommonTasks =
    Seq(
      orgFetchContributors := Def.task {
        onlyRootTask[List[Dev]](baseDirectory.value, (baseDirectory in LocalRootProject).value, streams.value.log, Nil) {
          val ghOps = orgGithubOpsSetting.value

          (for {
            list <- ghOps.fetchContributors
            maintainersIds = orgMaintainersSetting.value.map(_.id)
            filteredDevs = list
              .map(user => Dev(user.login, user.name, user.blog))
              .filterNot(dev => maintainersIds.contains(dev.id))
          } yield filteredDevs.sortBy(_.name)) match {
            case Right(devs) =>
              streams.value.log.info("Contributors fetched successfully")
              devs
            case Left(e) =>
              streams.value.log.error(s"Error fetching contributors")
              e.printStackTrace()
              Nil
          }
        }
      }.value,
      orgCompile in ThisBuild := Def
        .task[Unit] {
          compile in Compile
          (): Unit
        }
        .value
    )
}
