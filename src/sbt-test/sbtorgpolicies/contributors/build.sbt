name := "sbt-org-policies"

orgGithubTokenSetting := Option(System.getenv().get("GITHUB_TOKEN_REPO"))

lazy val testCheck = TaskKey[Unit]("testCheck")

testCheck := Def.task {
  orgContributorsSetting.value match {
    case Nil => sys.error("Test failed.")
    case _   => streams.value.log.info(s"Test succeeded.")
  }
}.value
