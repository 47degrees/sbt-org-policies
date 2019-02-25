import dependencies.DependenciesPlugin.autoImport._
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model.scalac
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val base64: String            = "0.2.4"
      val cats: String              = "1.6.0"
      val github4s: String          = "0.20.1"
      val moultingyaml: String      = "0.4.0"
      val scala: String             = "2.12.8"
      val scalacheck: String        = "1.13.5"
      val scalacheckToolbox: String = "0.2.4"
      val scalamock: String         = "3.6.0"
      val scalatest: String         = "3.0.5"
      val scalaxml: String          = "1.1.1"
    }

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      name := "sbt-org-policies",
      organization := "com.47deg",
      organizationName := "47 Degrees",
      organizationHomepage := Some(url("http://47deg.com"))
    )

    lazy val pluginSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin(%%("sbt-git", true)),
      addSbtPlugin(%%("sbt-unidoc", true)),
      addSbtPlugin(%%("sbt-release", true)),
      addSbtPlugin(%%("sbt-sonatype", true)),
      addSbtPlugin(%%("sbt-pgp", true)),
      addSbtPlugin(%%("sbt-jmh", true)),
      addSbtPlugin(%%("sbt-scoverage", true)),
      addSbtPlugin(%%("sbt-scalajs", true)),
      addSbtPlugin(%%("sbt-header", "3.0.1", true)),
      addSbtPlugin(%%("tut-plugin", true)),
      addSbtPlugin(%%("sbt-scalafmt", "1.5.1", true)),
      libraryDependencies ++= Seq(
        "com.geirsson" %% "scalafmt-cli" % "1.5.1",
      ),
      scriptedLaunchOpts := {
        scriptedLaunchOpts.value ++
          Seq(
            "-Xmx2048M",
            "-XX:ReservedCodeCacheSize=256m",
            "-XX:+UseConcMarkSweepGC",
            "-Dplugin.version=" + version.value,
            "-Dscala.version=" + scalaVersion.value
          )
      },
      scriptedBufferLog := false,
    )

    lazy val coreSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      scalaVersion := scalac.`2.12`,
      crossScalaVersions := Seq(scalac.`2.12`),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % V.scalaxml,
        %%("github4s", V.github4s),
        %%("cats-core", V.cats),
        %%("base64", V.base64),
        %%("moultingyaml", V.moultingyaml),
        %%("cats-laws", V.cats)                          % Test,
        %%("scalacheck", V.scalacheck)                   % Test,
        %%("scalatest", V.scalatest)                     % Test,
        %%("scheckToolboxDatetime", V.scalacheckToolbox) % Test,
        %%("scalamockScalatest", V.scalamock)            % Test
      )
    )
  }

  import autoImport.V
  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := V.scala,
    crossScalaVersions := Seq(V.scala),
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017),
    orgBadgeListSetting := List(
      TravisBadge.apply,
      MavenCentralBadge.apply,
      LicenseBadge.apply,
      GitHubIssuesBadge.apply
    ),
    orgAfterCISuccessTaskListSetting ~= (_ filterNot (_ == depUpdateDependencyIssues.asRunnableItem))
  )
}
