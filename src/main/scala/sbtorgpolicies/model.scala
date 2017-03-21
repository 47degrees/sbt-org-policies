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

import sbt.Append.Value
import sbt.{Append, URL, url}

object model {

  sealed abstract class License(val name: String, val url: URL) {
    def tupled: (String, URL) = (name, url)
    def custom: License = CustomLicense(this)
  }

  /** Apache 2.0 License.*/
  case object ApacheLicense extends License("Apache License", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

  /** MIT License.*/
  case object MITLicense extends License("MIT", url("http://opensource.org/licenses/MIT"))

  /** Custom License. */
  class CustomLicense(name: String, url: URL) extends License(name, url)

  object CustomLicense {

    def apply(name: String, url: URL): CustomLicense = new CustomLicense(name, url)

    def apply(other: License): CustomLicense = new CustomLicense(other.name, other.url)

  }

  implicit val settingAppender: Append.Value[Seq[(String, java.net.URL)], License] =
    new Value[Seq[(String, URL)], License] {
      override def appendValue(a: Seq[(String, URL)], b: License): Seq[(String, URL)] = a :+ b.tupled
    }

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
    "-language:reflectiveCalls"
  )

  lazy val scalacAdvancedOptions = Seq(
    "-Ypartial-unification", // enable fix for SI-2712
    "-Yliteral-types", // enable SIP-23 implementation
    "-Xplugin-require:macroparadise")

  /** Scalac strict compilation options.*/
  lazy val scalacStrictOptions = Seq(
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
    license: License) {
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

  object scalac {

    val `2.10`: String = "2.10.6"
    val `2.11`: String = "2.11.8"
    val `2.12`: String = "2.12.1"

    val latestScalaVersion: String = `2.12`

    val crossScalaVersions: List[String] = List(`2.11`, `2.12`)

  }

}