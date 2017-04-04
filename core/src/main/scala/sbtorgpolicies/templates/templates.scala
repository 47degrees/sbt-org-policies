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

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbtorgpolicies.model._
import sbtorgpolicies.utils._
import sbtorgpolicies.templates.syntax._

import scala.language.{implicitConversions, postfixOps}
import scala.util.matching.Regex

package object templates {

  val contributorsFilePath: String = "contributors.sbt"
  val versionFilePath: String      = "version.sbt"

  type Replacements = Map[String, Replaceable]

  trait Replaceable {

    def asString: String
  }

  case class ReplaceableT[T](t: T) extends Replaceable {
    override def asString: String = t.toString
  }

  case class ReplaceableList[T](list: List[T]) extends Replaceable {
    override def asString: String = list.map(elem => s"* ${elem.asReplaceable.asString}").mkString("\n")
  }

  case class FileType(
      mandatory: Boolean,
      overWritable: Boolean,
      templatePath: String,
      outputPath: String,
      replacements: Replacements,
      fileSections: List[FileSection] = Nil)

  case class FileSection(
      appendPosition: AppendPosition,
      template: String,
      replacements: Replacements,
      shouldAppend: (String) => Boolean = _ => true)

  sealed trait AppendPosition
  case object AppendAtTheBeginning    extends AppendPosition
  case object AppendAtTheEnd          extends AppendPosition
  case class AppendAfter(line: Regex) extends AppendPosition

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
        "year"                 -> replaceableYear(startYear).asReplaceable,
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

    def devTemplate(dev: Dev): String =
      dev.name match {
        case Some(n) => s"$n <[${dev.id}](https://github.com/${dev.id})>"
        case None    => s"[${dev.id}](https://github.com/${dev.id})"
      }

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
        "year"             -> replaceableYear(startYear).asReplaceable,
        "name"             -> ghSettings.project.asReplaceable,
        "organizationName" -> ghSettings.organizationName.asReplaceable,
        "licenseName"      -> license.name.asReplaceable
      )
    )
  }

  def VersionSbtFileType: FileType =
    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/version.sbt.template",
      outputPath = versionFilePath,
      replacements = Map.empty
    )

  def ChangelogFileType: FileType = ChangelogFileType(None)

  def ChangelogFileType(date: DateTime, version: String, changes: String): FileType =
    ChangelogFileType(Some(NewReleaseSection(date, version, changes)))

  private[this] case class NewReleaseSection(date: DateTime, version: String, changes: String)

  private[this] def ChangelogFileType(newChange: Option[NewReleaseSection]): FileType = {

    val template =
      """
        |## {{date}} - Version {{version}}
        |
        |Release changes:
        |
        |{{changes}} 
      """.stripMargin

    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/CHANGELOG.md.template",
      outputPath = "CHANGELOG.md",
      replacements = Map.empty,
      fileSections = newChange map { change =>
        FileSection(
          appendPosition = AppendAfter("""# Changelog""".r),
          template = template,
          replacements = Map(
            "date"    -> change.date.asReplaceable,
            "version" -> change.version.asReplaceable,
            "changes" -> change.changes.asReplaceable
          )
        )
      } toList
    )
  }

  def ReadmeFileType(ghSettings: GitHubSettings, startYear: Option[Int]): FileType = {

    val template =
      """
        |# Copyright
        |
        |{{name}} is designed and developed by {{organizationName}}
        |
        |Copyright (C) {{year}} {{organizationName}}. <{{organizationHomePage}}>
      """.stripMargin

    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/README.md.template",
      outputPath = "README.md",
      replacements = Map("name" -> ghSettings.project.asReplaceable),
      fileSections = List(
        FileSection(
          appendPosition = AppendAtTheEnd,
          template = template,
          replacements = Map(
            "year"                 -> replaceableYear(startYear).asReplaceable,
            "name"                 -> ghSettings.project.asReplaceable,
            "organizationName"     -> ghSettings.organizationName.asReplaceable,
            "organizationHomePage" -> ghSettings.organizationHomePage.asReplaceable
          ),
          shouldAppend = !_.contains("# Copyright")
        )
      )
    )
  }

  object syntax {

    implicit def ioListSyntax[T](list: List[T]): IOReplaceableListOps[T] =
      new IOReplaceableListOps(list)

    implicit def ioTSyntax[T](t: T): IOReplaceableOps[T] = new IOReplaceableOps(t)

    implicit def dateSyntax(t: DateTime): IOReplaceableDateOps = new IOReplaceableDateOps(t)

    final class IOReplaceableOps[T](t: T) {

      def asReplaceable: ReplaceableT[T] = ReplaceableT(t)

    }

    final class IOReplaceableListOps[T](list: List[T]) {

      def asReplaceable: ReplaceableList[T] = ReplaceableList[T](list)

    }

    final class IOReplaceableDateOps(date: DateTime) {

      private[this] val formatter = DateTimeFormat.forPattern("MM/dd/yyyy")

      def asReplaceable: Replaceable = ReplaceableT(date.toString(formatter))

    }
  }
}
