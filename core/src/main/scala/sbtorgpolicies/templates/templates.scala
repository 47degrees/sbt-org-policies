/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
import sbtorgpolicies.rules.ValidationFunctions._
import sbtorgpolicies.templates.badges.{BadgeBuilder, BadgeInformation}
import sbtorgpolicies.templates.sectionTemplates._
import sbtorgpolicies.templates.syntax._
import sbtorgpolicies.templates.utils._
import sbtorgpolicies.utils._

import scala.language.{implicitConversions, postfixOps}

package object templates {

  val versionFilePath: String  = "version.sbt"
  val scalafmtFilePath: String = ".scalafmt.conf"
  val travisFilePath: String   = ".travis.yml"

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
      finalVersionOnly = false,
      templatePath = licenseFile,
      outputPath = "LICENSE",
      replacements = Map(
        "year"                 -> replaceableYear(startYear).asReplaceable,
        "organizationName"     -> ghSettings.organizationName.asReplaceable,
        "organizationHomePage" -> ghSettings.organizationHomePage.asReplaceable
      ),
      validations = List(requiredStrings(List(license.name)))
    )
  }

  def ContributingFileType(projectName: String, ghSettings: GitHubSettings) = FileType(
    mandatory = true,
    overWritable = true,
    finalVersionOnly = false,
    templatePath = "templates/CONTRIBUTING.md.template",
    outputPath = "CONTRIBUTING.md",
    replacements = Map(
      "name"              -> projectName.asReplaceable,
      "project"           -> ghSettings.project.asReplaceable,
      "organization"      -> ghSettings.organization.asReplaceable,
      "organizationName"  -> ghSettings.organizationName.asReplaceable,
      "organizationEmail" -> ghSettings.organizationEmail.asReplaceable
    )
  )

  def AuthorsFileType(
      projectName: String,
      ghSettings: GitHubSettings,
      maintainers: List[Dev],
      contributors: List[Dev]): FileType = {

    def devTemplate(dev: Dev): String =
      dev.name match {
        case Some(n) => s"$n <[${dev.id}](https://github.com/${dev.id})>"
        case None    => s"[${dev.id}](https://github.com/${dev.id})"
      }

    def devListStrings(list: List[Dev]): List[String] = list.map(_.id) ++ list.flatMap(_.name)

    FileType(
      mandatory = true,
      overWritable = true,
      finalVersionOnly = false,
      templatePath = "templates/AUTHORS.md.template",
      outputPath = "AUTHORS.md",
      replacements = Map(
        "name"         -> projectName.asReplaceable,
        "maintainers"  -> maintainers.map(devTemplate).asReplaceable,
        "contributors" -> contributors.map(devTemplate).asReplaceable
      ),
      validations = List(requiredStrings(devListStrings(maintainers ++ contributors)))
    )
  }

  def NoticeFileType(
      projectName: String,
      ghSettings: GitHubSettings,
      license: License,
      startYear: Option[Int]): FileType = {

    FileType(
      mandatory = true,
      overWritable = true,
      finalVersionOnly = false,
      templatePath = "templates/NOTICE.md.template",
      outputPath = "NOTICE.md",
      replacements = Map(
        "year"             -> replaceableYear(startYear).asReplaceable,
        "name"             -> projectName.asReplaceable,
        "organizationName" -> ghSettings.organizationName.asReplaceable,
        "licenseName"      -> license.name.asReplaceable
      ),
      validations = List(requiredStrings(List(projectName, license.name)))
    )
  }

  def VersionSbtFileType: FileType =
    FileType(
      mandatory = true,
      overWritable = false,
      finalVersionOnly = false,
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
      finalVersionOnly = true,
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
      projectName: String,
      ghSettings: GitHubSettings,
      startYear: Option[Int],
      license: License,
      branch: String,
      sbtPlugin: Boolean,
      libraryName: String,
      libraryVersion: String,
      scalaBinaryVersion: String,
      sbtBinaryVersion: String,
      scalaJSVersion: Option[String],
      badgeBuilderList: List[BadgeBuilder] = Nil): FileType = {

    def replaceSection(title: String, top: Boolean): ReplaceSection =
      ReplaceSection(
        from = markdownComment(title, scape = true).r,
        to = markdownComment(title, start = false, scape = true).r,
        defaultTop = top)

    def replaceableBadges: Replaceable = {
      val info = BadgeInformation(
        owner = ghSettings.organization,
        repo = ghSettings.project,
        branch = branch,
        sbtPlugin = sbtPlugin,
        libOrg = ghSettings.groupId,
        libName = libraryName,
        libVersion = libraryVersion,
        scalaBinaryVersion = scalaBinaryVersion,
        sbtBinaryVersion = sbtBinaryVersion,
        scalaJSV = scalaJSVersion,
        license = license.some
      )
      badgeBuilderList.map(_(info)).map(_.asMarkDown.getOrElse("")).mkString(" ").asReplaceable
    }

    def readmeSections(name: String): List[String] = List(s"$name in the wild")

    FileType(
      mandatory = true,
      overWritable = false,
      finalVersionOnly = true,
      templatePath = "templates/README.md.template",
      outputPath = "README.md",
      replacements = Map("name" -> ghSettings.project.asReplaceable),
      fileSections = List(
        FileSection(
          appendPosition = replaceSection(copyrightSectionTitle, top = false),
          template = copyrightSectionTemplate,
          replacements = Map(
            "year"                 -> replaceableYear(startYear).asReplaceable,
            "name"                 -> projectName.asReplaceable,
            "organizationName"     -> ghSettings.organizationName.asReplaceable,
            "organizationHomePage" -> ghSettings.organizationHomePage.asReplaceable
          ),
          shouldAppend = content => {
            content.contains(markdownComment(copyrightSectionTitle)) ||
            !content.contains(s"# $copyrightSectionTitle")
          }
        ),
        FileSection(
          appendPosition = replaceSection(badgesSectionTitle, top = true),
          template = badgesSectionTemplate,
          replacements = Map("badges" -> replaceableBadges)
        )
      ),
      validations = List(requiredStrings(readmeSections(projectName)))
    )
  }

  def ScalafmtFileType: FileType = {

    FileType(
      mandatory = true,
      overWritable = false,
      finalVersionOnly = false,
      templatePath = "templates/scalafmt.conf.template",
      outputPath = scalafmtFilePath,
      replacements = Map.empty
    )
  }

  def TravisFileType(crossScalaV: Seq[String], scriptCICommand: String, afterCISuccessCommand: String): FileType = {

    import sbtorgpolicies.model.YamlFormats._

    FileType(
      mandatory = true,
      overWritable = false,
      finalVersionOnly = false,
      templatePath = "templates/travis.yml.template",
      outputPath = travisFilePath,
      replacements = Map(
        "crossScalaVersions" -> crossScalaV.toYaml.prettyPrint.asReplaceable
      ),
      validations = List(
        validTravisFile(
          crossScalaV,
          Seq(scriptCICommand),
          Seq(afterCISuccessCommand)
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
