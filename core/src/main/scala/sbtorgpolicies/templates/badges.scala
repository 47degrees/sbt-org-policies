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

package sbtorgpolicies.templates

import cats.syntax.option._
import sbtorgpolicies.model.{ApacheLicense, License, MITLicense}

object badges {

  case class BadgeInformation(
      owner: String,
      repo: String,
      branch: String,
      sbtPlugin: Boolean,
      libOrg: String,
      libName: String,
      libVersion: String,
      scalaBinaryVersion: String,
      sbtBinaryVersion: String,
      scalaJSV: Option[String] = None,
      license: Option[License] = None)

  case class BadgeIcon(title: String, icon: String, url: String)

  type BadgeBuilder = (BadgeInformation) => Badge

  abstract class Badge(info: BadgeInformation) {
    def badgeIcon: Option[BadgeIcon]
    def asMarkDown: Option[String] = badgeIcon map { badge =>
      s"[![${badge.title}](${badge.icon})](${badge.url})"
    }
  }

  case class CodecovBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "codecov.io",
        icon = s"http://codecov.io/github/${info.owner}/${info.repo}/coverage.svg?branch=${info.branch}",
        url = s"http://codecov.io/github/${info.owner}/${info.repo}?branch=${info.branch}"
      ).some
  }

  case class GitHubIssuesBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "GitHub Issues",
        icon = s"https://img.shields.io/github/issues/${info.owner}/${info.repo}.svg",
        url = s"https://github.com/${info.owner}/${info.repo}/issues"
      ).some
  }

  case class GitterBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = s"Join the chat at https://gitter.im/${info.owner}/${info.repo}",
        icon = s"https://badges.gitter.im/${info.owner}/${info.repo}.svg",
        url =
          s"https://gitter.im/${info.owner}/${info.repo}?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge"
      ).some
  }

  case class LicenseBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      (info.license match {
        case Some(ApacheLicense) =>
          "license-Apache%202-blue".some
        case Some(MITLicense) =>
          "License-MIT-blue".some
        case _ => None
      }) map { icon =>
        BadgeIcon(
          "License",
          s"https://img.shields.io/badge/$icon.svg",
          s"https://raw.githubusercontent.com/${info.owner}/${info.repo}/master/LICENSE"
        )
      }
  }

  case class MavenCentralBadge(info: BadgeInformation) extends Badge(info) {

    val url: String =
      if (info.sbtPlugin) {
        val nameWithSuffix = s"${info.libName}_${info.scalaBinaryVersion}_${info.sbtBinaryVersion}"
        s"https://repo1.maven.org/maven2/${info.libOrg.replaceAllLiterally(".", "/")}/$nameWithSuffix"
      } else {
        s"https://oss.sonatype.org/#nexus-search;gav~${info.libOrg}~${info.libName}*"
      }

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "Maven Central",
        icon = s"https://img.shields.io/badge/maven%20central-${info.libVersion}-green.svg",
        url = url
      ).some
  }

  case class ScalaJSBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] = info.scalaJSV map { sjsV =>
      val sjsVSplit = raw"^(\d+)\.(\d+)\.(\d+)(.*)+".r

      val badgeVersion = sjsV match {
        case sjsVSplit(major, minor, patch, other) if ((major == "0") && (minor == "6")) =>
          patch match {
            case "17" | "18" | "19" | "20" => s"$major.$minor.17"
            case "21"                      => "1.0.0-M1"
            case _                         => sjsV
          }
        case _ => sjsV
      }

      BadgeIcon(
        title = "Scala.js",
        icon = s"http://scala-js.org/assets/badges/scalajs-$badgeVersion.svg",
        url = "http://scala-js.org"
      )
    }
  }

  case class ScalaLangBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "Latest version",
        icon =
          s"https://img.shields.io/badge/${info.libName.replaceAllLiterally("-", "--")}-${info.libVersion}-green.svg",
        url = s"https://index.scala-lang.org/${info.owner}/${info.repo}"
      ).some
  }

  case class TravisBadge(info: BadgeInformation) extends Badge(info) {

    override def badgeIcon: Option[BadgeIcon] =
      BadgeIcon(
        title = "Build Status",
        icon = s"https://travis-ci.org/${info.owner}/${info.repo}.svg?branch=${info.branch}",
        url = s"https://travis-ci.org/${info.owner}/${info.repo}"
      ).some
  }

}
