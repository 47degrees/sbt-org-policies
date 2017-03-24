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

import sbt._
import sbtorgpolicies.model._

trait keys {

  val orgGithubSetting: SettingKey[GitHubSettings] =
    settingKey[GitHubSettings]("General Org GitHub Settings")

  val orgGithubTokenSetting: SettingKey[Option[String]] =
    settingKey[Option[String]]("Github token, needed to be used to interact with Github, empty by default")

  val orgLicenseSetting: SettingKey[License] =
    settingKey[License]("General Org License Setting")

  val orgMaintainersSetting: SettingKey[List[Dev]] =
    settingKey[List[Dev]]("List of Maintainers of the project")

  val orgContributorsSetting: SettingKey[List[Dev]] =
    settingKey[List[Dev]]("List of Devs involved in the development of the project")
}
