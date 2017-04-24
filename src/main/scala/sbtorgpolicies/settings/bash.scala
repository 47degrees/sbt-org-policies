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
import sbtorgpolicies.model.Dev
import sbtorgpolicies.runnable._
import sbtorgpolicies.utils._
import sbtrelease.ReleaseStateTransformations.reapply

trait bash {

  val orgBashTasks =
    Seq(
      orgPublishReleaseTask := Def.task {
        val scalaV = scalaVersion.value
        s"sbt ++$scalaV $orgPublishReleaseCommandKey".!
        (): Unit
      }.value
    )

  val orgPublishReleaseCommand: Command = Command(orgPublishReleaseCommandKey)(_ => OptNotSpace) { (st, _) =>
    val extracted = Project.extract(st)

    val buildV    = extracted.get(version in ThisBuild)
    val scalaV    = extracted.get(scalaVersion)
    val crossV    = extracted.get(crossScalaVersions)
    val orgBranch = extracted.get(orgCommitBranchSetting)

    val isLastScalaV = crossV.lastOption.exists(_ == scalaV)
    val isSnapshotV  = buildV.endsWith("-SNAPSHOT")

    st.log.info(s"""orgPublishRelease Command Initiated
                              |Build Version = $buildV
                              |Scala Version = $scalaV
                              |crossScalaVersions = $crossV
                              |isSnapshotV = $isSnapshotV
                              |isLastScalaV = $isLastScalaV
         """.stripMargin)

    val finalState = (isSnapshotV, isLastScalaV) match {
      case (true, _) =>
        st.log.info("SNAPSHOT version detected, skipping release and publishing it...")

        val ref = extracted.get(thisProjectRef)

        extracted.runAggregated[Unit](publishSigned in Global in ref, st)
      case (false, true) =>
        st.log.info("Release Version detected, starting the release process...")

        "git reset --hard HEAD" ::
          "git clean -f" ::
          s"git checkout $orgBranch" ::
          "git pull origin master" ::
          "release" ::
          st
      case _ =>
        st.log.info(s"Release Version detected but it'll be skipped for Scala $scalaV...")
        st
    }

    finalState
  }

  val orgScriptCICommand: Command = Command(orgScriptCICommandKey)(_ => OptNotSpace) { (st, _) =>
    runTaskListCommand("orgScriptCI", orgScriptTaskListSetting, st)
  }

  val orgAfterCISuccessCommand: Command = Command(orgAfterCISuccessCommandKey)(_ => OptNotSpace) { (st, _) =>
    val extracted = Project.extract(st)

    if (extracted.get(orgAfterCISuccessCheckSetting)) {

      val beforeTasksState = (st: State) => {
        val envVarToken = extracted.get(orgGithubTokenSetting)
        val maybeToken  = getEnvVar(envVarToken)

        val (fetchContributorsState, contributorList) =
          if (maybeToken.nonEmpty) {
            extracted.runTask[List[Dev]](orgFetchContributors, st)
          } else (st, Nil)

        reapply(Seq[Setting[_]](orgContributorsSetting := contributorList), fetchContributorsState)
      }

      runTaskListCommand(
        "orgAfterCISuccess",
        orgAfterCISuccessTaskListSetting,
        st,
        beforeTasksState
      )

    } else {
      st.log.info("[orgAfterCISuccess] orgAfterCISuccessCheckSetting is false, skipping tasks after CI success")
      st
    }
  }

  private[this] def runTaskListCommand(
      commandName: String,
      runnableItemListSettingKey: SettingKey[List[RunnableItemConfigScope[_]]],
      st: State,
      stateToState: (State) => State = st => st): State = {

    val extracted = Project.extract(st)

    val scalaV  = extracted.get(scalaVersion)
    val crossV  = extracted.get(crossScalaVersions)
    val baseDir = extracted.get(baseDirectory)
    val rootDir = extracted.get(baseDirectory in LocalRootProject)

    val isLastScalaV = crossV.lastOption.exists(_ == scalaV)
    val isRootModule = baseDir.getAbsolutePath == rootDir.getAbsolutePath

    val runnableItemList: List[RunnableItemConfigScope[_]] =
      extracted
        .get(runnableItemListSettingKey)
        .filter { runnableItem =>
          (isLastScalaV || runnableItem.crossScalaVersions) && (isRootModule || runnableItem.allModules)
        }

    if (runnableItemList.nonEmpty) {

      val stateToStateResult = stateToState(st)

      stateToStateResult.log.info(
        s"[$commandName] Initiating with this set of items: ${toStringListRunnableItems(runnableItemList)}")

      runnableItemList.foldLeft(stateToStateResult) { (currentState, item) =>
        val extractedRunnable: Extracted = Project.extract(currentState)

        item match {
          case RunnableItemConfigScope(RunnableTask(task), true, true, _) =>
            val ref: Reference = extractedRunnable.get(thisProjectRef)
            extractedRunnable.runAggregated(task in ref, currentState)

          case RunnableItemConfigScope(RunnableTask(task), _, _, _) =>
            extractedRunnable.runTask(task, currentState)._1

          case RunnableItemConfigScope(RunnableSetSetting(setSetting), _, _, _) =>
            reapply(Seq[Setting[_]](setSetting.setting := setSetting.value), currentState)

          case RunnableItemConfigScope(RunnableProcess(process), _, _, _) =>
            Command.process(process, currentState)
        }
      }
    } else {
      st.log.info(s"[$commandName] No runnable items to execute")
      st
    }
  }

  private[this] def toStringListRunnableItems(list: List[RunnableItemConfigScope[_]]): String =
    s"""
      |${list.map(toStringRunnableItem).mkString("\n")}
      |
      |""".stripMargin

  private[this] def toStringRunnableItem(runnableItem: RunnableItemConfigScope[_]): String = {
    val (label: String, description: Option[String]) = runnableItem match {

      case RunnableItemConfigScope(RunnableTask(task), _, _, _) =>
        (task.key.label, task.key.description)

      case RunnableItemConfigScope(RunnableSetSetting(setSetting), _, _, _) =>
        (s"Setting ${setSetting.setting.key.label} to ${setSetting.value}", setSetting.setting.key.description)

      case RunnableItemConfigScope(RunnableProcess(process), _, _, _) =>
        (s"Running process $process", None)
    }

    s"* $label${description map (d => s": $d") getOrElse ""}"
  }
}
