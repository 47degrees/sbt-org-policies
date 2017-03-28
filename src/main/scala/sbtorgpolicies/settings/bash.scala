package sbtorgpolicies.settings

import cats.syntax.either._
import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbt._
import sbtorgpolicies.github.GitHubOps
import sbtorgpolicies.io.{FileReader, IOResult}
import sbtorgpolicies.model.{Dev, GitHubSettings}
import sbtorgpolicies.templates.FileType
import sbtorgpolicies.utils._

trait bashKeys {

  val orgCommitBranchSetting: SettingKey[String] =
    settingKey[String]("Defines the target branch for committing policy files")

  val orgCommitPolicyFiles: TaskKey[Unit] = taskKey[Unit]("Commit into defined branch the policy files")

  val orgAfterTravisSuccess: TaskKey[Unit] = taskKey[Unit](
    "Task intended to execute sequentially tasks after a Travis build. It's only executed on the master branch")

}

trait bash extends bashKeys with filesKeys {

  val fileReader: FileReader = new FileReader

  val orgBashDefaultSettings = Seq(
    orgCommitBranchSetting := "master"
  )

  private[this] def readFileContents(list: List[FileType]): IOResult[List[(String, String)]] =
    list.foldLeft[IOResult[List[(String, String)]]](Right(Nil)) {
      case (Right(partialResult), file) =>
        fileReader.getFileContent(file.outputPath).map(partialResult :+ (file.outputPath, _))
      case (Left(e), _) => Left(e)
    }

  def orgBashTasks(
      gh: SettingKey[GitHubSettings],
      maintainers: SettingKey[List[Dev]],
      githubToken: SettingKey[Option[String]]) =
    Seq(
      orgCommitPolicyFiles := Def.task {
        val ghOps = new GitHubOps(gh.value.organization, gh.value.project, githubToken.value)
        (for {
          filesAndContents <- readFileContents(orgEnforcedFiles.value)
          _                <- ghOps.commitFiles(filesAndContents)
        } yield ()) match {
          case Right(_) => streams.value.log.info("Policy files committed successfully")
          case Left(e) =>
            streams.value.log.error(s"Error committing files")
            e.printStackTrace()
        }
      }.value,
      orgAfterTravisSuccess := Def.task {
        if (getEnvVarOrElse("TRAVIS_BRANCH") == "master" && getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false") {
          Def
            .sequential(
              orgCreateContributorsFile,
              orgCreateFiles,
              orgCommitPolicyFiles,
              publishSigned
            )
            .value
        }
      }.value
    )

}
