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
import catext.CatExtPlugin.autoImport._

object Dependencies {

  /**
    * Versions is being instantiated to have the control.
    * Original forked SBT Plugin: https://github.com/typelevel/sbt-catalysts/blob/69d8bb150ac6b3d79721d26c07326398ef5aae94/src/main/scala/org/typelevel/TypelevelDeps.scala
    */
  val versions = Map[String, String](
    "algebra"             -> "0.5.1",
    "alleycats"           -> "0.1.7",
    "aws-sdk"             -> "1.11.53",
    "catalysts"           -> "0.1.0",
    "cats"                -> "0.8.1",
    "circe"               -> "0.6.0",
    "config"              -> "1.3.0",
    "coursier"            -> "1.0.0-M14-2",
    "discipline"          -> "0.5",
    "doobie"              -> "0.3.1-M2",
    "evaluator"           -> "0.1.1-SNAPSHOT",
    "export-hook"         -> "1.1.0",
    "fetch"               -> "0.4.0",
    "github4s"            -> "0.9.0",
    "http4s"              -> "0.14.10a",
    "kind-projector"      -> "0.9.3",
    "log4s"               -> "1.3.0",
    "machinist"           -> "0.4.1",
    "macro-compat"        -> "1.1.1",
    "monix"               -> "2.1.0",
    "monocle"             -> "1.3.2",
    "newrelic"            -> "3.29.0",
    "paradise"            -> "2.1.0",
    "pure-config"         -> "0.3.3",
    "refined"             -> "0.3.6",
    "roshttp"             -> "2.0.0-RC1",
    "scala-exercises"     -> "0.3.0-SNAPSHOT",
    "scala-reflect"       -> "2.11.8",
    "scalac_2.10"         -> "2.10.6",
    "scalac_2.11"         -> "2.11.8",
    "scalac_2.12"         -> "2.12.0",
    "scalac"              -> "2.11.8",
    "scalacheck"          -> "1.13.4",
    "scalacheckshapeless" -> "1.1.3",
    "scalaj"              -> "2.3.0",
    "scalatest"           -> "3.0.1",
    "scalaz"              -> "7.2.4",
    "scalazspecs2"        -> "0.4.0",
    "scanamo"             -> "0.8.1",
    "shapeless"           -> "2.3.2",
    "simulacrum"          -> "0.10.0",
    "slf4j"               -> "1.7.21",
    "specs2"              -> "3.8.6"
  )

  /**
    * Used same format as sbt-catalyst:
    *
    *  Library name -> version key, org, library
    */
  val libraries = Map[String, (String, String, String)](
    "algebra-laws"                -> ("algebra", "org.typelevel", "algebra-laws"),
    "algebra"                     -> ("algebra", "org.typelevel", "algebra"),
    "alleycats"                   -> ("alleycats", "org.typelevel", "alleycats"),
    "aws-sdk"                     -> ("aws-sdk", "com.amazonaws", "aws-java-sdk"),
    "catalysts-checklite"         -> ("catalysts", "org.typelevel", "catalysts-checklite"),
    "catalysts-lawkit"            -> ("catalysts", "org.typelevel", "catalysts-lawkit"),
    "catalysts-macros"            -> ("catalysts", "org.typelevel", "catalysts-macros"),
    "catalysts-platform"          -> ("catalysts", "org.typelevel", "catalysts-platform"),
    "catalysts-scalatest"         -> ("catalysts", "org.typelevel", "catalysts-scalatest"),
    "catalysts-specbase"          -> ("catalysts", "org.typelevel", "catalysts-specbase"),
    "catalysts-speclite"          -> ("catalysts", "org.typelevel", "catalysts-speclite"),
    "catalysts-specs2"            -> ("catalysts", "org.typelevel", "catalysts-specs2"),
    "catalysts-testkit"           -> ("catalysts", "org.typelevel", "catalysts-testkit"),
    "catalysts"                   -> ("catalysts", "org.typelevel", "catalysts"),
    "cats-core"                   -> ("cats", "org.typelevel", "cats-core"),
    "cats-free"                   -> ("cats", "org.typelevel", "cats-free"),
    "cats-kernel"                 -> ("cats", "org.typelevel", "cats-kernel"),
    "cats-laws"                   -> ("cats", "org.typelevel", "cats-laws"),
    "cats-macros"                 -> ("cats", "org.typelevel", "cats-macros"),
    "cats-state"                  -> ("cats", "org.typelevel", "cats-state"),
    "cats"                        -> ("cats", "org.typelevel", "cats"),
    "circe-core"                  -> ("circe", "io.circe", "circe-core"),
    "circe-generic"               -> ("circe", "io.circe", "circe-generic"),
    "circe-parser"                -> ("circe", "io.circe", "circe-parser"),
    "config"                      -> ("config", "com.typesafe", "config"),
    "coursier-cache"              -> ("coursier", "io.get-coursier", "coursier-cache"),
    "coursier"                    -> ("coursier", "io.get-coursier", "coursier"),
    "discipline"                  -> ("discipline", "org.typelevel", "discipline"),
    "doobie-hikari"               -> ("doobie", "org.tpolecat", "doobie-contrib-hikari"),
    "doobie-postgresql"           -> ("doobie", "org.tpolecat", "doobie-contrib-postgresql"),
    "doobie-specs2"               -> ("doobie", "org.tpolecat", "doobie-contrib-specs2"),
    "doobie"                      -> ("doobie", "org.tpolecat", "doobie-core"),
    "evaluator-client"            -> ("scala-exercises", "org.scala-exercises", "evaluator-client"),
    "evaluator-shared"            -> ("scala-exercises", "org.scala-exercises", "evaluator-shared"),
    "export-hook"                 -> ("export-hook", "org.typelevel", "export-hook"),
    "fetch"                       -> ("fetch", "com.fortysevendeg", "fetch"),
    "github4s"                    -> ("github4s", "com.fortysevendeg", "github4s"),
    "http4s-blaze-client"         -> ("http4s", "org.http4s", "http4s-blaze-client"),
    "http4s-blaze-server"         -> ("http4s", "org.http4s", "http4s-blaze-server"),
    "http4s-client"               -> ("http4s", "org.http4s", "http4s-circe"),
    "http4s-dsl"                  -> ("http4s", "org.http4s", "http4s-dsl"),
    "log4s"                       -> ("log4s", "org.log4s", "log4s"),
    "machinist"                   -> ("machinist", "org.typelevel", "machinist"),
    "macro-compat"                -> ("macro-compat", "org.typelevel", "macro-compat"),
    "monix"                       -> ("monix", "io.monix", "monix"),
    "monocle-core"                -> ("monocle", "com.github.julien-truffaut", "monocle-core"),
    "monocle-generic"             -> ("monocle", "com.github.julien-truffaut", "monocle-generic"),
    "monocle-law"                 -> ("monocle", "com.github.julien-truffaut", "monocle-law"),
    "monocle-macro"               -> ("monocle", "com.github.julien-truffaut", "monocle-macro"),
    "monocle-state"               -> ("monocle", "com.github.julien-truffaut", "monocle-state"),
    "newrelic"                    -> ("newrelic", "com.newrelic.agent.java", "newrelic-agent"),
    "pure-config"                 -> ("pure-config", "com.github.melrief", "pureconfig"),
    "refined-scalacheck"          -> ("refined", "eu.timepit", "refined-scalacheck"),
    "refined-scalaz"              -> ("refined", "eu.timepit", "refined-scalaz"),
    "refined-scodec"              -> ("refined", "eu.timepit", "refined-scodec"),
    "refined"                     -> ("refined", "eu.timepit", "refined"),
    "roshttp"                     -> ("roshttp", "fr.hmil", "roshttp"),
    "scala-exercises-compiler"    -> ("scala-exercises", "org.scala-exercises", "exercise-compiler"),
    "scala-exercises-definitions" -> ("scala-exercises", "org.scala-exercises", "definitions"),
    "scala-exercises-runtime"     -> ("scala-exercises", "org.scala-exercises", "runtime"),
    "scala-reflect"               -> ("scala-reflect", "org.scala-lang", "scala-reflect"),
    "scalacheck"                  -> ("scalacheck", "org.scalacheck", "scalacheck"),
    "scalacheckshapeless"         -> ("scalacheckshapeless", "com.github.alexarchambault", "scalacheck-shapeless_1.13"),
    "scalaj"                      -> ("scalaj", "org.scalaj", "scalaj-http"),
    "scalatest"                   -> ("scalatest", "org.scalatest", "scalatest"),
    "scalaz-concurrent"           -> ("scalaz", "org.scalaz", "scalaz-concurrent"),
    "scalazspecs2"                -> ("scalazspecs2", "org.typelevel", "scalaz-specs2"),
    "scanamo"                     -> ("scanamo", "com.gu", "scanamo"),
    "shapeless"                   -> ("shapeless", "com.chuusai", "shapeless"),
    "simulacrum"                  -> ("simulacrum", "com.github.mpilquist", "simulacrum"),
    "slf4j-nop"                   -> ("slf4j", "org.slf4j", "slf4j-nop"),
    "slf4j-simple"                -> ("slf4j", "org.slf4j", "slf4j-simple"),
    "specs2-core"                 -> ("specs2", "org.specs2", "specs2-core"),
    "specs2-scalacheck"           -> ("specs2", "org.specs2", "specs2-scalacheck")
  )

  /**
    * Compiler plugins definitions and links to their versions
    *
    * Note that one version may apply to more than one plugin.
    *
    * Format: Library name -> version key, org, librar, crossVersion
    */
  val scalacPlugins = Map[String, (String, String, String, CrossVersion)](
    "kind-projector" -> ("kind-projector", "org.spire-math", "kind-projector", CrossVersion.binary),
    "paradise"       -> ("paradise", "org.scalamacros", "paradise", CrossVersion.full)
  )

  // Some helper methods to combine libraries
  /**
    * Sets all settings required for the macro-compat library.
    *
    * @param v Versions map to use
    * @return All settings required for the macro-compat library
    */
  def macroCompatSettings(v: Versions): Seq[Setting[_]] =
    addCompileLibs(v, "macro-compat") ++ addCompilerPlugins(v, "paradise") ++
      scalaMacroDependencies(v)
}
