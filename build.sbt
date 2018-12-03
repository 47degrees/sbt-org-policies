import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val `sbt-org-policies` = (project in file("."))
  .dependsOn(`org-policies-core-publish`)
  .aggregate(`org-policies-core-compile`, `org-policies-core-shaded`, `org-policies-core-publish`)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(SbtPlugin)
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)

lazy val `org-policies-core-compile` = (project in file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(moduleName := "org-policies-core-compile")
  .settings(coreSettings: _*)
  .settings(
    buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "sbtorgpolicies",
    publishArtifact := false,
    publish := {},
    publishLocal := {},
  )

lazy val `org-policies-core-shaded` = (project in file("core-shaded"))
  .enablePlugins(AssemblyPlugin)
  .settings(moduleName := "org-policies-core-shaded")
  .settings(
    update := (update dependsOn (`org-policies-core-compile` / Compile / packageBin)).value,
    Compile / unmanagedJars +=  (`org-policies-core-compile` / Compile / packageBin / artifactPath).value,
    // Shade and bundle jawn-parser and intermediate deps (https://github.com/47deg/sbt-org-policies/issues/1173)
    libraryDependencies ++= Seq(
      %%("github4s"),
      %%("circe-parser"),
      "io.circe" %% "circe-jawn" % "0.10.0",
      "org.spire-math" %% "jawn-parser" % "0.13.0",
    ).map(_.intransitive()), // intransitive, to bundle the minimum dependencies to shade the chain of calls from our code to jawn-parser
    assembly / assemblyOption ~= (_.copy(includeScala = false)),
    // must be careful not to rename any calls to unshaded packages outside our bundled dependencies (ex. io.circe.** would break)
    assembly / assemblyShadeRules := Seq(ShadeRule.rename(
      "github4s.**"        -> "org_policies_github4s.@1",
      "io.circe.parser.**" -> "org_policies_circe_parser.@1",
      "io.circe.jawn.**"   -> "org_policies_circe_jawn.@1",
      "jawn.**"            -> "org_policies_jawn_parser.@1"
    )).map(_.inAll),
    publishArtifact := false,
    publish := {},
    publishLocal := {},
  )

lazy val `org-policies-core-publish` = (project in file("core-publish"))
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)
  .settings(
    update := (update dependsOn (`org-policies-core-shaded` / Compile / assembly)).value,
    Compile / packageBin :=     (`org-policies-core-shaded` / Compile / assembly ).value,
  )

Global / cancelable := true

pgpPassphrase := Some(Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
