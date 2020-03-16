import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.model.scalac
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates.badges._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import sbtorgpolicies.model.GitHubSettings

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val V = new {
      val base64: String            = "0.2.9"
      val cats: String              = "2.1.1"
      val github4s: String          = "0.23.0"
      val moultingyaml: String      = "0.4.1"
      val scala212: String          = "2.12.10"
      val scala213: String          = "2.13.1"
      val scalacheck: String        = "1.14.3"
      val scalacheckToolbox: String = "0.3.3"
      val scalamock: String         = "4.4.0"
      val scalatest: String         = "3.1.1"
      val scalatestPlus: String     = "3.1.1.1"
      val scalaxml: String          = "1.2.0"
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
      addSbtPlugin(%%("sbt-header", "3.0.2", true)),
      addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.0"),
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
      // TODO this custom release process can be removed when the cyclic dependency is upgraded to 0.12.3 or newer
      releaseProcess := Seq[ReleaseStep](
        orgInitialVcsChecks,
        checkSnapshotDependencies,
        orgInquireVersions,
        if (sbtPlugin.value) releaseStepCommandAndRemaining("^ clean") else runClean,
        if (sbtPlugin.value) releaseStepCommandAndRemaining("^ test") else runTest,
        orgTagRelease,
        orgUpdateChangeLog,
        if (sbtPlugin.value) releaseStepCommandAndRemaining("^ publishSigned") else publishArtifacts,
        releaseStepCommandAndRemaining("sonatypeBundleRelease"),
        setNextVersion,
        orgCommitNextVersion,
        orgPostRelease
      )
    )

    lazy val coreSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      scalaVersion := V.scala212,
      crossScalaVersions := Seq(V.scala212),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % V.scalaxml,
        %%("github4s", V.github4s),
        %%("cats-core", V.cats),
        %%("base64", V.base64),
        %%("moultingyaml", V.moultingyaml),
        %%("cats-laws", V.cats)                          % Test,
        %%("scalacheck", V.scalacheck)                   % Test,
        %%("scalatest", V.scalatest)                     % Test,
        %%("scalatestPlus", V.scalatestPlus)             % Test,
        %%("scheckToolboxDatetime", V.scalacheckToolbox) % Test,
        %%("scalamock", V.scalamock)                     % Test
      )
    )
  }

  import autoImport.V
  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := V.scala212,
    crossScalaVersions := Seq(V.scala212),
    orgGithubSetting := GitHubSettings(
      organization = "47degrees",
      project = (name in LocalRootProject).value,
      organizationName = "47 Degrees",
      groupId = "com.47deg",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017),
    orgBadgeListSetting := List(
      TravisBadge.apply,
      MavenCentralBadge.apply,
      LicenseBadge.apply,
      GitHubIssuesBadge.apply
    )
  )
}
