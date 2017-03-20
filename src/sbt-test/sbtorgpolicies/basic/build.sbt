scalaVersion := "2.11.8"

lazy val testCheckSettings = TaskKey[Unit]("testCheckSettings")

testCheckSettings := Def.task {
  checkSettings.result.value match {
    case Inc(inc: Incomplete) => streams.value.log.info(s"Test succeeded. checkSettings failed with error message: ${inc.directCause.map(_.getMessage).getOrElse("")}.")
    case Value(_) => sys.error("Test failed: checkSettings executed successfully.")
  }
}.value
