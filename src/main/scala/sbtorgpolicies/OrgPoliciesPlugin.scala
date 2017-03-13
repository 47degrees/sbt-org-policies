/*
 * Copyright 2016 47 Degrees, LLC. <http://www.47deg.com>
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

package sbtorgpolicies

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object OrgPoliciesPlugin extends AutoPlugin {

  object autoImport extends settings with OrgPoliciesKeys

  import autoImport._

  override def requires: JvmPlugin.type = plugins.JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  lazy val orgDefaultSettings: Seq[Setting[_]] = Seq(
      orgGithubSettings := GitHubSettings(
        organization = "47deg",
        project = name.value,
        publishOrg = "47 Degrees",
        organizationHomePage = url("http://47deg.com"),
        license = apache),
      orgDevSettings := List(Dev("47 Degrees (twitter: @47deg)", "47 Degrees")),
      scalacOptions ++= scalacAllOptions
    ) ++
      sharedPublishSettings(orgGithubSettings, orgDevSettings) ++
      sharedBuildSettings(orgGithubSettings)

  override def projectSettings: Seq[Def.Setting[_]] =
    sharedCommonSettings ++
      sharedReleaseProcess ++
      credentialSettings ++
      pgpSettings ++
      orgDefaultSettings
}
