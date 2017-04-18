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

import org.scalajs.sbtplugin.ScalaJSCrossVersion
import org.scalajs.sbtplugin.cross.CrossProject
import sbt.Keys.libraryDependencies
import sbt._
import sbtorgpolicies.libraries._

import scala.language.postfixOps

trait dependencies {

  case class Dep(organization: String, name: String, revision: String) {

    def toModuleId: ModuleID = organization %% name % revision

    def toJsModuleId: ModuleID = (organization % name % revision).cross(ScalaJSCrossVersion.binary)
  }

  def %(artifactId: String): ModuleID =
    getLib(artifactId).toModuleId.cross(CrossVersion.Disabled)

  def %(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toModuleId.cross(CrossVersion.Disabled)

  def %%(artifactId: String): ModuleID =
    getLib(artifactId).toModuleId

  def %%(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toModuleId

  def %%%(artifactId: String): ModuleID =
    getLib(artifactId).toJsModuleId

  def %%%(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toJsModuleId

  implicit class CrossProjectOps(crossProject: CrossProject) {

    def crossDepSettings(modules: ModuleID*): CrossProject = {
      val dependencyList: Seq[(ModuleID, ModuleID)] = modules.toList.map { moduleID =>
        (%%(moduleID.name, moduleID.revision), %%%(moduleID.name, moduleID.revision))
      }
      crossProject
        .jvmSettings(libraryDependencies ++= dependencyList.map(_._1))
        .jsSettings(libraryDependencies ++= dependencyList.map(_._2))
    }

  }

  private[this] def getLib(lib: String, maybeVersion: Option[String] = None) = {
    val artifact: (String, String, String) = libs(lib)
    val dep                                = Dep(artifact._1, artifact._2, artifact._3)
    maybeVersion.foldLeft(dep)((module, revision) => module.copy(revision = revision))
  }
}
