import sbt.Keys._
import sbtorgpolicies.OrgPoliciesKeys.orgGithubSetting
import sbtorgpolicies.libraries.{javaLibs, scalaLibs}
import sbtorgpolicies.model.GitHubSettings

lazy val `org-policies-auto-dep-check` = (project in file("."))
  .settings(name := "org-policies-auto-dep-check")
  .settings(noPublishSettings: _*)
  .settings(Seq(
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.11.8"),
    scalaOrganization := "org.scala-lang",
    orgGithubSetting := GitHubSettings(
      organization = "47deg",
      project = "sbt-org-policies",
      organizationName = "47 Degrees",
      groupId = "com.47deg",
      organizationHomePage = url("http://47deg.com"),
      organizationEmail = "hello@47deg.com"
    ),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++=
      scalaLibs.mapValues(lib => lib._1  %% lib._2 % lib._3).values.toList ++
        javaLibs.mapValues(lib => lib._1 % lib._2  % lib._3).values.toList
  ): _*)
