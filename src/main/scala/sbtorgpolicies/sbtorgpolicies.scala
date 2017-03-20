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
import org.scalajs.sbtplugin.impl.CrossGroupArtifactID
import sbt._

package object sbtorgpolicies {

  lazy val gpgFolder: String = sys.env.getOrElse("PGP_FOLDER", ".")

  // Licences
  /** Apache 2.0 Licence.*/
  val apache: (String, URL) =
    ("Apache License", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

  /** MIT Licence.*/
  val mit: (String, URL) = ("MIT", url("http://opensource.org/licenses/MIT"))

  /** Common scalac options useful to most (if not all) projects.*/
  lazy val scalacCommonOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint"
  )

  /** Scalac options for additional language options.*/
  lazy val scalacLanguageOptions = Seq(
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-language:reflectiveCalls",
    "-Ypartial-unification", // enable fix for SI-2712
    "-Yliteral-types", // enable SIP-23 implementation
    "-Xplugin-require:macroparadise"
  )

  /** Scalac strict compilation options.*/
  lazy val scalacStrictOptions = Seq(
    "-Xfatal-warnings",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  )

  /** Combines all scalac options.*/
  lazy val scalacAllOptions: Seq[String] = scalacCommonOptions ++ scalacLanguageOptions ++ scalacStrictOptions

  /** Github settings and related settings usually found in a Github README.*/
  case class GitHubSettings(
      organization: String,
      project: String,
      organizationName: String,
      organizationHomePage: URL,
      organizationEmail: String,
      license: (String, URL)) {
    def home: String         = s"https://github.com/$organization/$project"
    def homePage: URL        = url(s"https://$organization.github.io/$project/")
    def repo: String         = s"git@github.com:$organization/$project.git"
    def api: String          = s"https://$organization.github.io/$project/api/"
    def organisation: String = s"com.github.$organization"
    override def toString: String =
      s"""GitHubSettings:home = $home
         |GitHubSettings:homePage = $homePage
         |GitHubSettings:repo = $repo
         |GitHubSettings:api = $api
         |GitHubSettings:organisation = $organisation
         |""".stripMargin
  }

  case class Dep(organization: String, name: String, revision: String) {

    def toModuleId: ModuleID = organization %% name % revision

    def toJsModuleId: ModuleID = new CrossGroupArtifactID(organization, name, ScalaJSCrossVersion.binary) % revision
  }

  object scalac {

    val `2.10`: String = "2.10.6"
    val `2.11`: String = "2.11.8"
    val `2.12`: String = "2.12.1"

    val latestScalaVersion: String = `2.12`

    val crossScalaVersions: List[String] = List(`2.11`, `2.12`)
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
