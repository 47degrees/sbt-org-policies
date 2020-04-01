import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys.gpgCommand

lazy val `sbt-org-policies` = (project in file("."))
  .enablePlugins(SbtPlugin)
  .dependsOn(`org-policies-core`)
  .aggregate(`org-policies-core`)
  .settings(name := "sbt-org-policies")
  .settings(pluginSettings: _*)

lazy val `org-policies-core` = (project in file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)
  .settings(
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "sbtorgpolicies"
  )

pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)
Global / gpgCommand := "gpg"

ThisBuild / parallelExecution := false
Global / cancelable := true

addCommandAlias("ci-test", "scalafmtCheck; scalafmtSbtCheck; test; publishLocal; scripted")
addCommandAlias("ci-docs", "compile")
