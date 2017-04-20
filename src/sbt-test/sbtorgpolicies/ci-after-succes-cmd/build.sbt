import sbtorgpolicies.runnable.syntax._

name := "sbt-org-policies"

version := "1.0"

scalaVersion := "2.12.2"

orgGithubTokenSetting := "GITHUB_TOKEN_REPO"

orgAfterCISuccessCheckSetting := true

orgAfterCISuccessTaskListSetting := List(orgCreateFiles.asRunnableItem)
