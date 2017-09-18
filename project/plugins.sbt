import sbt.Resolver.sonatypeRepo

resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases"))
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.6.4-M1")
