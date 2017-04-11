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

import cats.syntax.option._
import net.jcazevedo.moultingyaml._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sbtorgpolicies.model._
import sbtorgpolicies.templates.badges.{BadgeBuilder, BadgeInformation}
import sbtorgpolicies.templates.sectionTemplates._
import sbtorgpolicies.templates.syntax._
import sbtorgpolicies.templates.utils._
import sbtorgpolicies.utils._

import scala.language.{implicitConversions, postfixOps}

package object templates {

  val versionFilePath: String = "version.sbt"
  val travisFilePath: String  = ".travis.yml"

  type Replacements = Map[String, Replaceable]

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

  private[this] def ChangelogFileType(newChange: Option[NewReleaseSection]): FileType =
    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/CHANGELOG.md.template",
      outputPath = "CHANGELOG.md",
      replacements = Map.empty,
      fileSections = newChange map { change =>
        FileSection(
          appendPosition = AppendAfter("""# Changelog""".r),
          template = changelogSectionTemplate,
          replacements = Map(
            "date"    -> change.date.asReplaceable,
            "version" -> change.version.asReplaceable,
            "changes" -> change.changes.asReplaceable
          )
        )
      } toList
    )

  def ReadmeFileType(
      ghSettings: GitHubSettings,
      startYear: Option[Int],
      license: License,
      branch: String,
      scalaVersion: String,
      badgeBuilderList: List[BadgeBuilder] = Nil): FileType = {

    def replaceableBadges: Replaceable = {
      val info = BadgeInformation(
        owner = ghSettings.organization,
        repo = ghSettings.project,
        branch = branch,
        libOrg = ghSettings.groupId.some,
        libName = ghSettings.project.some,
        scalaV = scalaVersion.some,
        scalaJSV = None,
        license = license.some
      )
      badgeBuilderList.map(_(info)).map(_.asMarkDown.getOrElse("")).mkString(" ").asReplaceable
    }

    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/README.md.template",
      outputPath = "README.md",
      replacements = Map("name" -> ghSettings.project.asReplaceable),
      fileSections = List(
        FileSection(
          appendPosition = replaceSection(copyrightSectionTitle, top = false),
          template = copyrightSectionTemplate,
          replacements = Map(
            "year"                 -> replaceableYear(startYear).asReplaceable,
            "name"                 -> ghSettings.project.asReplaceable,
            "organizationName"     -> ghSettings.organizationName.asReplaceable,
            "organizationHomePage" -> ghSettings.organizationHomePage.asReplaceable
          ),
          shouldAppend = content => {
            content.contains(sectionSep(copyrightSectionTitle)) ||
            !content.contains(s"# $copyrightSectionTitle")
          }
        ),
        FileSection(
          appendPosition = replaceSection(badgesSectionTitle, top = true),
          template = badgesSectionTemplate,
          replacements = Map("badges" -> replaceableBadges)
        )
      )
    )
  }

  def TravisFileType(crossScalaV: Seq[String]): FileType = {

    import sbtorgpolicies.model.YamlFormats._

    FileType(
      mandatory = true,
      overWritable = false,
      templatePath = "templates/travis.yml.template",
      outputPath = travisFilePath,
      replacements = Map(
        "crossScalaVersions" -> crossScalaV.toYaml.prettyPrint.asReplaceable
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
