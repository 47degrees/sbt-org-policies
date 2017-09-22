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

trait ScalaSettings { self: dependencies =>

  def %(artifactId: String): ModuleID =
    getLib(artifactId).toModuleId.cross(CrossVersion.Disabled)

  def %(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toModuleId.cross(CrossVersion.Disabled)

  def %(artifactId: String, isSbtPlugin: Boolean): ModuleID =
    getLib(artifactId, isSbtPlugin = isSbtPlugin).toModuleId.cross(CrossVersion.Disabled)

  def %(artifactId: String, version: String, isSbtPlugin: Boolean): ModuleID =
    getLib(artifactId, Some(version), isSbtPlugin).toModuleId.cross(CrossVersion.Disabled)

  /**
   * It allows alternative Scala organization, however, scala-lang is still used
   * during transitive ivy resolution and should be added.
   */
  lazy val scalaDependencyOverrides = Seq(
    dependencyOverrides ++= Set(
      "org.scala-lang"        % "scala-compiler" % scalaVersion.value,
      "org.scala-lang"        % "scala-library"  % scalaVersion.value,
      "org.scala-lang"        % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang"        % "scalap"         % scalaVersion.value,
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
      scalaOrganization.value % "scala-library"  % scalaVersion.value,
      scalaOrganization.value % "scala-reflect"  % scalaVersion.value,
      scalaOrganization.value % "scalap"         % scalaVersion.value
    )
  )
}
