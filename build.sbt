import sbt.Keys._
import Resolver.sonatypeRepo

lazy val pluginSettings = Seq(
  sbtPlugin := true,
  resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
  addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.3.3"),
  addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.3"),
  addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "1.1"),
  addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.0.1"),
  addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.5.4"),
  addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.1.0"),
  addSbtPlugin("org.tpolecat"       % "tut-plugin"             % "0.4.8"),
  addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.20"),
  addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "0.8.0"),
  addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.0"),
  addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                % "0.8.5"),
  addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.14"),
  addSbtPlugin("com.fortysevendeg"  % "sbt-microsites"         % "0.4.0"),
  addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "1.6.0"),
  addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"          % "0.6.1"),
  addSbtPlugin("com.geirsson"       % "sbt-scalafmt"           % "0.6.3"),
  addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.0.2"),
  libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
)

lazy val `sbt-org-policies` = (project in file("."))
  .settings(moduleName := "sbt-org-policies")
  .settings(pluginSettings: _*)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(DependenciesPlugin)
