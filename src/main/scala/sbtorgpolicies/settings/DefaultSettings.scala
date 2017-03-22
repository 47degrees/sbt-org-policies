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

import sbt.Keys.{name, scalacOptions}
import sbt.{url, Setting}
import sbtorgpolicies._
import sbtorgpolicies.model._

trait DefaultSettings extends AllSettings {

  lazy val orgDefaultSettings: Seq[Setting[_]] =
    Seq(
      orgGithubSettings := GitHubSettings(
        organization = "47deg",
        project = name.value,
        organizationName = "47 Degrees",
        groupId = "com.47deg",
        organizationHomePage = url("http://47deg.com"),
        organizationEmail = "hello@47deg.com",
        license = ApacheLicense
      ),
      orgDevSettings := List(Dev("47 Degrees (twitter: @47deg)", "47 Degrees")),
      scalacOptions ++= scalacAllOptions
    ) ++
      sharedCommonSettings ++
      sharedCommonDependencies ++
      sharedReleaseProcess ++
      credentialSettings ++
      sharedPublishSettings(orgGithubSettings, orgDevSettings) ++
      sharedBuildSettings(orgGithubSettings) ++
      scalaDependencyOverrides ++
      sharedScoverageSettings() ++
      scalafmtSettings ++
      sbtDependenciesSettings(orgGithubSettings) ++
      orgFileSettings(orgGithubSettings) ++
      orgTemplatesDefaultSettings ++
      fileValidationSettings ++
      enforcementSettingsTasks
}
