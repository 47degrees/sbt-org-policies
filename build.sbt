import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import Path._
import sbt.io.SimpleFilter

lazy val `sbt-org-policies` = (project in file("."))
  .dependsOn(`org-policies-core`)
  .aggregate(`shaded-jawn-parser`, `org-policies-core`)
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)
  .enablePlugins(ScriptedPlugin)
  .disablePlugins(sbtassembly.AssemblyPlugin)

lazy val `org-policies-core` = (project in file("core"))
  .settings(moduleName := "org-policies-core")
  .settings(coreSettings: _*)
  .enablePlugins(AssemblyPlugin)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "sbtorgpolicies",
    Compile / packageBin / mappings ++= IO.unzip(`shaded-jawn-parser`.base / "target" / s"scala-${scalaBinaryVersion.value}" /
          s"shaded-jawn-parser-${scalaBinaryVersion.value}-${version.value}-assembly.jar", 
      `shaded-jawn-parser`.base / "target" / "temp", new SimpleFilter(!_.contains("META-INF"))).toSeq pair relativeTo(`shaded-jawn-parser`.base / "target" / "temp"),
    update := (update dependsOn (`shaded-jawn-parser` / assembly)).value,
  )

// Shade and bundle jawn-parser and intermediate deps (https://github.com/47deg/sbt-org-policies/issues/1173)
lazy val `shaded-jawn-parser` = (project in file("shaded-jawn-parser"))
  .settings(
    name := "shaded-jawn-parser",
    // intransitive, so we bundle the bare minimum dependencies to shade the chain of calls from our code to jawn-parser
    libraryDependencies ++= Seq(
      %%("github4s"),
      %%("circe-parser"),
      "io.circe" %% "circe-jawn" % "0.10.0",
      "org.spire-math" %% "jawn-parser" % "0.13.0",
    ).map(_.intransitive()), 
    //assembly / logLevel := Level.Debug,
    assembly / test := {},
    assembly / assemblyOption ~= { _.copy(includeScala = false) },
    assembly / assemblyJarName := s"${name.value}-${scalaBinaryVersion.value}-${version.value}-assembly.jar",
    // must be careful not to rename any calls to unshaded packages outside our bundled dependencies (ex. io.circe.** would break)
    assembly / assemblyShadeRules := Seq(ShadeRule.rename(
      "github4s.**"        -> "org_policies_github4s.@1",
      "io.circe.parser.**" -> "org_policies_circe_parser.@1",
      "io.circe.jawn.**"   -> "org_policies_circe_jawn.@1",
      "jawn.**"            -> "org_policies_jawn_parser.@1"
    )).map(_.inAll),
    addArtifact(Artifact("shaded-jawn-parser", "assembly"), sbtassembly.AssemblyKeys.assembly),
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )

pgpPassphrase := Some(Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
