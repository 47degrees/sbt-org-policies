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

package sbtorgpolicies

import org.scalafmt.sbt.ScalafmtPlugin
import sbt._
import sbtorgpolicies.settings.DefaultSettings

object OrgPoliciesPlugin extends AutoPlugin {

  object autoImport extends OrgPoliciesKeys with DefaultSettings

  import autoImport._

  override def requires: Plugins = plugins.JvmPlugin && ScalafmtPlugin

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = orgDefaultSettings

  override def globalSettings: Seq[Def.Setting[_]] = pgpSettings
}
