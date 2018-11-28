resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.47deg" % "sbt-org-policies" % sys.props("plugin.version"))

excludeDependencies += "io.circe" %% "circe-jawn"
