addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.6.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
