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

package sbtorgpolicies.settings

import cats.syntax.either._
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers.OptNotSpace
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.io.FileReader
import sbtorgpolicies.model.Dev
import sbtorgpolicies.rules.YamlOps
import sbtorgpolicies.rules.syntax._
import sbtorgpolicies.runnable._
import sbtorgpolicies.templates._
import sbtorgpolicies.utils._
import sbtrelease.ReleaseStateTransformations.reapply

trait bash extends bashCompat {

  val orgPublishReleaseCommand: Command = Command(orgPublishReleaseCommandKey)(_ => OptNotSpace) { (inputState, _) =>
    val st: State = deferredFetchContributorsState(inputState)
    val extracted = Project.extract(st)

    val buildV     = extracted.get(version in ThisBuild)
    val scalaV     = extracted.get(scalaVersion)
    val crossBuild = extracted.get(crossScalaVersions).toList
    val orgBranch  = extracted.get(orgCommitBranchSetting)
    val rootDir    = extracted.get(baseDirectory in LocalRootProject)

    val crossV       = readCrossScalaFromYaml(rootDir, crossBuild, st.log)
    val isLastScalaV = isLastScalaVersion(scalaV, crossV, st.log)
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

        extracted.runAggregated[Unit](publish in Global in ref, st)
        st
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

      runTaskListCommand(
        "orgAfterCISuccess",
        orgAfterCISuccessTaskListSetting,
        st,
        deferredFetchContributorsState
      )

    } else {
      st.log.info("[orgAfterCISuccess] orgAfterCISuccessCheckSetting is false, skipping tasks after CI success")
      st
    }
  }

  private[this] def isLastScalaVersion(scalaV: String, crossV: List[String], logger: Logger): Boolean = {
    crossV.sorted.lastOption match {
      case None =>
        logger.warn("crossScalaVersions is empty")
        false
      case Some(v) if v < scalaV =>
        logger.warn(
          s"Current Scala version ($scalaV) is greater than the major Scala version supported in crossScalaVersions ($crossV)")
        false
      case Some(v) if v == scalaV => true
      case Some(v)                => false
    }
  }

  private[this] def readCrossScalaFromYaml(rootDir: File, defaultCrossV: List[String], logger: Logger): List[String] = {

    import FileReader._
    import YamlOps._

    def verifyVersionsConsistency(yamlVersions: List[String]): List[String] = {
      val notInConfig = yamlVersions.filterNot(defaultCrossV.contains)
      val notInYaml   = defaultCrossV.filterNot(yamlVersions.contains)

      if (notInConfig.nonEmpty) {
        logger.warn(
          s"The Scala versions $notInConfig are defined in $travisFilePath but not in your project configuration")
      }

      if (notInYaml.nonEmpty) {
        logger.warn(
          s"The Scala versions $notInYaml are defined in your project configuration but not in $travisFilePath")
      }
      yamlVersions
    }

    getFileContent((rootDir / travisFilePath).getAbsolutePath) flatMap { content =>
      getFields(content, "scala").mapToString map (_.sorted) map verifyVersionsConsistency
    } valueOr { _ =>
      logger.warn(
        s"Can't read crossScalaVersion from yaml file $travisFilePath, using default [${defaultCrossV.mkString(",")}]")
      defaultCrossV
    }
  }

  private[this] def runTaskListCommand(
      commandName: String,
      runnableItemListSettingKey: SettingKey[List[RunnableItemConfigScope[_]]],
      st: State,
      stateToState: (State) => State = st => st): State = {

    val extracted = Project.extract(st)

    val scalaV  = extracted.get(scalaVersion)
    val baseDir = extracted.get(baseDirectory)
    val rootDir = extracted.get(baseDirectory in LocalRootProject)

    val crossV       = readCrossScalaFromYaml(rootDir, extracted.get(crossScalaVersions).toList, st.log)
    val isLastScalaV = isLastScalaVersion(scalaV, crossV, st.log)
    val isRootModule = baseDir.getAbsolutePath == rootDir.getAbsolutePath

    val runnableItemList: List[RunnableItemConfigScope[_]] = extracted.get(runnableItemListSettingKey)

    val filteredRunnableItemList: List[RunnableItemConfigScope[_]] =
      runnableItemList
        .filter { runnableItem =>
          (isLastScalaV || runnableItem.crossScalaVersions) && (isRootModule || runnableItem.allModules)
        }

    logInfo(commandName, isLastScalaV, isRootModule, runnableItemList, filteredRunnableItemList, st)

    runFilteredCommandList(filteredRunnableItemList, st, stateToState)
  }

  private[this] def runFilteredCommandList(
      runnableList: List[RunnableItemConfigScope[_]],
      st: State,
      stateToState: (State) => State): State = {

    if (runnableList.nonEmpty) {

      runnableList.foldLeft(stateToState(st)) { (currentState, item) =>
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
            process :: currentState
        }
      }
    } else st
  }

  private[this] def deferredFetchContributorsState(st: State) = {
    val extracted = Project.extract(st)

    val envVarToken        = extracted.get(orgGithubTokenSetting)
    val projectMaintainers = extracted.get(orgMaintainersSetting)

    val maybeToken = getEnvVar(envVarToken)

    val (fetchContributorsState, contributorList) =
      if (maybeToken.nonEmpty) {
        extracted.runTask[List[Dev]](orgFetchContributors, st)
      } else (st, Nil)

    val devs = projectMaintainers ++ contributorList

    val setContributorState =
      reapply(Seq[Setting[_]](orgContributorsSetting := contributorList), fetchContributorsState)

    reapply(Seq[Setting[_]](pomExtra := <developers> { devs.map(_.pomExtra) } </developers>), setContributorState)
  }

  private[this] def logInfo(
      commandName: String,
      isLastScalaV: Boolean,
      isRootModule: Boolean,
      runnableItemList: List[RunnableItemConfigScope[_]],
      filteredRunnableItemList: List[RunnableItemConfigScope[_]],
      st: State): Unit = {

    val nonRunnableItems: Set[RunnableItemConfigScope[_]] = runnableItemList.toSet -- filteredRunnableItemList.toSet

    def debugDiscardedItem(i: RunnableItemConfigScope[_]) =
      s"""
         |${toStringRunnableItem(i)}
         |
         |    => Item Info:
         |      # crossScalaVersions = ${i.crossScalaVersions}
         |      # allModules         = ${i.allModules}
         |      # Is last scalaVersion or the runnable item is crossScalaVersions? = ${isLastScalaV || i.crossScalaVersions}
         |      # Are we in the root module or the item should be apply to all modules? = ${isRootModule || i.allModules}
         |
         |      * At least, one of the conditions above is false.
         |
         |""".stripMargin

    if (nonRunnableItems.nonEmpty) {

      val discardedItems =
        s"""
           |${nonRunnableItems.map(debugDiscardedItem).mkString("\n")}
           |
           |""".stripMargin

      st.log.info(s"[$commandName] Skipping the next runnable items: $discardedItems")

    } else
      st.log.info(s"[$commandName] None command will be skipped, all of them are going to be executed")

    if (filteredRunnableItemList.nonEmpty) {

      st.log.info(s"""
                     |
                     |[$commandName] Items that will be executed:
                     |${toStringListRunnableItems(filteredRunnableItemList)}
                     |""".stripMargin)

    } else
      st.log.info(s"[$commandName] No runnable items to execute")
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
