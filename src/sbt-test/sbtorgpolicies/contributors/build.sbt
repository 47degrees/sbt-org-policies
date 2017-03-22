name := "sbt-org-policies"

githubToken := Option(System.getenv().get("GITHUB_TOKEN_REPO")).getOrElse("")

lazy val testCheck = TaskKey[Unit]("testCheck")

testCheck := Def.task {
  orgContributorsSettings.value match {
    case Nil => sys.error("Test failed.")
    case _ => streams.value.log.info(s"Test succeeded.")
  }
}.value
