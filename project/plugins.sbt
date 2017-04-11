resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.4.6-SNAPSHOT")
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
