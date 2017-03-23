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

import sbt.Keys._
import sbt._
import sbtorgpolicies.model._
import utils._

trait DefaultSettings extends AllSettings {

  lazy val orgDefaultSettings: Seq[Setting[_]] =
    Seq(
      orgGithubSettings := GitHubSettings(
        organization = "47deg",
        project = name.value,
        organizationName = "47 Degrees",
        groupId = "com.47deg",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com"
      ),
      orgLicense := ApacheLicense,
      orgMaintainersSettings := List(Dev("47degdev", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
      orgContributorsSettings := Nil,
      startYear := Some(currentYear),
      scalacOptions ++= scalacAllOptions
    ) ++
      sharedCommonSettings ++
      sharedCommonDependencies ++
      sharedReleaseProcess ++
      credentialSettings ++
      sharedPublishSettings(orgGithubSettings, orgLicense, orgMaintainersSettings, orgContributorsSettings) ++
      sharedBuildSettings(orgGithubSettings) ++
      scalaDependencyOverrides ++
      sharedScoverageSettings() ++
      scalafmtSettings ++
      sbtDependenciesSettings(orgGithubSettings) ++
      orgFileSettings(orgGithubSettings, orgLicense, orgMaintainersSettings, orgContributorsSettings) ++
      orgTemplatesDefaultSettings ++
      orgFileValidationSettings(orgMaintainersSettings, orgContributorsSettings) ++
      enforcementSettingsTasks
}
