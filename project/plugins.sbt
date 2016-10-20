addSbtPlugin("de.heikoseeberger" % "sbt-header"    % "1.6.0")
addSbtPlugin("org.typelevel"     % "sbt-catalysts" % "0.1.12")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"  % "0.4.7")

libraryDependencies <+= sbtVersion("org.scala-sbt" % "scripted-plugin" % _)
