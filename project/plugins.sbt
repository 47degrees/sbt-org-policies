import sbt.Resolver.sonatypeRepo
resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases"))
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.9.2")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")