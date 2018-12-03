import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val root = (project in file("."))
  .aggregate(`org-policies-core-compile`, `org-policies-core-shaded`, `org-policies-core-publish`,
              `sbt-org-policies-compile`,  `sbt-org-policies-shaded`,  `sbt-org-policies-publish`)
  .settings(noPublishSettings: _*)

lazy val `org-policies-core-compile` = (project in file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "org-policies-core-compile")
  .settings(coreSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "sbtorgpolicies",
  )

lazy val `org-policies-core-shaded` = (project in file("shading/core-shaded"))
  .enablePlugins(AssemblyPlugin)
  .settings(moduleName := "org-policies-core-shaded")
  .settings(jawnShadingSettings: _*)
  .settings(
    update := (update dependsOn (`org-policies-core-compile` / Compile / packageBin)).value,
    Compile / unmanagedJars +=  (`org-policies-core-compile` / Compile / packageBin / artifactPath).value,
  )

lazy val `org-policies-core-publish` = (project in file("shading/core-publish"))
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)
  .settings(
    update := (update dependsOn (`org-policies-core-shaded` / Compile / assembly)).value,
    Compile / packageBin :=     (`org-policies-core-shaded` / Compile / assembly ).value,
  )

lazy val `sbt-org-policies-compile` = (project in file("plugin"))
  .enablePlugins(SbtPlugin)
  .dependsOn(`org-policies-core-publish`)
  .settings(moduleName := "sbt-org-policies-compile")
  .settings(pluginSettings: _*)
  .settings(noPublishSettings: _*)

lazy val `sbt-org-policies-shaded` = (project in file("shading/plugin-shaded"))
  .enablePlugins(AssemblyPlugin)
  .settings(moduleName := "sbt-org-policies-shaded")
  .settings(jawnShadingSettings: _*)
  .settings(
    update := (update dependsOn (`sbt-org-policies-compile` / Compile / packageBin)).value,
    Compile / unmanagedJars +=  (`sbt-org-policies-compile` / Compile / packageBin / artifactPath).value,
  )

lazy val `sbt-org-policies-publish` = (project in file("shading/plugin-publish"))
  .enablePlugins(SbtPlugin)
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)
  .settings(
    update := (update dependsOn (`sbt-org-policies-shaded` / Compile / assembly)).value,
    Compile / packageBin :=     (`sbt-org-policies-shaded` / Compile / assembly ).value,
  )

pgpPassphrase := Some(Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

Global / cancelable := true
