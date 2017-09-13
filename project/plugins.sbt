resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.6.3-SNAPSHOT")
// remove after a version of sbt-org-policies is published with sbt-dependencies
addSbtPlugin("com.47deg" % "sbt-dependencies" % "0.2.0")

libraryDependencies += {
  lazy val sbtVersionValue = (sbtVersion in pluginCrossBuild).value

  scalaVersion.value match {
    case "2.10.6" => "org.scala-sbt" % "scripted-plugin"  % sbtVersionValue
    case _        => "org.scala-sbt" %% "scripted-plugin" % sbtVersionValue
  }
}
