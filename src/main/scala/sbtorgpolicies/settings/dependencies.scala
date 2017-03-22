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

package sbtorgpolicies.settings

import sbt._
import sbtorgpolicies.Dep

import scala.language.postfixOps

trait dependencies {

  def %(artifactId: String): ModuleID =
    getLib(artifactId).toModuleId.cross(CrossVersion.Disabled)

  def %(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toModuleId.cross(CrossVersion.Disabled)

  def %%(artifactId: String): ModuleID =
    getLib(artifactId).toModuleId

  def %%(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toModuleId

  def %%%(artifactId: String): ModuleID =
    getLib(artifactId).toJsModuleId

  def %%%(artifactId: String, version: String): ModuleID =
    getLib(artifactId, Some(version)).toJsModuleId

  private[this] def getLib(lib: String, version: Option[String] = None) =
    version
      .foldLeft(libs(lib))((module, revision) => module.copy(revision = revision))

  private[this] val v47: Map[String, String] = Map[String, String](
    "fetch"    -> "0.5.0",
    "github4s" -> "0.12.0"
  )

  private[this] val vOthers: Map[String, String] = Map[String, String](
    "akka"            -> "2.4.17",
    "algebra"         -> "0.6.0",
    "alleycats"       -> "0.1.7",
    "aws-sdk"         -> "1.11.83",
    "base64"          -> "0.2.3",
    "catalysts"       -> "0.1.0",
    "cats"            -> "0.9.0",
    "circe"           -> "0.7.0",
    "config"          -> "1.3.0",
    "coursier"        -> "1.0.0-M15-1",
    "discipline"      -> "0.7.3",
    "doobie"          -> "0.4.1",
    "embedded-redis"  -> "0.6",
    "evaluator"       -> "0.1.1-SNAPSHOT",
    "export-hook"     -> "1.2.0",
    "fs2"             -> "0.9.4",
    "fs2-cats"        -> "0.3.0",
    "http4s"          -> "0.16.0-cats-SNAPSHOT",
    "journal"         -> "3.0.18",
    "kind-projector"  -> "0.9.3",
    "log4s"           -> "1.3.4",
    "machinist"       -> "0.6.1",
    "macro-compat"    -> "1.1.1",
    "monix"           -> "2.2.3",
    "monocle"         -> "1.4.0",
    "newrelic"        -> "3.29.0",
    "paradise"        -> "2.1.0",
    "pcplod"          -> "1.2.0",
    "pure-config"     -> "0.5.1",
    "rediscala"       -> "1.8.0",
    "refined"         -> "0.6.2",
    "roshttp"         -> "2.0.1",
    "scala-exercises" -> "0.3.0-SNAPSHOT",
    "scala-reflect"   -> "2.11.8",
    "scalacheck"      -> "1.13.5",
    "scheckShapeless" -> "1.1.4",
    "scalaj"          -> "2.3.0",
    "scalatest"       -> "3.0.1",
    "scalaz"          -> "7.2.9",
    "scalazspecs2"    -> "0.4.0",
    "scanamo"         -> "0.9.1",
    "shapeless"       -> "2.3.2",
    "shocon"          -> "0.1.7",
    "simulacrum"      -> "0.10.0",
    "slf4j"           -> "1.7.21",
    "slogging"        -> "0.5.2",
    "specs2"          -> "3.8.7"
  )

  private[this] val v: Map[String, String] = v47 ++ vOthers

  private[this] val libs: Map[String, Dep] = Map(
    "akka-actor"                  -> Dep("com.typesafe.akka", "akka-actor", v("akka")),
    "algebra-laws"                -> Dep("org.typelevel", "algebra-laws", v("algebra")),
    "algebra"                     -> Dep("org.typelevel", "algebra", v("algebra")),
    "alleycats"                   -> Dep("org.typelevel", "alleycats", v("alleycats")),
    "aws-sdk"                     -> Dep("com.amazonaws", "aws-java-sdk", v("aws-sdk")),
    "base64"                      -> Dep("com.github.marklister", "base64", v("base64")),
    "catalysts-checklite"         -> Dep("org.typelevel", "catalysts-checklite", v("catalysts")),
    "catalysts-lawkit"            -> Dep("org.typelevel", "catalysts-lawkit", v("catalysts")),
    "catalysts-macros"            -> Dep("org.typelevel", "catalysts-macros", v("catalysts")),
    "catalysts-platform"          -> Dep("org.typelevel", "catalysts-platform", v("catalysts")),
    "catalysts-scalatest"         -> Dep("org.typelevel", "catalysts-scalatest", v("catalysts")),
    "catalysts-specbase"          -> Dep("org.typelevel", "catalysts-specbase", v("catalysts")),
    "catalysts-speclite"          -> Dep("org.typelevel", "catalysts-speclite", v("catalysts")),
    "catalysts-specs2"            -> Dep("org.typelevel", "catalysts-specs2", v("catalysts")),
    "catalysts-testkit"           -> Dep("org.typelevel", "catalysts-testkit", v("catalysts")),
    "catalysts"                   -> Dep("org.typelevel", "catalysts", v("catalysts")),
    "cats-core"                   -> Dep("org.typelevel", "cats-core", v("cats")),
    "cats-free"                   -> Dep("org.typelevel", "cats-free", v("cats")),
    "cats-kernel"                 -> Dep("org.typelevel", "cats-kernel", v("cats")),
    "cats-laws"                   -> Dep("org.typelevel", "cats-laws", v("cats")),
    "cats-macros"                 -> Dep("org.typelevel", "cats-macros", v("cats")),
    "cats-state"                  -> Dep("org.typelevel", "cats-state", v("cats")),
    "cats"                        -> Dep("org.typelevel", "cats", v("cats")),
    "circe-core"                  -> Dep("io.circe", "circe-core", v("circe")),
    "circe-generic"               -> Dep("io.circe", "circe-generic", v("circe")),
    "circe-parser"                -> Dep("io.circe", "circe-parser", v("circe")),
    "config"                      -> Dep("com.typesafe", "config", v("config")),
    "coursier-cache"              -> Dep("io.get-coursier", "coursier-cache", v("coursier")),
    "coursier"                    -> Dep("io.get-coursier", "coursier", v("coursier")),
    "discipline"                  -> Dep("org.typelevel", "discipline", v("discipline")),
    "doobie-hikari"               -> Dep("org.tpolecat", "doobie-contrib-hikari", v("doobie")),
    "doobie-postgresql"           -> Dep("org.tpolecat", "doobie-contrib-postgresql", v("doobie")),
    "doobie-specs2"               -> Dep("org.tpolecat", "doobie-contrib-specs2", v("doobie")),
    "doobie"                      -> Dep("org.tpolecat", "doobie-core", v("doobie")),
    "doobie-core-cats"            -> Dep("org.tpolecat", "doobie-core-cats", v("doobie")),
    "doobie-h2-cats"              -> Dep("org.tpolecat", "doobie-h2-cats", v("doobie")),
    "embedded-redis"              -> Dep("com.orange.redis-embedded", "embedded-redis", v("embedded-redis")),
    "evaluator-client"            -> Dep("org.scala-exercises", "evaluator-client", v("scala-exercises")),
    "evaluator-shared"            -> Dep("org.scala-exercises", "evaluator-shared", v("scala-exercises")),
    "export-hook"                 -> Dep("org.typelevel", "export-hook", v("export-hook")),
    "fs2-core"                    -> Dep("co.fs2", "fs2-core", v("fs2")),
    "fs2-io"                      -> Dep("co.fs2", "fs2-io", v("fs2")),
    "fs2-cats"                    -> Dep("co.fs2", "fs2-cats", v("fs2-cats")),
    "fetch"                       -> Dep("com.47deg", "fetch", v("fetch")),
    "fetch-monix"                 -> Dep("com.47deg", "fetch-monix", v("fetch")),
    "fetch-debug"                 -> Dep("com.47deg", "fetch-debug", v("fetch")),
    "github4s"                    -> Dep("com.47deg", "github4s", v("github4s")),
    "http4s-blaze-client"         -> Dep("org.http4s", "http4s-blaze-client", v("http4s")),
    "http4s-blaze-server"         -> Dep("org.http4s", "http4s-blaze-server", v("http4s")),
    "http4s-client"               -> Dep("org.http4s", "http4s-circe", v("http4s")),
    "http4s-core"                 -> Dep("org.http4s", "http4s-core", v("http4s")),
    "http4s-dsl"                  -> Dep("org.http4s", "http4s-dsl", v("http4s")),
    "journal-core"                -> Dep("io.verizon.journal", "core", v("journal")),
    "kind-projector"              -> Dep("org.spire-math", "kind-projector", v("kind-projector")),
    "log4s"                       -> Dep("org.log4s", "log4s", v("log4s")),
    "machinist"                   -> Dep("org.typelevel", "machinist", v("machinist")),
    "macro-compat"                -> Dep("org.typelevel", "macro-compat", v("macro-compat")),
    "monix"                       -> Dep("io.monix", "monix", v("monix")),
    "monix-cats"                  -> Dep("io.monix", "monix-cats", v("monix")),
    "monix-eval"                  -> Dep("io.monix", "monix-eval", v("monix")),
    "monocle-core"                -> Dep("com.github.julien-truffaut", "monocle-core", v("monocle")),
    "monocle-generic"             -> Dep("com.github.julien-truffaut", "monocle-generic", v("monocle")),
    "monocle-law"                 -> Dep("com.github.julien-truffaut", "monocle-law", v("monocle")),
    "monocle-macro"               -> Dep("com.github.julien-truffaut", "monocle-macro", v("monocle")),
    "monocle-state"               -> Dep("com.github.julien-truffaut", "monocle-state", v("monocle")),
    "newrelic"                    -> Dep("com.newrelic.agent.java", "newrelic-agent", v("newrelic")),
    "paradise"                    -> Dep("org.scalamacros", "paradise", v("paradise")),
    "pcplod"                      -> Dep("org.ensime", "pcplod", v("pcplod")),
    "pure-config"                 -> Dep("com.github.melrief", "pureconfig", v("pure-config")),
    "quasiquotes"                 -> Dep("org.scalamacros", "quasiquotes", v("paradise")),
    "rediscala"                   -> Dep("com.github.etaty", "rediscala", v("rediscala")),
    "refined-scalacheck"          -> Dep("eu.timepit", "refined-scalacheck", v("refined")),
    "refined-scalaz"              -> Dep("eu.timepit", "refined-scalaz", v("refined")),
    "refined-scodec"              -> Dep("eu.timepit", "refined-scodec", v("refined")),
    "refined"                     -> Dep("eu.timepit", "refined", v("refined")),
    "roshttp"                     -> Dep("fr.hmil", "roshttp", v("roshttp")),
    "scala-exercises-compiler"    -> Dep("org.scala-exercises", "exercise-compiler", v("scala-exercises")),
    "scala-exercises-definitions" -> Dep("org.scala-exercises", "definitions", v("scala-exercises")),
    "scala-exercises-runtime"     -> Dep("org.scala-exercises", "runtime", v("scala-exercises")),
    "scala-reflect"               -> Dep("org.scala-lang", "scala-reflect", v("scala-reflect")),
    "scalacheck"                  -> Dep("org.scalacheck", "scalacheck", v("scalacheck")),
    "scheckShapeless"             -> Dep("com.github.alexarchambault", "scalacheck-shapeless_1.13", v("scheckShapeless")),
    "scalaj"                      -> Dep("org.scalaj", "scalaj-http", v("scalaj")),
    "scalatest"                   -> Dep("org.scalatest", "scalatest", v("scalatest")),
    "scalaz-concurrent"           -> Dep("org.scalaz", "scalaz-concurrent", v("scalaz")),
    "scalazspecs2"                -> Dep("org.typelevel", "scalaz-specs2", v("scalazspecs2")),
    "scanamo"                     -> Dep("com.gu", "scanamo", v("scanamo")),
    "shapeless"                   -> Dep("com.chuusai", "shapeless", v("shapeless")),
    "shocon"                      -> Dep("eu.unicredit", "shocon", v("shocon")),
    "simulacrum"                  -> Dep("com.github.mpilquist", "simulacrum", v("simulacrum")),
    "slf4j-nop"                   -> Dep("org.slf4j", "slf4j-nop", v("slf4j")),
    "slf4j-simple"                -> Dep("org.slf4j", "slf4j-simple", v("slf4j")),
    "slogging"                    -> Dep("biz.enef", "slogging", v("slogging")),
    "specs2-core"                 -> Dep("org.specs2", "specs2-core", v("specs2")),
    "specs2-scalacheck"           -> Dep("org.specs2", "specs2-scalacheck", v("specs2"))
  )
}
