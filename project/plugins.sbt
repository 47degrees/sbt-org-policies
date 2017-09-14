resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.6.3-SNAPSHOT")
// remove after a version of sbt-org-policies is published with sbt-dependencies
addSbtPlugin("com.47deg" % "sbt-dependencies" % "0.2.0")
