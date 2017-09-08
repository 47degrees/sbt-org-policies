//import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt.{Def, _}
// import sbtorgpolicies.OrgPoliciesKeys.orgAfterCISuccessTaskListSetting
// import sbtorgpolicies.OrgPoliciesPlugin
// import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
// import sbtorgpolicies.runnable.syntax._
// import sbtorgpolicies.templates.badges._
// import sbtorgpolicies.model.scalac

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
      addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3" commonExcludes),
      addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"             % "0.4.1"),
      addSbtPlugin("com.github.gseitz"  % "sbt-release"            % "1.0.6"),
      addSbtPlugin("org.xerial.sbt"     % "sbt-sonatype"           % "2.0"),
      addSbtPlugin("com.jsuereth"       % "sbt-pgp"                % "1.1.0-M1"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.2"),
      addSbtPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.3.0"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.2.27"),
      addSbtPlugin("org.scalastyle"     %% "scalastyle-sbt-plugin" % "1.0.0"),
      addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.1"),
      addSbtPlugin("org.scala-js"       % "sbt-scalajs"            % "0.6.19"),
      addSbtPlugin("de.heikoseeberger"  % "sbt-header"             % "3.0.1"),
      addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"          % "0.7.0"),
      // addSbtPlugin("com.lucidchart"     % "sbt-scalafmt"  % "1.10"),
      // addSbtPlugin("com.geirsson"       % "sbt-scalafmt"  % "1.2.0"),
      libraryDependencies ++= {
        val sbtVersionValue       = (sbtVersion in pluginCrossBuild).value
        val sbtBinaryVersionValue = (sbtBinaryVersion in pluginCrossBuild).value

        val scalaBinaryVersionValue = (scalaBinaryVersion in update).value

        val (tutPluginVersion, sbtScalafmtVersion) = sbtVersionValue match {
          case sbtV.`0.13` => ("0.5.3", "0.6.8")
          case sbtV.`1.0`  => ("0.6.0", "1.2.0")
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
      }
//      addSbtPlugin("io.get-coursier"          % "sbt-coursier"           % "1.0.0-RC11"),
//      addCustomSBTPlugin("com.47deg"          % "sbt-dependencies"       % "0.1.1", sbt210 = true),
//      addCustomSBTPlugin("com.47deg"          % "sbt-microsites"         % "0.6.1", sbt210 = true)
    ) ++ Seq(
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
        (sbtVersion in pluginCrossBuild).value match {
          case sbtV.`0.13` => scalac.`2.10`
          case sbtV.`1.0`  => scalac.`2.12`
        }
      },
      libraryDependencies ++= Seq(
        "com.47deg"             %% "github4s"                    % "0.15.0",
        "org.typelevel"         %% "cats-core"                   % "0.9.0",
        "com.github.marklister" %% "base64"                      % "0.2.3",
        "net.jcazevedo"         %% "moultingyaml"                % "0.4.0",
        "org.scalatest"         %% "scalatest"                   % "3.0.3" % Test,
        "org.scalacheck"        %% "scalacheck"                  % "1.13.4" % Test,
        "com.47deg"             %% "scalacheck-toolbox-datetime" % "0.2.2" % Test,
        "org.scalamock"         %% "scalamock-scalatest-support" % "3.5.0" % Test
//        %%("github4s"),
//        %%("cats"),
//        %%("base64"),
//        %%("moultingyaml"),
//        %%("scalatest")             % Test,
//        %%("scalacheck")            % Test,
//        %%("scheckToolboxDatetime") % Test,
      ),
      libraryDependencies ++= {
        lazy val sbtVersionValue = (sbtVersion in pluginCrossBuild).value

        (scalaVersion in update).value match {
          case scalac.`2.10` =>
            Seq(
              "org.scala-sbt" % "scripted-plugin" % sbtVersionValue
            )
          case _ =>
            Seq(
              "org.scala-lang.modules" %% "scala-xml"       % "1.0.6",
              "org.scala-sbt"          %% "scripted-plugin" % sbtVersionValue
            )
        }
      }
    )

    implicit class ModuleExcludes(module: ModuleID) {

      def commonExcludes: ModuleID =
        module
          .exclude("javax.jms", "jms")
          .exclude("com.sun.jdmk", "jmxtools")
          .exclude("com.sun.jmx", "jmxri")

      def exclude210Suffixes: ModuleID =
        module
          .excludeAll(ExclusionRule(organization = "org.scala-sbt"))
          .excludeAll(ExclusionRule(organization = "com.github.mpilquist"))
          .excludeAll(ExclusionRule(organization = "org.typelevel"))
          .excludeAll(ExclusionRule(organization = "io.circe"))
          .excludeAll(ExclusionRule(organization = "com.chuusai"))
          .excludeAll(ExclusionRule(organization = "com.lihaoyi"))
          .excludeAll(ExclusionRule(organization = "com.github.nscala-time"))
          .excludeAll(ExclusionRule(organization = "net.jcazevedo"))
          .excludeAll(ExclusionRule(organization = "com.github.marklister"))
          .excludeAll(ExclusionRule(organization = "org.scalaj"))
          .exclude("com.47deg", "github4s")
    }

    private[this] def addPlugin(module: ModuleID, sbt210: Boolean = false) =
      if (sbt210) addSbtPlugin(module exclude210Suffixes, "0.13", "2.10")
      else addSbtPlugin(module)

  }

  override def projectSettings: Seq[Def.Setting[_]] = artifactSettings //++ shellPromptSettings

  private[this] val artifactSettings = Seq(
    scalaVersion := scalac.`2.12`,
    scalaOrganization := "org.scala-lang",
    startYear := Some(2017)
//    ,
//    orgBadgeListSetting := List(
//      TravisBadge.apply,
//      MavenCentralBadge.apply,
//      LicenseBadge.apply,
//      GitHubIssuesBadge.apply
//    ),
//    orgAfterCISuccessTaskListSetting ~= (_ filterNot(_ == depUpdateDependencyIssues.asRunnableItem))
  )
}
