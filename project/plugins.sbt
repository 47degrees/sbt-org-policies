resolvers += Resolver.sonatypeRepo("snapshots")
// addSbtPlugin("com.47deg"    % "sbt-org-policies" % "0.6.0-SNAPSHOT")
addSbtPlugin("com.jsuereth" % "sbt-pgp"          % "1.1.0-M1")

libraryDependencies += {
  lazy val sbtVersionValue = (sbtVersion in pluginCrossBuild).value

  scalaVersion.value match {
    case "2.10.6" => "org.scala-sbt" % "scripted-plugin"  % sbtVersionValue
    case _        => "org.scala-sbt" %% "scripted-plugin" % sbtVersionValue
  }
}
