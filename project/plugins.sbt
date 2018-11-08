import sbt.Resolver.sonatypeRepo
resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases"))
addSbtPlugin("com.47deg" % "sbt-org-policies" % "0.10.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")