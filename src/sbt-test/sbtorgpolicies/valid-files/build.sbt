scalaVersion := "2.11.8"

import sbtorgpolicies.model._

orgMaintainersSettings := List(
  Dev("47deg", Some("47 Degrees"))
)

orgContributorsSettings := List(
  Dev("developer1", Some("Developer 1 Name")),
  Dev("developer2", Some("Developer 2 Name"))
)