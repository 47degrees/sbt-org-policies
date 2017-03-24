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

import de.heikoseeberger.sbtheader.HeaderKey
import sbt._
import sbt.Keys._
import sbtorgpolicies.exceptions.ValidationException
import sbtorgpolicies.model._
import scoverage.ScoverageKeys

import scala.util.matching.Regex

trait enforcementKeys {

  val orgCheckSettings: TaskKey[Unit] = taskKey[Unit]("Task for checking the project settings")

}

trait enforcement extends enforcementKeys {

  lazy val orgEnforcementSettingsTasks = Seq(
    orgCheckSettings := Def
      .sequential(
        checkScalaVersion,
        checkCrossScalaVersion,
        checkScoverageSettings,
        checkFileHeaderSettings
      )
      .value
  )

  private[this] def checkScalaVersion = Def.task {
    val scalaVersionValue = scalaVersion.value
    if (scalaVersionValue != scalac.latestScalaVersion) {
      throw ValidationException(s"scalaVersion is $scalaVersionValue. It should be ${scalac.latestScalaVersion}")
    }
  }

  private[this] def checkCrossScalaVersion = Def.task {
    val crossScalaVersionsValue = crossScalaVersions.value
    if (crossScalaVersionsValue != scalac.crossScalaVersions) {
      throw ValidationException(
        s"crossScalaVersions is $crossScalaVersionsValue. It should be ${scalac.crossScalaVersions}")
    }
  }

  private[this] def checkScoverageSettings = Def.task {

    val coverageFailOnMinimumValue = ScoverageKeys.coverageFailOnMinimum.value
    val coverageMinimumValue       = ScoverageKeys.coverageMinimum.value

    if (!coverageFailOnMinimumValue)
      throw ValidationException(
        s"coverageFailOnMinimum is $coverageFailOnMinimumValue, however, it should be enabled.")

    if (coverageMinimumValue < scoverageMinimum)
      throw ValidationException(
        s"coverageMinimumValue is $coverageMinimumValue. It should be at least $scoverageMinimum%")
  }

  private[this] def checkFileHeaderSettings = Def.task {
    val headersValue: Map[String, (Regex, String)] = HeaderKey.headers.value
    if (headersValue.size <= 0) {
      throw ValidationException(s"HeaderKey.headers is empty and it's a mandatory setting")
    }
  }

}
