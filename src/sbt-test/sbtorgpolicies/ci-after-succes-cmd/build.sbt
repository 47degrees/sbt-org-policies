name := "sbt-org-policies"

scalaVersion := "2.12.1"

orgGithubTokenSetting := Option(System.getenv().get("GITHUB_TOKEN_REPO"))

orgAfterCISuccessCheckSetting := true

lazy val testCheck = TaskKey[Unit]("testCheck")

testCheck := Def.task {
  orgContributorsSetting.value match {
    case Nil => sys.error("Test failed.")
    case _   => streams.value.log.info(s"Test succeeded.")
  }
}.value

orgAfterCISuccessTaskListSetting := List(orgCreateContributorsFile, orgCreateFiles)
