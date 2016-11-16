import sbt.Keys._
import de.heikoseeberger.sbtheader.license.Apache2_0
import Resolver.sonatypeRepo
import PgpKeys.gpgCommand

val dev = Seq(Dev("47 Degrees (twitter: @47deg)", "47 Degrees"))
val gh  = GitHubSettings("com.fortysevendeg", "sbt-catalysts-extras", "47 Degrees", apache)

lazy val artifactSettings = Seq(
  name := gh.proj,
  organization := gh.org,
  organizationName := "47 Degrees",
  homepage := Option(url("http://www.47deg.com")),
  organizationHomepage := Some(new URL("http://47deg.com")),
  headers := Map(
    "scala" -> Apache2_0("2016", "47 Degrees, LLC. <http://www.47deg.com>")
  )
)

lazy val pluginSettings = Seq(
  sbtPlugin := true,
  resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
  addSbtPlugin("com.eed3si9n"        % "sbt-unidoc"             % "0.3.3"),
  addSbtPlugin("com.github.gseitz"   % "sbt-release"            % "1.0.3"),
  addSbtPlugin("com.github.tkawachi" % "sbt-doctest"            % "0.4.1"),
  addSbtPlugin("org.xerial.sbt"      % "sbt-sonatype"           % "1.1"),
  addSbtPlugin("com.jsuereth"        % "sbt-pgp"                % "1.0.1"),
  addSbtPlugin("com.typesafe.sbt"    % "sbt-ghpages"            % "0.5.4"),
  addSbtPlugin("com.typesafe.sbt"    % "sbt-site"               % "1.1.0"),
  addSbtPlugin("org.tpolecat"        % "tut-plugin"             % "0.4.6"),
  addSbtPlugin("pl.project13.scala"  % "sbt-jmh"                % "0.2.16"),
  addSbtPlugin("org.scalastyle"      %% "scalastyle-sbt-plugin" % "0.8.0"),
  addSbtPlugin("org.scoverage"       % "sbt-scoverage"          % "1.5.0"),
  addSbtPlugin("com.typesafe.sbt"    % "sbt-git"                % "0.8.5"),
  addSbtPlugin("org.scala-js"        % "sbt-scalajs"            % "0.6.13"),
  addSbtPlugin("com.fortysevendeg"   % "sbt-microsites"         % "0.3.2"),
  addSbtPlugin("de.heikoseeberger"   % "sbt-header"             % "1.6.0"),
  addSbtPlugin("com.eed3si9n"        % "sbt-buildinfo"          % "0.6.1"),
  libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
)

lazy val gpgFolder = sys.env.getOrElse("PGP_FOLDER", ".")

lazy val pgpSettings = Seq(
  pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray),
  gpgCommand := gpgFolder,
  pgpPublicRing := file(s"$gpgFolder/pubring.gpg"),
  pgpSecretRing := file(s"$gpgFolder/secring.gpg")
)

lazy val miscSettings = Seq(
  shellPrompt := { s: State =>
    val c     = scala.Console
    val blue  = c.RESET + c.BLUE + c.BOLD
    val white = c.RESET + c.BOLD

    val projectName = Project.extract(s).currentProject.id

    s"$blue$projectName$white>${c.RESET}"
  }
)

lazy val allSettings = artifactSettings ++
    pluginSettings ++
    miscSettings ++
    sharedReleaseProcess ++
    pgpSettings ++
    credentialSettings ++
    sharedPublishSettings(gh, dev)

lazy val `sbt-catalysts-extras` = (project in file("."))
  .settings(moduleName := "sbt-catalysts-extras")
  .settings(allSettings: _*)
  .enablePlugins(AutomateHeaderPlugin)
