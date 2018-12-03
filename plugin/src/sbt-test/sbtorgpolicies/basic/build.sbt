version := "1.0"

scalaVersion := sbtorgpolicies.model.scalac.`2.11`

lazy val testCheckSettings = TaskKey[Unit]("testCheckSettings")

testCheckSettings := Def.task {
  orgCheckSettings.result.value match {
    case Inc(inc: Incomplete) =>
      streams.value.log.info(
        s"Test succeeded. checkSettings failed with error message: ${inc.directCause.map(_.getMessage).getOrElse("")}.")
    case Value(_) => sys.error("Test failed: orgCheckSettings executed successfully.")
  }
}.value
