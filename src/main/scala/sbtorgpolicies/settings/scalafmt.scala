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

package sbtorgpolicies.settings

import org.scalafmt.bootstrap.ScalafmtBootstrap
import sbt.Keys._
import sbt._
import sbt.inc.Analysis

/**
 * https://gist.github.com/olafurpg/e045ef9d8a4273bae3e2ccf610636d66#file-automatescalafmtplugin-scala
 */
trait scalafmt {

  val scalafmtInc: TaskKey[Unit] = taskKey[Unit]("Incrementally format modified sources")

  val scalafmtGenerateFile: TaskKey[Unit] = taskKey[Unit]("Generate a default scalafmt configuration")

  lazy val generateScalafmtTask = Seq(
    scalafmtGenerateFile := Def.task {
      if (!file(".scalafmt.conf").exists()) {
        IO.write(
          file(".scalafmt.conf"),
          """style = defaultWithAlign
            |maxColumn = 100
            |
            |continuationIndent.callSite = 2
            |
            |newlines {
            |  sometimesBeforeColonInMethodReturnType = false
            |}
            |
            |align {
            |  arrowEnumeratorGenerator = false
            |  ifWhileOpenParen = false
            |  openParenCallSite = false
            |  openParenDefnSite = false
            |}
            |
            |docstrings = JavaDoc
            |
            |rewrite {
            |  rules = [SortImports, RedundantBraces]
            |  redundantBraces.maxLines = 1
            |}
        """.stripMargin.getBytes(IO.utf8)
        )
      }
    }
  )

  def automateScalafmtFor(configurations: Configuration*): Seq[Setting[_]] =
    configurations.flatMap { c =>
      inConfig(c)(
        Seq(
          compileInputs.in(compile) := {
            scalafmtInc.value
            compileInputs.in(compile).value
          },
          sourceDirectories.in(scalafmtInc) := Seq(scalaSource.value),
          scalafmtInc := {
            val cache   = streams.value.cacheDirectory / "scalafmt"
            val include = includeFilter.in(scalafmtInc).value
            val exclude = excludeFilter.in(scalafmtInc).value
            val sources =
              sourceDirectories
                .in(scalafmtInc)
                .value
                .descendantsExcept(include, exclude)
                .get
                .toSet

            def format(handler: Set[File] => Unit, msg: String) = {
              def update(handler: Set[File] => Unit, msg: String)(in: ChangeReport[File], out: ChangeReport[File]) = {
                val label = Reference.display(thisProjectRef.value)
                val files = in.modified -- in.removed
                Analysis
                  .counted("Scala source", "", "s", files.size)
                  .foreach(count => streams.value.log.info(s"$msg $count in $label ..."))
                handler(files)
                files
              }

              FileFunction.cached(cache)(FilesInfo.hash, FilesInfo.exists)(update(handler, msg))(
                sources
              )
            }

            def formattingHandler(files: Set[File]) =
              if (files.nonEmpty) {
                val filesArg = files.map(_.getAbsolutePath).mkString(",")
                ScalafmtBootstrap.main(List("--quiet", "-i", "-f", filesArg))
              }

            format(formattingHandler, "Formatting")
            format(_ => (), "Reformatted") // Recalculate the cache
          }
        )
      )
    }

}