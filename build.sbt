import sbt.Keys._
import de.heikoseeberger.sbtheader.license.Apache2_0
import Resolver.sonatypeRepo

lazy val artifactSettings = Seq(
  name := "sbt-catalysts-ext",
  organization := "com.fortysevendeg",
  organizationName := "47 Degrees",
  homepage := Option(url("http://www.47deg.com")),
  organizationHomepage := Some(new URL("http://47deg.com")),
  headers := Map(
    "scala" -> Apache2_0("2016", "47 Degrees, LLC. <http://www.47deg.com>")
  )
)

lazy val pluginSettings = Seq(
  sbtPlugin := true,
  scalaVersion in ThisBuild := "2.10.6",
  resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
  // Extension import:
  addSbtPlugin("org.typelevel" % "sbt-catalysts" % "0.1.12"),
  // Additional Plugins:
  addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "1.6.0"),
  addSbtPlugin("com.eed3si9n"      % "sbt-buildinfo" % "0.6.1")
)

lazy val miscSettings = Seq(
  shellPrompt := { s: State =>
    val c     = scala.Console
    val blue  = c.RESET + c.BLUE + c.BOLD
    val white = c.RESET + c.BOLD

    val projectName = Project.extract(s).currentProject.id

    s"$blue$projectName$white>${c.RESET}"
  },
  scalafmtConfig in ThisBuild := Some(file(".scalafmt"))
)

lazy val allSettings = artifactSettings ++ pluginSettings ++ miscSettings ++ reformatOnCompileSettings

lazy val `sbt-catalysts-ext` = (project in file("."))
  .settings(moduleName := "sbt-catalysts-ext")
  .settings(allSettings: _*)
  .enablePlugins(AutomateHeaderPlugin)
