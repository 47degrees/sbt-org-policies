/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtorgpolicies

object libraries {

  type Artifact = (String, String, String)

  val v47: Map[String, String] = Map[String, String](
    "case-classy"           -> "0.3.0",
    "fetch"                 -> "0.6.0",
    "github4s"              -> "0.14.2",
    "scheckToolboxDatetime" -> "0.2.1"
  )

  protected val vOthers: Map[String, String] = Map[String, String](
    "akka"            -> "2.4.17",
    "akka-http"       -> "10.0.5",
    "algebra"         -> "0.6.0",
    "alleycats"       -> "0.1.9",
    "aws-sdk"         -> "1.11.83",
    "base64"          -> "0.2.3",
    "catalysts"       -> "0.0.5",
    "catbird"         -> "0.13.0",
    "cats"            -> "0.9.0",
    "circe"           -> "0.7.0",
    "config"          -> "1.3.0",
    "coursier"        -> "1.0.0-M15-6",
    "discipline"      -> "0.7.3",
    "doobie"          -> "0.4.1",
    "embedded-redis"  -> "0.6",
    "export-hook"     -> "1.2.0",
    "finch"           -> "0.13.0",
    "fs2"             -> "0.9.4",
    "fs2-cats"        -> "0.3.0",
    "h2"              -> "1.4.194",
    "http4s"          -> "0.17.0-SNAPSHOT",
    "joda-convert"    -> "1.8.1",
    "joda-time"       -> "2.9.9",
    "journal"         -> "3.0.18",
    "jwt-scala"       -> "0.12.0",
    "kind-projector"  -> "0.9.3",
    "log4s"           -> "1.3.4",
    "machinist"       -> "0.6.1",
    "macro-compat"    -> "1.1.1",
    "monix"           -> "2.2.3",
    "monocle"         -> "1.4.0",
    "mockito"         -> "2.7.19",
    "moultingyaml"    -> "0.4.0",
    "newrelic"        -> "3.29.0",
    "paradise"        -> "2.1.0",
    "pcplod"          -> "1.2.0",
    "play"            -> "2.6.0-M2",
    "pure-config"     -> "0.5.1",
    "rediscala"       -> "1.8.0",
    "refined"         -> "0.6.2",
    "roshttp"         -> "2.0.1",
    "scala-reflect"   -> "2.11.8",
    "scalacheck"      -> "1.13.5",
    "scheckShapeless" -> "1.1.4",
    "scalaj"          -> "2.3.0",
    "scalatest"       -> "3.0.1",
    "scalaz"          -> "7.2.9",
    "scalazspecs2"    -> "0.5.0",
    "scanamo"         -> "0.9.1",
    "shapeless"       -> "2.3.2",
    "shocon"          -> "0.1.7",
    "simulacrum"      -> "0.10.0",
    "slf4j"           -> "1.7.21",
    "slogging"        -> "0.5.2",
    "specs2"          -> "3.8.7",
    "slick"           -> "3.2.0"
  )

  val v: Map[String, String] = v47 ++ vOthers

  val langLibs: Map[String, Artifact] = Map(
    "paradise"      -> (("org.scalamacros", "paradise", v("paradise"))),
    "quasiquotes"   -> (("org.scalamacros", "quasiquotes", v("paradise"))),
    "scala-reflect" -> (("org.scala-lang", "scala-reflect", v("scala-reflect")))
  )

  val javaLibs: Map[String, Artifact] = Map(
    "aws-sdk"        -> (("com.amazonaws", "aws-java-sdk", v("aws-sdk"))),
    "config"         -> (("com.typesafe", "config", v("config"))),
    "embedded-redis" -> (("com.orange.redis-embedded", "embedded-redis", v("embedded-redis"))),
    "h2"             -> (("com.h2database", "h2", v("h2"))),
    "joda-convert"   -> (("org.joda", "joda-convert", v("joda-convert"))),
    "joda-time"      -> (("joda-time", "joda-time", v("joda-time"))),
    "mockito-core"   -> (("org.mockito", "mockito-core", v("mockito"))),
    "newrelic"       -> (("com.newrelic.agent.java", "newrelic-agent", v("newrelic"))),
    "slf4j-nop"      -> (("org.slf4j", "slf4j-nop", v("slf4j"))),
    "slf4j-simple"   -> (("org.slf4j", "slf4j-simple", v("slf4j")))
  )

  val scalaLibs: Map[String, Artifact] = Map(
    "akka-actor"             -> (("com.typesafe.akka", "akka-actor", v("akka"))),
    "akka-http"              -> (("com.typesafe.akka", "akka-http", v("akka-http"))),
    "akka-http-testkit"      -> (("com.typesafe.akka", "akka-http-testkit", v("akka-http"))),
    "algebra-laws"           -> (("org.typelevel", "algebra-laws", v("algebra"))),
    "algebra"                -> (("org.typelevel", "algebra", v("algebra"))),
    "alleycats"              -> (("org.typelevel", "alleycats-core", v("alleycats"))),
    "base64"                 -> (("com.github.marklister", "base64", v("base64"))),
    "classy-cats"            -> (("com.fortysevendeg", "classy-cats", v("case-classy"))),
    "classy-config-typesafe" -> (("com.fortysevendeg", "classy-config-typesafe", v("case-classy"))),
    "classy-config-shocon"   -> (("com.fortysevendeg", "classy-config-shocon", v("case-classy"))),
    "classy-core"            -> (("com.fortysevendeg", "classy-core", v("case-classy"))),
    "classy-generic"         -> (("com.fortysevendeg", "classy-generic", v("case-classy"))),
    "catalysts-checklite"    -> (("org.typelevel", "catalysts-checklite", v("catalysts"))),
    "catalysts-lawkit"       -> (("org.typelevel", "catalysts-lawkit", v("catalysts"))),
    "catalysts-macros"       -> (("org.typelevel", "catalysts-macros", v("catalysts"))),
    "catalysts-platform"     -> (("org.typelevel", "catalysts-platform", v("catalysts"))),
    "catalysts-scalatest"    -> (("org.typelevel", "catalysts-scalatest", v("catalysts"))),
    "catalysts-specbase"     -> (("org.typelevel", "catalysts-specbase", v("catalysts"))),
    "catalysts-speclite"     -> (("org.typelevel", "catalysts-speclite", v("catalysts"))),
    "catalysts-specs2"       -> (("org.typelevel", "catalysts-specs2", v("catalysts"))),
    "catalysts-testkit"      -> (("org.typelevel", "catalysts-testkit", v("catalysts"))),
    "catbird-util"           -> (("io.catbird", "catbird-util", v("catbird"))),
    "cats-core"              -> (("org.typelevel", "cats-core", v("cats"))),
    "cats-free"              -> (("org.typelevel", "cats-free", v("cats"))),
    "cats-kernel"            -> (("org.typelevel", "cats-kernel", v("cats"))),
    "cats-laws"              -> (("org.typelevel", "cats-laws", v("cats"))),
    "cats-macros"            -> (("org.typelevel", "cats-macros", v("cats"))),
    "cats"                   -> (("org.typelevel", "cats", v("cats"))),
    "circe-core"             -> (("io.circe", "circe-core", v("circe"))),
    "circe-generic"          -> (("io.circe", "circe-generic", v("circe"))),
    "circe-parser"           -> (("io.circe", "circe-parser", v("circe"))),
    "circe-optics"           -> (("io.circe", "circe-optics", v("circe"))),
    "coursier-cache"         -> (("io.get-coursier", "coursier-cache", v("coursier"))),
    "coursier"               -> (("io.get-coursier", "coursier", v("coursier"))),
    "discipline"             -> (("org.typelevel", "discipline", v("discipline"))),
    "doobie-core"            -> (("org.tpolecat", "doobie-core", v("doobie"))),
    "doobie-core-cats"       -> (("org.tpolecat", "doobie-core-cats", v("doobie"))),
    "doobie-hikari"          -> (("org.tpolecat", "doobie-hikari", v("doobie"))),
    "doobie-hikari-cats"     -> (("org.tpolecat", "doobie-hikari-cats", v("doobie"))),
    "doobie-h2"              -> (("org.tpolecat", "doobie-h2", v("doobie"))),
    "doobie-h2-cats"         -> (("org.tpolecat", "doobie-h2-cats", v("doobie"))),
    "doobie-postgres"        -> (("org.tpolecat", "doobie-postgres", v("doobie"))),
    "doobie-postgres-cats"   -> (("org.tpolecat", "doobie-postgres-cats", v("doobie"))),
    "doobie-scalatest"       -> (("org.tpolecat", "doobie-scalatest", v("doobie"))),
    "doobie-scalatest-cats"  -> (("org.tpolecat", "doobie-scalatest-cats", v("doobie"))),
    "doobie-specs2"          -> (("org.tpolecat", "doobie-specs2", v("doobie"))),
    "doobie-specs2-cats"     -> (("org.tpolecat", "doobie-specs2-cats", v("doobie"))),
    "export-hook"            -> (("org.typelevel", "export-hook", v("export-hook"))),
    "fetch"                  -> (("com.47deg", "fetch", v("fetch"))),
    "fetch-monix"            -> (("com.47deg", "fetch-monix", v("fetch"))),
    "fetch-debug"            -> (("com.47deg", "fetch-debug", v("fetch"))),
    "finch-core"             -> (("com.github.finagle", "finch-core", v("finch"))),
    "fs2-core"               -> (("co.fs2", "fs2-core", v("fs2"))),
    "fs2-io"                 -> (("co.fs2", "fs2-io", v("fs2"))),
    "fs2-cats"               -> (("co.fs2", "fs2-cats", v("fs2-cats"))),
    "github4s"               -> (("com.47deg", "github4s", v("github4s"))),
    "http4s-blaze-client"    -> (("org.http4s", "http4s-blaze-client", v("http4s"))),
    "http4s-blaze-server"    -> (("org.http4s", "http4s-blaze-server", v("http4s"))),
    "http4s-circe"           -> (("org.http4s", "http4s-circe", v("http4s"))),
    "http4s-core"            -> (("org.http4s", "http4s-core", v("http4s"))),
    "http4s-dsl"             -> (("org.http4s", "http4s-dsl", v("http4s"))),
    "journal-core"           -> (("io.verizon.journal", "core", v("journal"))),
    "jwt-core"               -> (("com.pauldijou", "jwt-core", v("jwt-scala"))),
    "jwt-play-json"          -> (("com.pauldijou", "jwt-play-json", v("jwt-scala"))),
    "jwt-play"               -> (("com.pauldijou", "jwt-play", v("jwt-scala"))),
    "jwt-json4s-native"      -> (("com.pauldijou", "jwt-json4s-native", v("jwt-scala"))),
    "jwt-json4s-jackson"     -> (("com.pauldijou", "jwt-json4s-jackson", v("jwt-scala"))),
    "jwt-circe"              -> (("com.pauldijou", "jwt-circe", v("jwt-scala"))),
    "jwt-upickle"            -> (("com.pauldijou", "jwt-upickle", v("jwt-scala"))),
    "kind-projector"         -> (("org.spire-math", "kind-projector", v("kind-projector"))),
    "log4s"                  -> (("org.log4s", "log4s", v("log4s"))),
    "machinist"              -> (("org.typelevel", "machinist", v("machinist"))),
    "macro-compat"           -> (("org.typelevel", "macro-compat", v("macro-compat"))),
    "monix"                  -> (("io.monix", "monix", v("monix"))),
    "monix-cats"             -> (("io.monix", "monix-cats", v("monix"))),
    "monix-eval"             -> (("io.monix", "monix-eval", v("monix"))),
    "monocle-core"           -> (("com.github.julien-truffaut", "monocle-core", v("monocle"))),
    "monocle-generic"        -> (("com.github.julien-truffaut", "monocle-generic", v("monocle"))),
    "monocle-law"            -> (("com.github.julien-truffaut", "monocle-law", v("monocle"))),
    "monocle-macro"          -> (("com.github.julien-truffaut", "monocle-macro", v("monocle"))),
    "monocle-state"          -> (("com.github.julien-truffaut", "monocle-state", v("monocle"))),
    "moultingyaml"           -> (("net.jcazevedo", "moultingyaml", v("moultingyaml"))),
    "play"                   -> (("com.typesafe.play", "play", v("play"))),
    "play-test"              -> (("com.typesafe.play", "play-test", v("play"))),
    "pcplod"                 -> (("org.ensime", "pcplod", v("pcplod"))),
    "pure-config"            -> (("com.github.melrief", "pureconfig", v("pure-config"))),
    "rediscala"              -> (("com.github.etaty", "rediscala", v("rediscala"))),
    "refined-scalacheck"     -> (("eu.timepit", "refined-scalacheck", v("refined"))),
    "refined-scalaz"         -> (("eu.timepit", "refined-scalaz", v("refined"))),
    "refined-scodec"         -> (("eu.timepit", "refined-scodec", v("refined"))),
    "refined"                -> (("eu.timepit", "refined", v("refined"))),
    "roshttp"                -> (("fr.hmil", "roshttp", v("roshttp"))),
    "scalacheck"             -> (("org.scalacheck", "scalacheck", v("scalacheck"))),
    "scheckShapeless"        -> (("com.github.alexarchambault", "scalacheck-shapeless_1.13", v("scheckShapeless"))),
    "scheckToolboxDatetime"  -> (("com.fortysevendeg", "scalacheck-toolbox-datetime", v("scheckToolboxDatetime"))),
    "scalaj"                 -> (("org.scalaj", "scalaj-http", v("scalaj"))),
    "scalatest"              -> (("org.scalatest", "scalatest", v("scalatest"))),
    "scalaz-concurrent"      -> (("org.scalaz", "scalaz-concurrent", v("scalaz"))),
    "scalazspecs2"           -> (("org.typelevel", "scalaz-specs2", v("scalazspecs2"))),
    "scanamo"                -> (("com.gu", "scanamo", v("scanamo"))),
    "shapeless"              -> (("com.chuusai", "shapeless", v("shapeless"))),
    "shocon"                 -> (("eu.unicredit", "shocon", v("shocon"))),
    "simulacrum"             -> (("com.github.mpilquist", "simulacrum", v("simulacrum"))),
    "slick"                  -> (("com.typesafe.slick", "slick", v("slick"))),
    "slogging"               -> (("biz.enef", "slogging", v("slogging"))),
    "specs2-core"            -> (("org.specs2", "specs2-core", v("specs2"))),
    "specs2-scalacheck"      -> (("org.specs2", "specs2-scalacheck", v("specs2")))
  )

  val libs: Map[String, Artifact] = langLibs ++ javaLibs ++ scalaLibs
}
