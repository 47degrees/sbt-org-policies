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

//import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
//import de.heikoseeberger.sbtheader.HeaderKey.headers
//import de.heikoseeberger.sbtheader.license.Apache2_0
//import dependencies.DependenciesPlugin.autoImport._
//import scoverage.ScoverageKeys
//import scoverage.ScoverageKeys.coverageEnabled
//import sbtorgpolicies.runnable.SetSetting
import sbt.Keys._
import sbt._
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.model._
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates._
import sbtorgpolicies.templates.badges._

trait DefaultSettings extends AllSettings {

  lazy val orgDefaultSettings: Seq[Setting[_]] =
    orgCommonDefaultSettings ++
      sharedCommonSettings ++
      sharedCommonDependencies ++
      sharedReleaseProcess ++
      credentialSettings ++
      sharedPublishSettings ++
      sharedBuildSettings ++
      scalaDependencyOverrides ++
      //sharedScoverageSettings() ++
      scalafmtSettings ++
      orgFilesTasks ++
      orgFilesSettings ++
      orgFileValidationTasks ++
      orgEnforcementSettingsTasks ++
      orgBashTasks ++
      orgCommonTasks
  //++
//      sbtDependenciesSettings ++
//      sbtMicrositesSettings ++
      //AutomateHeaderPlugin.automateFor(Compile, Test)

  lazy val orgCommonDefaultSettings = Seq(
    orgProjectName := name.value,
    orgGithubSetting := GitHubSettings(
      organization = "47deg",
      project = (name in LocalRootProject).value,
      organizationName = "47 Degrees",
      groupId = "com.47deg",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    orgGithubTokenSetting := "ORG_GITHUB_TOKEN",
    orgGithubOpsSetting := new GitHubOps(
      orgGithubSetting.value.organization,
      orgGithubSetting.value.project,
      getEnvVar(orgGithubTokenSetting.value)),
    orgLicenseSetting := ApacheLicense,
//    headers := Map(
//      "scala" -> Apache2_0(replaceableYear(startYear.value), "47 Degrees, LLC. <http://www.47deg.com>")
//    ),
    orgMaintainersSetting := List(Dev("47degdev", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
    orgContributorsSetting := Nil,
    orgCommitBranchSetting := "master",
    orgCommitMessageSetting := "Updates policy files from SBT",
    orgTargetDirectorySetting := resourceManaged.value / "org-policies",
    orgSupportedScalaJSVersion := None,
    orgBadgeListSetting := List(
      TravisBadge.apply(_),
      MavenCentralBadge.apply(_),
      LicenseBadge.apply(_),
      GitterBadge.apply(_),
      GitHubIssuesBadge.apply(_)
    ) ++ guard(!sbtPlugin.value)(ScalaLangBadge.apply(_)),
    orgEnforcedFilesSetting := List(
      LicenseFileType(orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      ContributingFileType(orgProjectName.value, orgGithubSetting.value),
      AuthorsFileType(
        orgProjectName.value,
        orgGithubSetting.value,
        orgMaintainersSetting.value,
        orgContributorsSetting.value),
      NoticeFileType(orgProjectName.value, orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      VersionSbtFileType,
      ChangelogFileType,
      ReadmeFileType(
        orgProjectName.value,
        orgGithubSetting.value,
        startYear.value,
        orgLicenseSetting.value,
        orgCommitBranchSetting.value,
        sbtPlugin.value,
        name.value,
        version.value,
        scalaBinaryVersion.value,
        sbtBinaryVersion.value,
        orgSupportedScalaJSVersion.value,
        orgBadgeListSetting.value
      ),
      ScalafmtFileType,
      TravisFileType(crossScalaVersions.value, orgScriptCICommandKey, orgAfterCISuccessCommandKey)
    ),
    orgTemplatesDirectorySetting := (resourceDirectory in Compile).value / "org" / "templates",
    commands ++= Seq(orgScriptCICommand, orgPublishReleaseCommand, orgAfterCISuccessCommand),
    orgAfterCISuccessCheckSetting := {
      getEnvVarOrElse("TRAVIS_BRANCH") == orgCommitBranchSetting.value &&
      getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false"
    },
    orgAfterCISuccessTaskListSetting := List(
      orgUpdateDocFiles.asRunnableItem,
//      depUpdateDependencyIssues.asRunnableItem,
      orgPublishReleaseTask.asRunnableItem(allModules = true, aggregated = false, crossScalaVersions = true)
    ) ++ guard(((baseDirectory in LocalRootProject).value / "docs").exists() && !version.value.endsWith("-SNAPSHOT"))(
      defaultPublishMicrosite),
    orgScriptTaskListSetting := List(
      orgValidateFiles.asRunnableItem,
      orgCheckSettings.asRunnableItem,
      (clean in Global).asRunnableItemFull,
//      SetSetting(coverageEnabled in Global, true).asRunnableItem,
      (compile in Compile).asRunnableItemFull,
      (test in Test).asRunnableItemFull
//      (ScoverageKeys.coverageReport in Test).asRunnableItemFull
    )
  )
}
