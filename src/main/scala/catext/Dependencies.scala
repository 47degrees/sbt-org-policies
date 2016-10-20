/*
 * Copyright 2016 47 Degrees, LLC. <http://www.47deg.com>
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

package catext

import sbt._
import sbtcatalysts.CatalystsPlugin.autoImport._

object Dependencies {

  import org.typelevel.Dependencies.{
    libraries => tllibraries,
    scalacPlugins => tlscalacPlugins,
    macroCompatSettings => tlmacroCompatSettings
  }

  // Versions is being instantiated to have the control. It's based on
  // https://github.com/typelevel/sbt-catalysts/blob/master/src/main/scala/org/typelevel/TypelevelDeps.scala#L15:
  val versions = Map[String, String](
    "algebra"             -> "0.5.1",
    "alleycats"           -> "0.1.7",
    "catalysts"           -> "0.1.0",
    "cats"                -> "0.7.2",
    "circe"               -> "0.5.4",
    "config"              -> "1.3.0",
    "coursier"            -> "1.0.0-M14-2",
    "discipline"          -> "0.5",
    "doobie"              -> "0.3.0",
    "evaluator"           -> "0.1.1-SNAPSHOT",
    "export-hook"         -> "1.1.0",
    "kind-projector"      -> "0.9.0",
    "github4s"            -> "0.8.0-SNAPSHOT",
    "http4s"              -> "0.14.10a",
    "log4s"               -> "1.3.0",
    "machinist"           -> "0.4.1",
    "macro-compat"        -> "1.1.1",
    "monix"               -> "2.0.3",
    "monocle"             -> "1.2.0",
    "newrelic"            -> "3.29.0",
    "paradise"            -> "2.1.0",
    "refined"             -> "0.3.6",
    "roshttp"             -> "1.1.0",
    "scalacheck"          -> "1.12.5",
    "scalacheckshapeless" -> "0.3.1",
    "scalac"              -> "2.11.8",
    "scalac_2.11"         -> "2.11.8",
    "scalac_2.10"         -> "2.10.6",
    "scala-exercises"     -> "0.3.0-SNAPSHOT",
    "scalatest"           -> "2.2.6",
    "scalaz"              -> "7.2.4",
    "scalazspecs2"        -> "0.4.0",
    "shapeless"           -> "2.3.0",
    "simulacrum"          -> "0.8.0",
    "slf4j"               -> "1.7.21",
    "specs2"              -> "3.6.4"
  )

  /**
    * Used same format as sbt-catalyst:
    *
    *  Library name -> version key, org, library
    */
  val libraries = tllibraries ++ Map[String, (String, String, String)](
      "circe-core"                  -> ("circe", "io.circe", "circe-core"),
      "circe-generic"               -> ("circe", "io.circe", "circe-generic"),
      "circe-parser"                -> ("circe", "io.circe", "circe-parser"),
      "config"                      -> ("config", "com.typesafe", "config"),
      "coursier"                    -> ("coursier", "io.get-coursier", "coursier"),
      "coursier-cache"              -> ("coursier", "io.get-coursier", "coursier-cache"),
      "doobie"                      -> ("doobie", "org.tpolecat", "doobie-core"),
      "doobie-hikari"               -> ("doobie", "org.tpolecat", "doobie-contrib-hikari"),
      "doobie-postgresql"           -> ("doobie", "org.tpolecat", "doobie-contrib-postgresql"),
      "doobie-specs2"               -> ("doobie", "org.tpolecat", "doobie-contrib-specs2"),
      "evaluator-client"            -> ("scala-exercises", "org.scala-exercises", "evaluator-client"),
      "evaluator-shared"            -> ("scala-exercises", "org.scala-exercises", "evaluator-shared"),
      "github4s"                    -> ("github4s", "com.fortysevendeg", "github4s"),
      "http4s-dsl"                  -> ("http4s", "org.http4s", "http4s-dsl"),
      "http4s-blaze-server"         -> ("http4s", "org.http4s", "http4s-blaze-server"),
      "http4s-blaze-client"         -> ("http4s", "org.http4s", "http4s-blaze-client"),
      "http4s-client"               -> ("http4s", "org.http4s", "http4s-circe"),
      "log4s"                       -> ("log4s", "org.log4s", "log4s"),
      "monix"                       -> ("monix", "io.monix", "monix"),
      "newrelic"                    -> ("newrelic", "com.newrelic.agent.java", "newrelic-agent"),
      "roshttp"                     -> ("roshttp", "fr.hmil", "roshttp"),
      "scalacheckshapeless"         -> ("scalacheckshapeless", "com.github.alexarchambault", "scalacheck-shapeless_1.12"),
      "scalaz-concurrent"           -> ("scalaz", "org.scalaz", "scalaz-concurrent"),
      "scalazspecs2"                -> ("scalazspecs2", "org.typelevel", "scalaz-specs2"),
      "scala-exercises-compiler"    -> ("scala-exercises", "org.scala-exercises", "exercise-compiler"),
      "scala-exercises-definitions" -> ("scala-exercises", "org.scala-exercises", "definitions"),
      "scala-exercises-runtime"     -> ("scala-exercises", "org.scala-exercises", "runtime"),
      "slf4j-nop"                   -> ("slf4j", "org.slf4j", "slf4j-nop"),
      "slf4j-simple"                -> ("slf4j", "org.slf4j", "slf4j-simple")
    )

  val scalacPlugins = tlscalacPlugins

  def macroCompatSettings(v: Versions): Seq[Setting[_]] = tlmacroCompatSettings(v)

}
