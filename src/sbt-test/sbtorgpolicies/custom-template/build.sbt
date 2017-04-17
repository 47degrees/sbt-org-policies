version := "1.0"

scalaVersion := "2.11.8"

def getLines(fileName: String) =
  IO.readLines(file(fileName))

lazy val check = TaskKey[Unit]("check")

check := {
  val content = getLines("LICENSE").mkString

  if (!content.startsWith("Custom License"))
    sys.error("user template is not being copied successfully")
}
