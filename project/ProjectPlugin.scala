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
import sbtorgpolicies.model.{sbtV, scalac}

object ProjectPlugin extends AutoPlugin {

  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
      name := "sbt-org-policies",
      organization := "com.47deg",
      organizationName := "47 Degrees",
      organizationHomepage := Some(url("http://47deg.com")),
      crossScalaVersions := Seq(scalac.`2.12`),
      resolvers += Resolver.url("dwijnand-sbt-plugins", url("https://dl.bintray.com/dwijnand/sbt-plugins/"))(
        Resolver.ivyStylePatterns),
      libraryDependencies += {
        Defaults.sbtPluginExtra(
          "com.dwijnand" % "sbt-compat" % "1.0.0",
          (sbtBinaryVersion in pluginCrossBuild).value,
          (scalaBinaryVersion in update).value
        )
      }
    )

    lazy val pluginSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      sbtPlugin := true,
      crossSbtVersions := Seq(sbtV.`0.13`, sbtV.`1.0`),
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                % "0.9.3"),
      addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.4.1"),
      addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.6"),
      addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "2.0"),
      addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.1.0"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.2"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.3.1"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.27"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "1.0.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.1"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.20"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "3.0.1"),
      addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.3.1"),
      // addSbtPlugin("com.lucidchart"     % "sbt-scalafmt"  % "1.10"),
      // addSbtPlugin("com.geirsson"       % "sbt-scalafmt"  % "1.2.0"),
      libraryDependencies ++= {
        val sbtBinaryVersionValue = (sbtBinaryVersion in pluginCrossBuild).value

        val scalaBinaryVersionValue = (scalaBinaryVersion in update).value

        val (tutPluginVersion, sbtScalafmtVersion) = sbtBinaryVersionValue match {
          case "0.13" => ("0.5.5", "0.6.8")
          case "1.0"  => ("0.6.1", "1.2.0")
        }

        Seq(
          Defaults.sbtPluginExtra(
            "org.tpolecat" % "tut-plugin" % tutPluginVersion,
            sbtBinaryVersionValue,
            scalaBinaryVersionValue),
          Defaults.sbtPluginExtra(
            "com.geirsson" % "sbt-scalafmt" % sbtScalafmtVersion,
            sbtBinaryVersionValue,
            scalaBinaryVersionValue)
        )
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
      },
      addSbtPlugin("com.47deg" % "sbt-microsites" % "0.7.2")
    )

    lazy val coreSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      crossScalaVersions := Seq(scalac.`2.10`, scalac.`2.12`),
      scalaVersion := {
        (sbtBinaryVersion in pluginCrossBuild).value match {
          case "0.13" => scalac.`2.10`
          case "1.0"  => scalac.`2.12`
        }
      },
      libraryDependencies ++= Seq(
        %%("github4s"),
        %%("cats-core"),
        %%("base64"),
        %%("moultingyaml"),
        %%("scalatest")             % Test,
        %%("scalacheck")            % Test,
        %%("scheckToolboxDatetime") % Test,
        %%("scalamockScalatest")    % Test,
      ),
      libraryDependencies ++= {
        lazy val sbtVersionValue = (sbtVersion in pluginCrossBuild).value

        (scalaVersion in update).value match {
          case scalac.`2.10` =>
            Seq(
              "org.scala-sbt" % "sbt" % sbtVersionValue % "provided"
            )
          case scalac.`2.12` =>
            Seq(
              "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
              "org.scala-sbt"          % "sbt"        % sbtVersionValue % "provided"
            )
          case _ => Nil
        }
      }
    )

  }

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := scalac.`2.12`,
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
