import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport.{ShadeRule, assembly, _}
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
      addSbtPlugin("io.get-coursier"    %  "sbt-coursier"          % "1.0.3"),
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
      scriptedBufferLog := false,
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

    // Shade and bundle jawn-parser and intermediate deps (https://github.com/47deg/sbt-org-policies/issues/1173)
    lazy val jawnShadingSettings: Seq[Def.Setting[_]] = Seq(
      // intransitive, so we bundle the bare minimum dependencies to shade the chain of calls from our code to jawn-parser
      libraryDependencies ++= Seq(
        %%("github4s"),
        %%("circe-parser"),
        "io.circe" %% "circe-jawn" % "0.10.0",
        "org.spire-math" %% "jawn-parser" % "0.13.0",
      ).map(_.intransitive()),
      assembly / assemblyOption ~= (_.copy(includeScala = false)),
      // must be careful not to rename any calls to unshaded packages outside our bundled dependencies (ex. io.circe.** would break)
      assembly / assemblyShadeRules := Seq(ShadeRule.rename(
        "github4s.**"        -> "org_policies_github4s.@1",
        "io.circe.parser.**" -> "org_policies_circe_parser.@1",
        "io.circe.jawn.**"   -> "org_policies_circe_jawn.@1",
        "jawn.**"            -> "org_policies_jawn_parser.@1"
      )).map(_.inAll),
    ) ++ noPublishSettings

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
