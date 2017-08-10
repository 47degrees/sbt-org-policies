//import dependencies.DependenciesPlugin.autoImport.depUpdateDependencyIssues
import sbt.Keys._
import sbt.Resolver.sonatypeRepo
import sbt.ScriptedPlugin.autoImport._
import sbt.{Def, _}
//import sbtorgpolicies.OrgPoliciesKeys.orgAfterCISuccessTaskListSetting
//import sbtorgpolicies.OrgPoliciesPlugin
//import sbtorgpolicies.OrgPoliciesPlugin.autoImport._
//import sbtorgpolicies.runnable.syntax._
//import sbtorgpolicies.templates.badges._

object ProjectPlugin extends AutoPlugin {

//  override def requires: Plugins = OrgPoliciesPlugin

  override def trigger: PluginTrigger = allRequirements

  private[this] val scala212 = "2.12.3"

  object autoImport {

    lazy val commonSettings = Seq(
      name := "sbt-org-policies",
      organization := "com.47deg",
      organizationName := "47 Degrees",
      organizationHomepage := Some(url("http://47deg.com")),
      crossScalaVersions := Seq(scala212)
    )

    lazy val pluginSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      sbtPlugin := true,
      crossSbtVersions := Vector("0.13.16", "1.0.0"),
      scalaVersion := scala212,
      resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases")),
      addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3" commonExcludes),
      addSbtPlugin("com.eed3si9n"      % "sbt-unidoc"   % "0.4.1"),
      addSbtPlugin("com.github.gseitz" % "sbt-release"  % "1.0.6"),
      addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype" % "2.0"),
      addSbtPlugin("com.jsuereth"      % "sbt-pgp"      % "1.1.0-M1"),
//      addPlugin("com.typesafe.sbt"   % "sbt-ghpages"            % "0.6.1", sbt210 = true),
//      addPlugin("com.typesafe.sbt"   % "sbt-site"               % "1.2.1", sbt210 = true),
      addSbtPlugin("org.tpolecat"       % "tut-plugin" % "0.6.0"),
      addSbtPlugin("pl.project13.scala" % "sbt-jmh"    % "0.2.27"),
//      addPlugin("org.scoverage"      % "sbt-scoverage"          % "1.5.0", sbt210 = true),
      addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.19"),
//      addPlugin("de.heikoseeberger"  % "sbt-header"             % "1.8.0", sbt210 = true),
      addSbtPlugin("com.eed3si9n"    % "sbt-buildinfo" % "0.7.0"),
      addSbtPlugin("com.lucidchart"  % "sbt-scalafmt"  % "1.10"),
      addSbtPlugin("io.get-coursier" % "sbt-coursier"  % "1.0.0-RC10")
//      ,
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
//      ScriptedPlugin.scriptedSettings ++ Seq(
//      scriptedDependencies := (compile in Test) map { _ =>
//        Unit
//      },
//      scriptedLaunchOpts := {
//        scriptedLaunchOpts.value ++
//          Seq(
//            "-Xmx2048M",
//            "-XX:ReservedCodeCacheSize=256m",
//            "-XX:+UseConcMarkSweepGC",
//            "-Dplugin.version=" + version.value,
//            "-Dscala.version=" + scalaVersion.value
//          )
//      }
//    )

    lazy val coreSettings: Seq[Def.Setting[_]] = commonSettings ++ Seq(
      resolvers += Resolver.typesafeIvyRepo("releases"),
      libraryDependencies ++= Seq(
        "org.scala-sbt"          %% "scripted-plugin"             % sbtVersion.value,
        "com.47deg"              %% "github4s"                    % "0.15.0",
        "org.typelevel"          %% "cats-core"                   % "0.9.0",
        "com.github.marklister"  %% "base64"                      % "0.2.3",
        "net.jcazevedo"          %% "moultingyaml"                % "0.4.0",
        "org.scala-lang.modules" %% "scala-xml"                   % "1.0.5",
        "org.scalatest"          %% "scalatest"                   % "3.0.3" % Test,
        "org.scalacheck"         %% "scalacheck"                  % "1.13.4" % Test,
        "com.47deg"              %% "scalacheck-toolbox-datetime" % "0.2.2" % Test,
        "org.scalamock"          %% "scalamock-scalatest-support" % "3.5.0" % Test
        //        %%("github4s"),
//        %%("cats"),
//        %%("base64"),
//        %%("moultingyaml"),
//        %%("scalatest")             % Test,
//        %%("scalacheck")            % Test,
//        %%("scheckToolboxDatetime") % Test,
      )
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
    scalaVersion := scala212,
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
