import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

lazy val `sbt-org-policies` = (project in file("."))
  .dependsOn(`shaded-jawn-parser`, `org-policies-core`)
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
    Compile / unmanagedJars ++= Seq(
      `shaded-jawn-parser`.base / "target" / s"scala-${scalaBinaryVersion.value}" /
        s"shaded-jawn-parser-${scalaBinaryVersion.value}-${version.value}-assembly.jar"
    ),
    update := (update dependsOn (`shaded-jawn-parser` / assembly)).value,
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    assemblyJarName in assembly := s"${name.value}-${version.value}-assembly.jar",
    addArtifact(artifact in (Compile, assembly), assembly)
  )

lazy val `shaded-jawn-parser` = (project in file("shaded-jawn-parser"))
  .settings(
    name := "shaded-jawn-parser",
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.13.0",
    assembly / test := {},
    assembly / assemblyOption ~= { _.copy(includeScala = false) },
    assembly / assemblyJarName := s"${name.value}-${scalaBinaryVersion.value}-${version.value}-assembly.jar",
    assembly / assemblyShadeRules := Seq(ShadeRule.rename("jawn.**" -> "org_policies_jawn.@1").inAll),
    addArtifact(Artifact("shaded-jawn-parser", "assembly"), sbtassembly.AssemblyKeys.assembly),
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )

pgpPassphrase := Some(Option(System.getenv().get("PGP_PASSPHRASE")).getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")
