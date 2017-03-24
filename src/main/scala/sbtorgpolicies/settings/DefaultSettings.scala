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

import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderKey.headers
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt._
import sbt.Keys._
import sbtorgpolicies.model._

trait DefaultSettings extends AllSettings {

  lazy val orgDefaultSettings: Seq[Setting[_]] =
    Seq(
      orgGithubSetting := GitHubSettings(
        organization = "47deg",
        project = name.value,
        organizationName = "47 Degrees",
        groupId = "com.47deg",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      orgGithubTokenSetting := None,
      orgLicenseSetting := ApacheLicense,
      orgMaintainersSetting := List(Dev("47degdev", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgContributorsSetting := Nil,
      startYear := Some(currentYear),
      scalacOptions ++= scalacAllOptions,
      headers := Map(
        "scala" -> Apache2_0("2017", "47 Degrees, LLC. <http://www.47deg.com>")
      )
    ) ++
      sharedCommonSettings ++
      sharedCommonDependencies ++
      sharedReleaseProcess ++
      credentialSettings ++
      sharedPublishSettings(orgGithubSetting, orgLicenseSetting, orgMaintainersSetting, orgContributorsSetting) ++
      sharedBuildSettings(orgGithubSetting) ++
      scalaDependencyOverrides ++
      sharedScoverageSettings() ++
      scalafmtSettings ++
      orgFileSettings(orgGithubSetting, orgLicenseSetting, orgMaintainersSetting, orgContributorsSetting) ++
      orgTemplatesDefaultSettings ++
      orgFileValidationSettings(name, orgLicenseSetting, orgMaintainersSetting, orgContributorsSetting) ++
      orgEnforcementSettingsTasks ++
      AutomateHeaderPlugin.automateFor(Compile, Test)
}
