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
    buildInfoPackage := "sbtorgpolicies",
  )

pgpPassphrase := Some(Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
Global / gpgCommand := "gpg"
usePgpKeyHex("00DD4E68B6B8ACA56BA79E1199E76A8A1D1E27FF")

ThisBuild / parallelExecution := false
Global / cancelable := true
