import sbt.Keys._
import sbtorgpolicies.OrgPoliciesKeys.orgGithubSetting
import sbtorgpolicies.io.FileReader
import sbtorgpolicies.libraries._
import sbtorgpolicies.model.GitHubSettings

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val checkDependencies = taskKey[Unit]("Check the module dependencies")

lazy val `org-policies-auto-dep-check` = (project in file("."))
  .settings(name := "org-policies-auto-dep-check")
  .settings(noPublishSettings: _*)
  .settings(Seq(
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.12.3"),
    scalaOrganization := "org.scala-lang",
    orgGithubSetting := GitHubSettings(
      organization = "47deg",
      project = "sbt-org-policies",
      organizationName = "47 Degrees",
      groupId = "com.47deg",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"), Resolver.bintrayIvyRepo("sbt", "sbt-plugin-releases")),
    libraryDependencies ++=
      scalaLibs
        .filter(_._1 != "scalameta-paradise")
        .mapValues(lib => lib._1 %% lib._2 % lib._3)
        .values
        .toList ++
        javaLibs.mapValues(lib => lib._1 % lib._2 % lib._3).values.toList,
    checkDependencies := Def.taskDyn {
      val fr             = new FileReader
      val versionSbtFile = baseDirectory.value.getParentFile / "version.sbt"
      val VersionRegex   = """.*"(.*-SNAPSHOT)".*""".r

      val currentPluginVersion =
        fr.getFileContent(versionSbtFile.getAbsolutePath) match {
          case Right(VersionRegex(v)) => Some(v)
          case _                      => None
        }

      val isTravisMaster = getEnvVarOrElse("TRAVIS_BRANCH") == "master" &&
        getEnvVarOrElse("TRAVIS_PULL_REQUEST") == "false"

      if (isTravisMaster && currentPluginVersion.isDefined)
        Def.task(depUpdateDependencyIssues.value)
      else
        Def.task(streams.value.log.warn("Skipping auto-dependency check"))
    }.value
  ): _*)
  .settings(
    allPlugins
      .mapValues(lib => addSbtPlugin(lib._1 % lib._2 % lib._3))
      .values
      .toList: _*)
