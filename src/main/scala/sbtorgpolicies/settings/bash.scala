package sbtorgpolicies.settings

import cats.syntax.either._
import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.io.{FileReader, IOResult}
import sbtorgpolicies.templates.{FileType, contributorsFilePath}
import sbtorgpolicies.utils._

trait bashKeys {

  val orgCommitBranchSetting: SettingKey[String] =
    settingKey[String]("Defines the target branch for committing the policy files")

  val orgCommitMessageSetting: SettingKey[String] =
    settingKey[String]("Defines the commit message when publishing files into GitHub")

  val orgCommitPolicyFiles: TaskKey[Unit] = taskKey[Unit]("Commit the policy files into the specified branch")

  val orgAfterCISuccess: TaskKey[Unit] = taskKey[Unit]("It will execute some tasks after a CI build.")

}

trait bash extends bashKeys with filesKeys with keys {

  val fileReader: FileReader = new FileReader

  val orgBashDefaultSettings = Seq(
    orgCommitBranchSetting := "master",
    orgCommitMessageSetting := "Updates policy files from SBT"
  )

  private[this] def readFileContents(list: List[FileType]): IOResult[List[(String, String)]] = {

    val sbtContributors = fileReader.getFileContent(contributorsFilePath).map((contributorsFilePath, _))

    list.foldLeft[IOResult[List[(String, String)]]](sbtContributors.map(List(_))) {
      case (Right(partialResult), file) =>
        fileReader.getFileContent(file.outputPath).map(partialResult :+ (file.outputPath, _))
      case (Left(e), _) => Left(e)
    }
  }

  val orgBashTasks =
    Seq(
      orgCommitPolicyFiles := Def.task {
        val ghOps = new GitHubOps(
          orgGithubSetting.value.organization,
          orgGithubSetting.value.project,
          orgGithubTokenSetting.value)
        (for {
          filesAndContents <- readFileContents(orgEnforcedFiles.value)
          _ <- ghOps.commitFiles(
            owner = orgGithubSetting.value.organization,
            repo = orgGithubSetting.value.project,
            branch = orgCommitBranchSetting.value,
            message = orgCommitMessageSetting.value,
            filesAndContents = filesAndContents
          )
        } yield ()) match {
          case Right(_) => streams.value.log.info("Policy files committed successfully")
          case Left(e) =>
            streams.value.log.error(s"Error committing files")
            e.printStackTrace()
        }
      }.value,
      orgAfterCISuccess := Def.taskDyn {
        if (getEnvVarOrElse("TRAVIS_BRANCH") == "master" &&
          getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false") {
          Def
            .sequential(
              orgCreateContributorsFile,
              orgCreateFiles,
              orgCommitPolicyFiles,
              publishSigned
            )
        } else Def.task()
      }.value
    )

}
