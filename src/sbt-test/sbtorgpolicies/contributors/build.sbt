name := "sbt-org-policies"

lazy val testCheck = TaskKey[Unit]("testCheck")

testCheck := Def.task {
  orgContributorsSettings.value match {
    case Nil => sys.error("Test failed.")
    case _ => streams.value.log.info(s"Test succeeded.")
  }
}.value
