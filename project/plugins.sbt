import Resolver.sonatypeRepo
resolvers ++= Seq(sonatypeRepo("snapshots"), sonatypeRepo("releases"))

addSbtPlugin("com.47deg"        % "sbt-org-policies"    % "0.13.3")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "0.14.10")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo"       % "0.9.0")
addSbtPlugin("com.geirsson"     % "sbt-ci-release"      % "1.5.2")
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.2")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"        % "2.3.2")
