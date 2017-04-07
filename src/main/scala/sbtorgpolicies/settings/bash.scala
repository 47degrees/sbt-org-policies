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

import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers.OptNotSpace
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.model.Dev
import sbtrelease.ReleaseStateTransformations.reapply

trait bash {

  val orgBashTasks =
    Seq(
      orgCommitPolicyFiles := Def.task {
        val ghOps: GitHubOps = orgGithubOpsSetting.value
        ghOps.commitFiles(
          baseDir = (baseDirectory in LocalRootProject).value,
          branch = orgCommitBranchSetting.value,
          message = s"${orgCommitMessageSetting.value} [ci skip]",
          files = orgEnforcedFilesSetting.value.map(_.outputPath)
        ) match {
          case Right(Some(_)) =>
            streams.value.log.info("Policy files committed successfully")
          case Right(None) =>
            streams.value.log.info("No changes detected in policy files. Skipping commit")
          case Left(e) =>
            streams.value.log.error(s"Error committing files")
            e.printStackTrace()
        }
      }.value,
      orgPublishRelease := Def.taskDyn {

        val buildV       = (version in ThisBuild).value
        val scalaV       = scalaVersion.value
        val crossV       = crossScalaVersions.value
        val isSnapshotV  = buildV.endsWith("-SNAPSHOT")
        val isLastScalaV = crossV.lastOption.exists(_ == scalaV)

        streams.value.log.info(s"""orgPublishRelease Initiated
             |Build Version = $buildV
             |Scala Version = $scalaV
             |crossScalaVersions = $crossV
             |isSnapshotV = $isSnapshotV
             |isLastScalaV = $isLastScalaV
         """.stripMargin)

        (isSnapshotV, isLastScalaV) match {
          case (true, _) =>
            streams.value.log.info("SNAPSHOT version detected, skipping release and publishing it...")
            Def.task(publishSigned.value)
          case (false, true) =>
            streams.value.log.info("Release Version detected, starting the release process...")
            s"git checkout ${orgCommitBranchSetting.value}".!
            "git reset --hard HEAD".!
            "git clean -f".!
            "git pull origin master".!
            "sbt release".!
            Def.task((): Unit)
          case _ =>
            streams.value.log.info(s"Release Version detected but it'll be skipped for Scala $scalaV...")
            Def.task((): Unit)
        }
      }.value
    )

  val orgAfterCISuccessCommand: Command = Command(afterCISuccessCommandKey)(_ => OptNotSpace) { (st, _) =>
    val extracted = Project.extract(st)

    val afterSuccessCheck = extracted.get(orgAfterCISuccessCheckSetting)

    if (afterSuccessCheck) {

      val scalaV     = extracted.get(scalaVersion)
      val crossV     = extracted.get(crossScalaVersions)
      val baseDir    = extracted.get(baseDirectory)
      val maybeToken = extracted.get(orgGithubTokenSetting)
      val rootDir    = extracted.get(baseDirectory in LocalRootProject)

      val isLastScalaV = crossV.lastOption.exists(_ == scalaV)
      val isRootModule = baseDir.getAbsolutePath == rootDir.getAbsolutePath
      val taskList     = extracted.get(orgAfterCISuccessTaskListSetting)

      val executableTasks = taskList.filter { tsk =>
        (isLastScalaV || tsk.crossScalaVersionsScope) &&
        (isRootModule || tsk.allModulesScope)
      } map (_.task)

      if (executableTasks.nonEmpty) {

        st.log.info(
          s"[orgAfterCISuccess] Initiating orgAfterCISuccessCommand " +
            s"with this set of tasks: ${toStringListTask(executableTasks)}")

        val (fetchContributorsState, contributorList) =
          if (maybeToken.nonEmpty) {
            extracted.runTask[List[Dev]](orgFetchContributors, st)
          } else (st, Nil)

        val newState = reapply(Seq[Setting[_]](orgContributorsSetting := contributorList), fetchContributorsState)

        executableTasks map (Project.extract(newState).runTask(_, newState))
        newState
      } else {
        st.log.info("[orgAfterCISuccess] No tasks to execute")
        st
      }
    } else {
      st.log.info("[orgAfterCISuccess] orgAfterCISuccessCheckSetting is false, skipping tasks after CI success")
      st
    }
  }

  private[this] def toStringListTask[T](taskList: List[TaskKey[T]]): String =
    s"""
      |${taskList.map(toStringTask).mkString("\n")}
      |
      |""".stripMargin

  private[this] def toStringTask[T](task: TaskKey[T]): String =
    s"* ${task.key.label}${task.key.description map (d => s": $d") getOrElse ""}"
}
