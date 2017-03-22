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

import org.scalajs.sbtplugin.ScalaJSCrossVersion
import sbt._

package object sbtorgpolicies {

  case class Dep(organization: String, name: String, revision: String) {

    def toModuleId: ModuleID = organization %% name % revision

    def toJsModuleId: ModuleID = (organization % name % revision).cross(ScalaJSCrossVersion.binary)
  }

  /** The name and github user id */
  case class Dev(name: String, id: String) {
    def pomExtra: xml.NodeSeq =
      <developer>
        <id>{ id }</id>
        <name>{ name }</name>
        <url>http://github.com/{ id }</url>
      </developer>
  }
}
