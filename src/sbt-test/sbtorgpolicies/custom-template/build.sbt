version := "1.0"

scalaVersion := sbtorgpolicies.model.scalac.`2.11`

def getLines(fileName: String) =
  IO.readLines(file(fileName))

lazy val check = TaskKey[Unit]("check")

check := {
  val content = getLines("LICENSE").mkString

  if (!content.startsWith("Custom License"))
    sys.error("user template is not being copied successfully")
}
