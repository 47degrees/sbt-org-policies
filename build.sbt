import sbt.Keys._
import Resolver.sonatypeRepo

lazy val `sbt-org-policies` = (project in file("."))
  .dependsOn(`org-policies-core`)
  .aggregate(`org-policies-core`)
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)

lazy val `org-policies-core` = (project in file("core"))
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)
