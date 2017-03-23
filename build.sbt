import sbt.Keys._
import Resolver.sonatypeRepo

lazy val pluginSettings = Seq(
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
  addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.14"),
  addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "1.8.0"),
  addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"          % "0.6.1"),
  addSbtPlugin("com.geirsson"       % "sbt-scalafmt"           % "0.6.6"),
  addSbtPlugin("com.47deg"          % "sbt-microsites"         % "0.5.0-SNAPSHOT"),
  addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.0.5-SNAPSHOT"),
  libraryDependencies ++= Seq(
    "com.47deg"      %% "github4s"       % "0.12.0",
    "org.typelevel"  %% "cats"           % "0.9.0",
    "org.scala-sbt"  % "scripted-plugin" % sbtVersion.value,
    "org.scalatest"  %% "scalatest"      % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck"     % "1.13.5" % "test",
    "org.mockito"    % "mockito-all"     % "2.0.2-beta" % "test"
  )
)

lazy val `sbt-org-policies` = (project in file("."))
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(DependenciesPlugin)
