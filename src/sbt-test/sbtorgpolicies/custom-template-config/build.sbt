import sbtorgpolicies.model.{ApacheLicense, GitHubSettings}

version := "1.0"

scalaVersion := sbtorgpolicies.model.scalac.`2.11`

orgGithubSetting := GitHubSettings(
  organization = "myorg",
  project = "custom-template-config",
  organizationName = "My Organization",
  groupId = "com.myorg",
  organizationHomePage = url("http://myorg.com"),
  organizationEmail = "hello@myorg.com"
)

orgLicenseSetting := ApacheLicense.custom

startYear := Some(2012)

orgTemplatesDirectorySetting := (resourceDirectory in Compile).value / "myorg"

def getLines(fileName: String) =
  IO.readLines(file(fileName))

lazy val check = TaskKey[Unit]("check")

lazy val checkApache = TaskKey[Unit]("checkApache")

lazy val checkMIT = TaskKey[Unit]("checkMIT")

check := {
  val content = getLines("LICENSE").mkString

  if (!content.startsWith("Custom License"))
    sys.error("user template is not being copied successfully")

  if (!content.contains("My Organization"))
    sys.error("custom user setting didn't work")

  if (!content.contains(s"2012-"))
    sys.error("custom user setting didn't work")
}

checkApache := {
  val content = getLines("LICENSE").mkString

  if (!content.contains("Licensed under the Apache License, Version 2.0"))
    sys.error("Apache template is not being copied successfully")

  if (!content.contains("My Organization"))
    sys.error("custom user setting didn't work")

  if (!content.contains(s"2012-"))
    sys.error("custom user setting didn't work")
}

checkMIT := {
  val content = getLines("LICENSE").mkString

  if (!content.contains("The MIT License (MIT)"))
    sys.error("MIT template is not being copied successfully")

  if (!content.contains("My Organization"))
    sys.error("custom user setting didn't work")

  if (!content.contains(s"2012-"))
    sys.error("custom user setting didn't work")
}
