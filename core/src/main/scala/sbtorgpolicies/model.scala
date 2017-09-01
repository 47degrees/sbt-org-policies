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

import net.jcazevedo.moultingyaml._
import sbt.Append.Value
// import sbt.io._
// import sbt.io.syntax._
import sbt.{url, URL}
import sbtorgpolicies.runnable.RunnableItemConfigScope
import sbtorgpolicies.runnable.syntax._

object model {

  sealed abstract class License(val name: String, val url: URL) {
    def tupled: (String, URL) = (name, url)
    def custom: License       = CustomLicense(this)
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

  implicit val settingAppender: Value[Seq[(String, java.net.URL)], License] =
    new Value[Seq[(String, URL)], License] {
      override def appendValue(a: Seq[(String, URL)], b: License): Seq[(String, URL)] = a :+ b.tupled
    }

  lazy val scoverageMinimum = 80d

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

  /**
   * Alias helper for the publishMicrosite task when docs module is located in the "docs" sbt module.
   */
  lazy val defaultPublishMicrosite: RunnableItemConfigScope[Unit] = ";project docs;publishMicrosite".asRunnableItem

  /** Github settings and related settings usually found in a Github README.*/
  case class GitHubSettings(
      organization: String,
      project: String,
      organizationName: String,
      groupId: String,
      organizationHomePage: URL,
      organizationEmail: String) {
    def home: String         = s"https://github.com/$organization/$project"
    def homePage: URL        = url(s"https://$organization.github.io/$project/")
    def repo: String         = s"git@github.com:$organization/$project.git"
    def api: String          = s"https://$organization.github.io/$project/api/"
    def organisation: String = s"com.github.$organization"
    override def toString: String =
      s"""GitHubSettings:project = $project
         |GitHubSettings:home = $home
         |GitHubSettings:homePage = $homePage
         |GitHubSettings:repo = $repo
         |GitHubSettings:api = $api
         |GitHubSettings:organisation = $organisation
         |""".stripMargin
  }

  /** The name and github user id */
  case class Dev(id: String, name: Option[String] = None, url: Option[String] = None) {
    def pomExtra: xml.NodeSeq =
      <developer>
        <id>{ id }</id>
        { name.fold(xml.NodeSeq.Empty)(x => <name>{x}</name>) }
        <url>http://github.com/{ id }</url>
      </developer>
  }

  object sbtV {
    val `0.13`: String = "0.13.16"
    val `1.0`: String  = "1.0.1"

    val crossSbtVersions: List[String] = List(`0.13`, `1.0`)
  }

  object scalac {

    val `2.10`: String = "2.10.6"
    val `2.11`: String = "2.11.11"
    val `2.12`: String = "2.12.3"
    val `2.13`: String = "2.13.0-M1"

    val latestScalaVersion: String = `2.12`

    val crossScalaVersions: List[String] = List(`2.11`, `2.12`)

  }

  object YamlFormats extends DefaultYamlProtocol {

    implicit object AnyYamlFormat extends YamlFormat[Any] {
      def write(x: Any): YamlValue = x match {
        case n: Int            => YamlNumber(n)
        case n: Long           => YamlNumber(n)
        case n: Double         => YamlNumber(n)
        case s: String         => YamlString(s)
        case b: Boolean        => YamlBoolean(b)
        case x: Seq[_]         => seqFormat[Any].write(x)
        case m: Map[String, _] => mapFormat[String, Any].write(m)
        case t =>
          serializationError("Serialization Error - Non expected type " + t.getClass.getName)
      }

      def read(value: YamlValue): Any = value match {
        case YamlNumber(n)  => n.intValue()
        case YamlString(s)  => s
        case YamlBoolean(b) => b
        case _: YamlArray   => listFormat[Any].read(value)
        case _: YamlObject  => mapFormat[String, Any].read(value)
        case x =>
          deserializationError("Deserialization Error - it failed the deserialization of " + x)
      }
    }
  }

}
