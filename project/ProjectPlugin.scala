// import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt._
import sbt.ScriptedPlugin.autoImport._
// import sbtorgpolicies.OrgPoliciesKeys.orgAfterCISuccessTaskListSetting
// import sbtorgpolicies.OrgPoliciesPlugin
// import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
// import sbtorgpolicies.runnable.syntax._
// import sbtorgpolicies.templates.badges._
// import sbtorgpolicies.model.{sbtV, scalac}

object ProjectPlugin extends AutoPlugin {

  // override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  object sbtV {
    val `0.13`: String = "0.13.16"
    val `1.0`: String  = "1.0.1"

    val crossSbtVersions: List[String] = List(`0.13`, `1.0`)
  }

  object scalac {

    val `2.10`: String = "2.10.6"
    val `2.11`: String = "2.11.11"
    val `2.12`: String = "2.12.3"
    val `2.13`: String = "2.13.0-M1"

    val latestScalaVersion: String = `2.12`

    val crossScalaVersions: List[String] = List(`2.11`, `2.12`)

  }

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
      // addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.1.0" exclude ("org.spire-math","jawn-parser")),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.2"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.3.1"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.27"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "1.0.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.1"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.20"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "3.0.1"),
      // addSbtPlugin("com.47deg"          % "sbt-dependencies"       % "0.3.4"),
      // addSbtPlugin("com.47deg"          % "sbt-microsites"         % "0.7.3"),
      libraryDependencies ++= {
        val sbtBinaryVersionValue = (sbtBinaryVersion in pluginCrossBuild).value

        val scalaBinaryVersionValue = (scalaBinaryVersion in update).value

        val (tutPluginVersion, sbtScalafmtVersion) = sbtBinaryVersionValue match {
          case "0.13" => ("0.5.5", "0.6.8")
          case "1.0"  => ("0.6.1", "1.2.0")
        }

        Seq(
          // Defaults.sbtPluginExtra(
          //   "org.tpolecat" % "tut-plugin" % tutPluginVersion,
          //   sbtBinaryVersionValue,
          //   scalaBinaryVersionValue),
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
      }
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
        "com.47deg"             %% "github4s"                    % "0.16.0" exclude ("com.github.mpilquist", "simulacrum_2.10") exclude ("com.github.mpilquist", "simulacrum_2.12"),
        "org.typelevel"         %% "cats-core"                   % "1.0.0-MF",
        "org.typelevel"         %% "cats-free"                   % "1.0.0-MF",
        "org.typelevel"         %% "cats-mtl-core"               % "0.0.2",
        "com.github.marklister" %% "base64"                      % "0.2.3",
        "net.jcazevedo"         %% "moultingyaml"                % "0.4.0",
        "org.typelevel"         %% "cats-laws"                   % "1.0.0-MF" % Test,
        "org.scalatest"         %% "scalatest"                   % "3.0.4" % Test,
        "org.scalacheck"        %% "scalacheck"                  % "1.13.5" % Test,
        "com.47deg"             %% "scalacheck-toolbox-datetime" % "0.2.2" % Test,
        "org.scalamock"         %% "scalamock-scalatest-support" % "3.6.0" % Test
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

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings // ++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := scalac.`2.12`,
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017)
    // orgBadgeListSetting := List(
    //   TravisBadge.apply,
    //   MavenCentralBadge.apply,
    //   LicenseBadge.apply,
    //   GitHubIssuesBadge.apply
    // )//,
    // orgAfterCISuccessTaskListSetting ~= (_ filterNot (_ == depUpdateDependencyIssues.asRunnableItem))
  )
}
