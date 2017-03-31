import com.timushev.sbt.updates.UpdatesPlugin.autoImport.dependencyUpdatesExclusions
import com.typesafe.sbt.SbtPgp.autoImportImpl.PgpKeys.gpgCommand
import com.typesafe.sbt.SbtPgp.autoImportImpl._
import dependencies.DependenciesPlugin
import dependencies.DependenciesPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderKey._
import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.license.Apache2_0
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin._
import sbt._
import sbtorgpolicies.utils._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = plugins.JvmPlugin && HeaderPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val pluginSettings: Seq[Def.Setting[_]] = Seq(
      sbtPlugin := true,
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.4.0"),
      addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.4"),
      addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "1.1"),
      addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.0.1"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.0"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.2.0"),
      addSbtPlugin("org.tpolecat"       % "tut-plugin"             % "0.4.8"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.24"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "0.8.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.0"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                % "0.8.5"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.15"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "1.8.0"),
      addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"          % "0.6.1"),
      addSbtPlugin("com.geirsson"       % "sbt-scalafmt"           % "0.6.6"),
      addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.1.0"),
      addSbtPlugin("com.47deg"          % "sbt-microsites"         % "0.5.1")
    ) ++
      ScriptedPlugin.scriptedSettings ++ Seq(
      scriptedDependencies := (compile in Test) map { _ =>
        Unit
      },
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dplugin.version=" + version.value,
            "-Dscala.version=" + scalaVersion.value
          )
      }
    )

    lazy val coreSettings = Seq(
      libraryDependencies ++= Seq(
        "com.47deg"      %% "github4s"       % "0.13.0",
        "org.typelevel"  %% "cats"           % "0.9.0",
        "joda-time"      % "joda-time"       % "2.9.9",
        "org.scala-sbt"  % "scripted-plugin" % sbtVersion.value,
        "org.scalatest"  %% "scalatest"      % "3.0.1" % "test",
        "org.scalacheck" %% "scalacheck"     % "1.13.5" % "test",
        "org.mockito"    % "mockito-all"     % "2.0.2-beta" % "test"
      )
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] =
    artifactSettings ++
      releaseProcessSettings ++
      pgpSettings ++
      credentialSettings ++
      publishSettings ++
      miscSettings ++
      sbtDependenciesSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    scalaOrganization := "org.scala-lang",
    organization := "com.47deg",
    organizationName := "47 Degrees",
    homepage := Option(url("http://www.47deg.com")),
    startYear := Some(2017),
    organizationHomepage := Some(new URL("http://47deg.com")),
    headers := Map(
      "scala" -> Apache2_0("2017", "47 Degrees, LLC. <http://www.47deg.com>")
    )
  )

  private[this] val gitUrl = "https://github.com/47deg/sbt-org-policies"

  private[this] val publishSettings = Seq(
    licenses += ("Apache License", url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo := Some(ScmInfo(url(gitUrl), s"scm:git:$gitUrl.git")),
    apiURL := Some(url(gitUrl)),
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := Function.const(false),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("Snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    },
    autoAPIMappings := true,
    pomExtra := <developers>
      <developer>
        <name>47 Degrees (twitter: @47deg)</name>
        <email>hello@47deg.com</email>
      </developer>
      <developer>
        <name>47 Degrees</name>
      </developer>
    </developers>
  )

  private[this] val gpgFolder = getEnvVar("PGP_FOLDER").getOrElse(".")

  private[this] val pgpSettings = Seq(
    pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray),
    gpgCommand := gpgFolder,
    pgpPublicRing := file(s"$gpgFolder/pubring.gpg"),
    pgpSecretRing := file(s"$gpgFolder/secring.gpg")
  )

  private[this] val credentialSettings = Seq(
    credentials ++= (for {
      username <- getEnvVar("SONATYPE_USERNAME")
      password <- getEnvVar("SONATYPE_PASSWORD")
    } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
  )

  private[this] val releaseProcessSettings = Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges
    )
  )

  private[this] val miscSettings = Seq(
    shellPrompt := { s: State =>
      val c     = scala.Console
      val blue  = c.RESET + c.BLUE + c.BOLD
      val white = c.RESET + c.BOLD

      val projectName = Project.extract(s).currentProject.id

      s"$blue$projectName$white>${c.RESET}"
    }
  )

  private[this] val sbtDependenciesSettings = DependenciesPlugin.defaultSettings ++ Seq(
    dependencyUpdatesExclusions :=
      moduleFilter(organization = "org.scala-lang") |
        moduleFilter(organization = "org.scala-sbt"),
    depGithubOwnerSetting := "47deg",
    depGithubRepoSetting := name.value,
    depGithubTokenSetting := getEnvVar("GITHUB_TOKEN_REPO")
  )
}
