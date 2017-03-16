import sbtorgpolicies._

scalaVersion := "2.11.8"

orgGithubSettings := GitHubSettings(
  organization = "myorg",
  project = "custom-template-config",
  organizationName = "My Organization",
  organizationHomePage = url("http://myorg.com"),
  organizationEmail = "hello@myorg.com",
  license = apache
)

orgTemplatesDirectory := (resourceDirectory in Compile).value / "myorg"

def getLines(fileName: String) =
  IO.readLines(file(fileName))

lazy val check = TaskKey[Unit]("check")

check := {
  val content = getLines("LICENSE").mkString

  if (!content.startsWith("Custom License"))
    sys.error("user template is not being copied successfully")

  if (!content.contains("My Organization"))
    sys.error("custom user setting didn't work")
}
