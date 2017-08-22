name := "sbt-org-policies"

version := "0.0.1"

scalaVersion := sbtorgpolicies.model.scalac.`2.12`

resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
