package sbtorgpolicies.settings

import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt._
import sbt.complete.DefaultParsers.OptNotSpace
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.model.Dev
import sbtorgpolicies.templates.contributorsFilePath
import sbtrelease.ReleasePlugin.autoImport.releaseTagComment
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
          files = orgEnforcedFilesSetting.value.map(_.outputPath) :+ contributorsFilePath
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
            "sbt release".!
            Def.task()
          case _ =>
            streams.value.log.info(s"Release Version detected but it'll be skipped for Scala $scalaV...")
            Def.task()
        }
      }.value
    )

  val orgAfterCISuccessCommand: Command = Command(afterCISuccessCommandKey)(_ => OptNotSpace) { (st, _) =>
    val extracted = Project.extract(st)

    val scalaV            = extracted.get(scalaVersion)
    val crossV            = extracted.get(crossScalaVersions)
    val baseDir           = extracted.get(baseDirectory)
    val maybeToken        = extracted.get(orgGithubTokenSetting)
    val rootDir           = extracted.get(baseDirectory in LocalRootProject)
    val afterSuccessCheck = extracted.get(orgAfterCISuccessCheckSetting)

    val isLastScalaV = crossV.lastOption.exists(_ == scalaV)

    if (isLastScalaV &&
      baseDir.getAbsolutePath == rootDir.getAbsolutePath &&
      afterSuccessCheck) {

      st.log.info(
        "Initiating orgAfterCISuccessCommand set of tasks " +
          "configured at orgAfterCISuccessTaskListSetting setting")

      println(s"Token = $maybeToken")

      val (fetchContributorsState, contributorList) =
        if (maybeToken.nonEmpty) {
          extracted.runTask[List[Dev]](orgFetchContributors, st)
        } else (st, Nil)

      println(s"contributorList = $contributorList")

      val newState = reapply(Seq[Setting[_]](orgContributorsSetting := contributorList), fetchContributorsState)

      val taskList = extracted.get(orgAfterCISuccessTaskListSetting)

      taskList map (Project.extract(newState).runTask(_, newState))
      newState
    } else {
      st.log.info("Skipping after CI Success tasks")
      st
    }
  }
}
