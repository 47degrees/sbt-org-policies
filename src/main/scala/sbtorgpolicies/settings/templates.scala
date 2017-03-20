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

trait templatesKeys {

  val orgTemplatesDirectory: SettingKey[File] = settingKey[File](
    "Optional. Directory where are placed the different templates it'll be used. " +
      "By default, it'll be the resourcesDirectory + '/org/templates'")

}

trait templates extends templatesKeys {

  lazy val orgTemplatesDefaultSettings = Seq(
    orgTemplatesDirectory := (resourceDirectory in Compile).value / "org" / "templates"
  )
}
