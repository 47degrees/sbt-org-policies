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

import sbt._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.model._
import sbtorgpolicies.runnable._
import sbtorgpolicies.rules.Validation
import sbtorgpolicies.templates.FileType
import sbtorgpolicies.templates.badges.BadgeBuilder

trait OrgPoliciesKeys extends OrgPoliciesSettingsKeys with OrgPoliciesTaskKeys with CommandKeys with TaskKeysUtils

object OrgPoliciesKeys extends OrgPoliciesKeys

sealed trait OrgPoliciesSettingsKeys {

  // (Settings keys are ordered alphabetically)

  val orgAfterCISuccessCheckSetting: SettingKey[Boolean] =
    settingKey[Boolean](
      "Defines the condition that the orgAfterCISuccess command will check before running the orgAfterCISuccessTaskListSetting list.")

  val orgAfterCISuccessTaskListSetting: SettingKey[List[RunnableItemConfigScope[_]]] =
    settingKey[List[RunnableItemConfigScope[_]]](
      "Defines the list of tasks that should be executed once the Continuous integration build has finished successfully.")

  val orgBadgeListSetting: SettingKey[List[BadgeBuilder]] =
    settingKey[List[BadgeBuilder]]("Defines the list of badges that will be included in the README file.")

  val orgCommitBranchSetting: SettingKey[String] =
    settingKey[String]("Defines the target git branch where policy files will be committed.")

  val orgCommitMessageSetting: SettingKey[String] =
    settingKey[String]("Defines the default commit message when policy files are pushed to Github.")

  val orgContributorsSetting: SettingKey[List[Dev]] =
    settingKey[List[Dev]]("List of Devs involved in the project development.")

  val orgEnforcedFilesSetting: SettingKey[List[FileType]] =
    settingKey[List[FileType]]("List of files must exists and should be checked by the sbt-org-policies plugin.")

  val orgGithubSetting: SettingKey[GitHubSettings] =
    settingKey[GitHubSettings]("General Organization Github Settings. 47 Degrees data by default.")

  val orgGithubTokenSetting: SettingKey[String] =
    settingKey[String](
      "Environment variable name where the Github token will be stored. Default value: 'ORG_GITHUB_TOKEN'")

  val orgGithubOpsSetting: SettingKey[GitHubOps] =
    settingKey[GitHubOps]("Github wrapper to interact with github4s and the Github API.")

  val orgLicenseSetting: SettingKey[License] =
    settingKey[License]("Organization License Setting.")

  val orgMaintainersSetting: SettingKey[List[Dev]] =
    settingKey[List[Dev]]("List of Maintainers of the project.")

  val orgProjectName: SettingKey[String] =
    settingKey[String]("Name that will be used in docs. By default, the module name will be used.")

  val orgScriptTaskListSetting: SettingKey[List[RunnableItemConfigScope[_]]] =
    settingKey[List[RunnableItemConfigScope[_]]](
      "Defines the list of tasks that should be executed to figure out whether the build is correct. " +
        "By default, it'd be something like this: 'sbt clean coverage compile test coverageReport'")

  val orgSupportedScalaJSVersion: SettingKey[Option[String]] =
    settingKey[Option[String]]("Defines the ScalaJS version supported. None by default")

  val orgTargetDirectorySetting: SettingKey[File] =
    SettingKey[File]("orgTargetDirectory", "Where sbt-org-policies output goes.")

  val orgTemplatesDirectorySetting: SettingKey[File] = settingKey[File](
    "Optional. Directory where are placed the different templates it'll be used. " +
      "By default, it'll be the resourcesDirectory + '/org/templates'")

  val orgValidationListSetting: SettingKey[List[Validation]] =
    settingKey[List[Validation]]("Validation list the plugin must check")

  val orgUpdateDocFilesSetting: SettingKey[List[File]] =
    settingKey[List[File]]("List of files and directories whose replace blocks will be replaced with the new values.")

  val orgUpdateDocFilesCommitSetting: SettingKey[Boolean] =
    settingKey[Boolean]("Determines if the files should be committed after the update. 'true' by default.")

  val orgUpdateDocFilesReplacementsSetting: SettingKey[Map[String, String]] =
    settingKey[Map[String, String]](
      "Replacements for the replace blocks. " +
        "By default, the regular expression \"\\\\d+.\\\\d+.\\\\d+\" will be replaced by the project version.")
}

sealed trait OrgPoliciesTaskKeys {

  // (Task keys are ordered alphabetically)

  val orgCheckSettings: TaskKey[Unit] = taskKey[Unit]("Task to check the project settings.")

  val orgCompile: TaskKey[Unit] =
    taskKey[Unit]("Just a (compile in Compile) but ignoring the result (Analysis type) and returning Unit.")

  val orgCreateFiles: TaskKey[Unit] =
    taskKey[Unit]("Task to create the files that must exists in a project to accomplish the Organization's policies.")

  val orgFetchContributors: TaskKey[List[Dev]] = taskKey[List[Dev]]("Task to fetch the project's contributors.")

  val orgPublishReleaseTask: TaskKey[Unit] = taskKey[Unit](
    "This task allows to publish the artifact (publishSigned) in case of dealing with an snapshot, or, " +
      "releasing a new version in any other case.")

  val orgScalafmtInc: TaskKey[Unit] = taskKey[Unit]("Incrementally format modified sources")

  val orgValidateFiles: TaskKey[Unit] = taskKey[Unit]("Validates all files according to a set of policy rules.")

  val orgUpdateDocFiles: TaskKey[Unit] =
    taskKey[Unit](
      "Updates all replace blocks in the defined files and directories and commits both, " +
        "the modified files and the policy files, only if `orgUpdateDocFilesCommitSetting` is `true`")

}

sealed trait CommandKeys {

  val orgAfterCISuccessCommandKey = "orgAfterCISuccess"

  val orgPublishReleaseCommandKey = "orgPublishRelease"

  val orgScriptCICommandKey = "orgScriptCI"

}

sealed trait TaskKeysUtils {

  def onlyRootUnitTask(baseDir: File, rootDir: File, log: Logger)(functionTask: => Unit): Unit =
    onlyRootTask[Unit](baseDir, rootDir, log, (): Unit)(functionTask)

  def onlyRootTask[T](baseDir: File, rootDir: File, log: Logger, defaultValue: T)(functionTask: => T): T = {
    if (baseDir.getAbsolutePath == rootDir.getAbsolutePath) {
      functionTask
    } else {
      log.info(s"Skipping task for module '${IO.relativize(rootDir, baseDir).getOrElse(baseDir.getName)}'")
      defaultValue
    }
  }

  def printList(message: String, list: List[String]): String = s"$message\n * ${list.mkString("\n * ")}"

}
