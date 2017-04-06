import sbt.Keys._

lazy val `sbt-org-policies` = (project in file("."))
  .dependsOn(`org-policies-core`)
  .aggregate(`org-policies-core`)
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)

lazy val `org-policies-core` = (project in file("core"))
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)

lazy val `org-policies-auto-dep-check` = (project in file("autocheck"))
  .settings(moduleName := "org-policies-auto-dep-check")
  .settings(autoCheckDepSettings: _*)

addCommandAlias(
  "publishLocalAll",
  ";org-policies-core/publishLocal;org-policies-auto-dep-check/publishLocal;sbt-org-policies/publishLocal")

pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.asc")
pgpSecretRing := file(s"$gpgFolder/secring.asc")
