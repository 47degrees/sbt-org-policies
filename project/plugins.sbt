addSbtPlugin("de.heikoseeberger" % "sbt-header"       % "1.8.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"          % "1.0.1")
addSbtPlugin("com.github.gseitz" % "sbt-release"      % "1.0.4")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"     % "1.1")
addSbtPlugin("org.scala-js"      % "sbt-scalajs"      % "0.6.14")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"     % "0.6.3")
addSbtPlugin("com.47deg"         % "sbt-dependencies" % "0.0.4")
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
