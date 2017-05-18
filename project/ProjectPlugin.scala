import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin._
import sbt._
import sbtorgpolicies.OrgPoliciesKeys.orgAfterCISuccessTaskListSetting
import sbtorgpolicies.OrgPoliciesPlugin
import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
import sbtorgpolicies.runnable.syntax._
import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val pluginSettings: Seq[Def.Setting[_]] = Seq(
      sbtPlugin := true,
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                % "0.9.2" commonExcludes),
      addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.4.0"),
      addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.4"),
      addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "1.1"),
      addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.0.1"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.0"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.2.0"),
      addSbtPlugin("org.tpolecat"       % "tut-plugin"             % "0.5.2"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.24"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "0.8.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.0"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.16"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "1.8.0"),
      addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"          % "0.7.0"),
      addSbtPlugin("com.geirsson"       % "sbt-scalafmt"           % "0.6.8"),
      addSbtPlugin("io.get-coursier"    % "sbt-coursier"           % "1.0.0-RC3"),
      addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.1.1"),
      addSbtPlugin("com.47deg"          % "sbt-microsites"         % "0.5.7")
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
      resolvers += Resolver.typesafeIvyRepo("releases"),
      libraryDependencies ++= Seq(
        %%("github4s"),
        %%("cats"),
        %("joda-convert"),
        %("joda-time"),
        %%("base64"),
        %%("moultingyaml"),
        %%("scalatest")             % Test,
        %%("scalacheck")            % Test,
        %%("scheckToolboxDatetime") % Test,
        "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test,
        "org.scala-sbt"             % "scripted-plugin" % sbtVersion.value
      )
    )

    implicit class ModuleExcludes(module: ModuleID) {

      def commonExcludes: ModuleID =
        module
          .exclude("javax.jms", "jms")
          .exclude("com.sun.jdmk", "jmxtools")
          .exclude("com.sun.jmx", "jmxri")
    }

  }

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017),
    orgBadgeListSetting := List(
      TravisBadge.apply,
      MavenCentralBadge.apply,
      LicenseBadge.apply,
      GitHubIssuesBadge.apply
    ),
    orgAfterCISuccessTaskListSetting ~= (_ filterNot(_ == depUpdateDependencyIssues.asRunnableItem))
  )
}
