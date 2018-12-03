import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt._
import sbt.ScriptedPlugin.autoImport._
import sbtorgpolicies.OrgPoliciesKeys.orgAfterCISuccessTaskListSetting
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      name := "sbt-org-policies",
      organization := "com.47deg",
      organizationName := "47 Degrees",
      organizationHomepage := Some(url("http://47deg.com"))
    )

    lazy val pluginSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                % "1.0.0"),
      addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.4.2"),
      addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.10"),
      addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "2.3"),
      addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.1.2"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.2"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.3.2"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.3.4"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "1.0.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.6.0-M3"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.25"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "3.0.1"),
      addSbtPlugin("com.47deg"          %% "sbt-dependencies"      % "0.3.9"),
      addSbtPlugin("com.47deg"          %% "sbt-microsites"        % "0.7.24"),
      addSbtPlugin("org.tpolecat"       % "tut-plugin"             % "0.6.9"),
      addSbtPlugin("com.geirsson"       % "sbt-scalafmt"           % "1.2.0"),
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
    )

    lazy val coreSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      scalaVersion := "2.12.7",
      crossScalaVersions := Seq("2.12.7"),
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.1.1",
        %%("github4s"),
        %%("cats-core"),
        %%("base64"),
        %%("moultingyaml"),
        %%("cats-laws")             % Test,
        %%("scalacheck", "1.13.5")  % Test force(),
        %%("scalatest")             % Test,
        %%("scheckToolboxDatetime") % Test,
        %%("scalamockScalatest")    % Test
      ),
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := "2.12.7",
    crossScalaVersions := Seq("2.12.7"),
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
