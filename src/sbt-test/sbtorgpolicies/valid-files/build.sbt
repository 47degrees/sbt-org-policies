scalaVersion := "2.11.8"

name := "sbt-org-policies-test"

import sbtorgpolicies.model._

orgMaintainersSetting := List(
  Dev("47deg", Some("47 Degrees"))
)

orgContributorsSetting := List(
  Dev("developer1", Some("Developer 1 Name")),
  Dev("developer2", Some("Developer 2 Name"))
)
