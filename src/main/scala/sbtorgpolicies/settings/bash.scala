package sbtorgpolicies.settings

import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.OrgPoliciesKeys._
import sbtorgpolicies.templates.contributorsFilePath
import sbtorgpolicies.utils._

trait bash {

  val orgBashTasks =
    Seq(
      orgCommitPolicyFiles := Def.task {
        val ghOps: GitHubOps = orgGithubOpsSetting.value
        ghOps.commitFiles(
          baseDir = baseDirectory.value,
          branch = orgCommitBranchSetting.value,
          message = s"${orgCommitMessageSetting.value} [ci skip]",
          files = orgEnforcedFilesSetting.value.map(_.outputPath) :+ contributorsFilePath
        ) match {
          case Right(_) => streams.value.log.info("Policy files committed successfully")
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
      }.value,
      orgAfterCISuccess := Def.taskDyn {
        val scalaV       = scalaVersion.value
        val crossV       = crossScalaVersions.value
        val isLastScalaV = crossV.lastOption.exists(_ == scalaV)
        if (isLastScalaV &&
          getEnvVarOrElse("TRAVIS_BRANCH") == "master" &&
          getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false") {
          Def.task {
            orgCreateContributorsFile.value
            orgCreateFiles.value
            orgCommitPolicyFiles.value
            orgPublishRelease.value
          }
        } else Def.task()
      }.value
    )

}
