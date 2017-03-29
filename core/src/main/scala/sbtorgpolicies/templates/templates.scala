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

import sbtorgpolicies.model._
import sbtorgpolicies.utils._
import sbtorgpolicies.templates.syntax._

import scala.language.implicitConversions

package object templates {

  type Replacements = Map[String, Replaceable]

  trait Replaceable {

    def asString: String
  }

  case class ReplaceableT[T](t: T) extends Replaceable {
    override def asString: String = t.toString
  }

  case class ReplaceableList[T <: Replaceable](list: List[T]) extends Replaceable {
    override def asString: String = list.map(elem => s"* ${elem.asString}").mkString("\n")
  }

  case class FileType(
      mandatory: Boolean,
      overWritable: Boolean,
      templatePath: String,
      outputPath: String,
      replacements: Replacements)

  val contributorsFilePath: String = "contributors.sbt"

  private[this] def replaceableYear(startYear: Option[Int]): Replaceable = {
    startYear.getOrElse(currentYear) match {
      case start if start == currentYear => currentYear.asReplaceable
      case start                         => s"$start-$currentYear".asReplaceable
    }
  }

  def LicenseFileType(ghSettings: GitHubSettings, license: License, startYear: Option[Int]): FileType = {

    def licenseFile: String = license match {
      case ApacheLicense => "templates/LICENSE_ASL2.template"
      case MITLicense    => "templates/LICENSE_MIT.template"
      case _             => "templates/LICENSE.template"
    }

    FileType(
      mandatory = true,
      overWritable = true,
      templatePath = licenseFile,
      outputPath = "LICENSE",
      replacements = Map(
        "year"                 -> replaceableYear(startYear),
        "organizationName"     -> ghSettings.organizationName.asReplaceable,
        "organizationHomePage" -> ghSettings.organizationHomePage.asReplaceable
      )
    )
  }

  def ContributingFileType(ghSettings: GitHubSettings) = FileType(
    mandatory = true,
    overWritable = true,
    templatePath = "templates/CONTRIBUTING.md.template",
    outputPath = "CONTRIBUTING.md",
    replacements = Map(
      "name"              -> ghSettings.project.asReplaceable,
      "organization"      -> ghSettings.organization.asReplaceable,
      "organizationName"  -> ghSettings.organizationName.asReplaceable,
      "organizationEmail" -> ghSettings.organizationEmail.asReplaceable
    )
  )

  def AuthorsFileType(ghSettings: GitHubSettings, maintainers: List[Dev], contributors: List[Dev]): FileType = {

    def devTemplate(dev: Dev): Replaceable =
      (dev.name match {
        case Some(n) => s"$n <[${dev.id}](https://github.com/${dev.id})>"
        case None    => s"[${dev.id}](https://github.com/${dev.id})"
      }).asReplaceable

    FileType(
      mandatory = true,
      overWritable = true,
      templatePath = "templates/AUTHORS.md.template",
      outputPath = "AUTHORS.md",
      replacements = Map(
        "name"         -> ghSettings.project.asReplaceable,
        "maintainers"  -> maintainers.map(devTemplate).asReplaceable,
        "contributors" -> contributors.map(devTemplate).asReplaceable
      )
    )
  }

  def ContributorsSBTFileType(list: List[Dev]): FileType = {

    def optionAsScalaString(o: Option[String]): String =
      o.map(v => s"""Some("$v")""").getOrElse("None")

    def devsAsScalaListString: List[String] = list.map { dev =>
      s"""    Dev("${dev.id}", ${optionAsScalaString(dev.name)}, ${optionAsScalaString(dev.url)})"""
    }

    FileType(
      mandatory = false,
      overWritable = true,
      templatePath = "templates/contributors.sbt.template",
      outputPath = contributorsFilePath,
      replacements = Map(
        "devs" -> devsAsScalaListString.mkString(",\n").asReplaceable
      )
    )
  }

  def NoticeFileType(ghSettings: GitHubSettings, license: License, startYear: Option[Int]): FileType = {

    FileType(
      mandatory = true,
      overWritable = true,
      templatePath = "templates/NOTICE.md.template",
      outputPath = "NOTICE.md",
      replacements = Map(
        "year"             -> replaceableYear(startYear),
        "name"             -> ghSettings.project.asReplaceable,
        "organizationName" -> ghSettings.organizationName.asReplaceable,
        "licenseName"      -> license.name.asReplaceable
      )
    )
  }

  object syntax {

    implicit def ioListSyntax[T <: Replaceable](list: List[T]): IOReplaceableListOps[T] =
      new IOReplaceableListOps(list)

    implicit def ioTSyntax[T](t: T): IOReplaceableOps[T] = new IOReplaceableOps(t)

    final class IOReplaceableOps[T](t: T) {

      def asReplaceable: ReplaceableT[T] = ReplaceableT(t)

    }

    final class IOReplaceableListOps[T <: Replaceable](list: List[T]) {

      def asReplaceable: ReplaceableList[T] = ReplaceableList[T](list)

    }
  }
}
