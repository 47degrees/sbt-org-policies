/*
 * Copyright 2016 47 Degrees, LLC. <http://www.47deg.com>
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
    "-language:experimental.macros"
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
  case class GitHubSettings(org: String, proj: String, publishOrg: String, license: (String, URL)) {
    def home         = s"https://github.com/$org/$proj"
    def repo         = s"git@github.com:$org/$proj.git"
    def api          = s"https://$org.github.io/$proj/api/"
    def organisation = s"com.github.$org"
    override def toString =
      s"GitHubSettings:home = $home\nGitHubSettings:repo = $repo\nGitHubSettings:api = $api\nGitHubSettings:organisation = $organisation"
  }

  case class Dep(organization: String, name: String, revision: String) {

    def toModuleId: ModuleID = organization %% name % revision

    def toJsModuleId: ModuleID = new CrossGroupArtifactID(organization, name, ScalaJSCrossVersion.binary) % revision
  }

  object scalac {

    val `2.10`: String = "2.10.6"
    val `2.11`: String = "2.11.8"
    val `2.12`: String = "2.12.1"

    val crossScalaVersions: List[String] = List(`2.10`, `2.11`, `2.12`)
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
