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

import de.heikoseeberger.sbtheader.AutomateHeaderPlugin
import de.heikoseeberger.sbtheader.HeaderKey.headers
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys._
import sbt._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.model._
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.templates._
import sbtorgpolicies.utils.getEnvVarOrElse

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
      sharedScoverageSettings() ++
      scalafmtSettings ++
      orgFilesTasks ++
      orgFileValidationTasks ++
      orgEnforcementSettingsTasks ++
      orgBashTasks ++
      sbtDependenciesSettings ++
      fileValidationDefaultSettings ++
      AutomateHeaderPlugin.automateFor(Compile, Test)

  lazy val orgCommonDefaultSettings = Seq(
    orgGithubSetting := GitHubSettings(
      organization = "47deg",
      project = name.value,
      organizationName = "47 Degrees",
      groupId = "com.47deg",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    orgGithubTokenSetting := None,
    orgGithubOpsSetting := new GitHubOps(
      orgGithubSetting.value.organization,
      orgGithubSetting.value.project,
      orgGithubTokenSetting.value),
    orgLicenseSetting := ApacheLicense,
    headers := Map(
      "scala" -> Apache2_0(replaceableYear(startYear.value), "47 Degrees, LLC. <http://www.47deg.com>")
    ),
    orgMaintainersSetting := List(Dev("47degdev", Some("47 Degrees (twitter: @47deg)"), Some("hello@47deg.com"))),
    orgContributorsSetting := Nil,
    orgCommitBranchSetting := "master",
    orgCommitMessageSetting := "Updates policy files from SBT",
    orgTargetDirectorySetting := resourceManaged.value / "org-policies",
    orgEnforcedFilesSetting := List(
      LicenseFileType(orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      ContributingFileType(orgGithubSetting.value),
      AuthorsFileType(orgGithubSetting.value, orgMaintainersSetting.value, orgContributorsSetting.value),
      NoticeFileType(orgGithubSetting.value, orgLicenseSetting.value, startYear.value),
      VersionSbtFileType,
      ChangelogFileType,
      ReadmeFileType(orgGithubSetting.value, startYear.value)
    ),
    orgTemplatesDirectorySetting := (resourceDirectory in Compile).value / "org" / "templates",
    commands += orgAfterCISuccessCommand,
    orgAfterCISuccessCheckSetting := {
      getEnvVarOrElse("TRAVIS_BRANCH") == orgCommitBranchSetting.value &&
      getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false"
    },
    orgAfterCISuccessTaskListSetting := List(
      orgCreateContributorsFile,
      orgCreateFiles,
      orgCommitPolicyFiles,
      orgPublishRelease
    )
  )
}
