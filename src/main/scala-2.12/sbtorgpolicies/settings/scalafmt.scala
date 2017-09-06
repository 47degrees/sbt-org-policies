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

// import org.scalafmt.bootstrap.ScalafmtBootstrap
import sbt.Keys._
import sbt._
import sbt.util.CacheStoreFactory
import sbt.internal.inc.Analysis
import sbtorgpolicies.OrgPoliciesKeys._

/**
 * https://gist.github.com/olafurpg/e045ef9d8a4273bae3e2ccf610636d66#file-automatescalafmtplugin-scala
 */
trait scalafmt {

  def orgAutomateScalafmtFor(configurations: Configuration*): Seq[Setting[_]] =
    configurations.flatMap { c =>
      inConfig(c)(
        Seq(
          orgScalafmtInc := {
            orgScalafmtIncDef.value
            (): Unit
          },
          sourceDirectories.in(orgScalafmtInc) := Seq(scalaSource.value),
          compileInputs.in(compile) := (compileInputs.in(compile) dependsOn orgScalafmtInc).value
        )
      )
    }

  val orgScalafmtIncDef: Def.Initialize[Task[Set[File]]] = Def.task {
    val cache: CacheStoreFactory = CacheStoreFactory(streams.value.cacheDirectory / "scalafmt")
    val include: FileFilter      = includeFilter.in(orgScalafmtInc).value
    val exclude: FileFilter      = excludeFilter.in(orgScalafmtInc).value
    val sources: Set[File] =
      sourceDirectories
        .in(orgScalafmtInc)
        .value
        .descendantsExcept(include, exclude)
        .get
        .toSet

    def format(handler: Set[File] => Unit, msg: String): Set[File] = {
      def update(handler: Set[File] => Unit, msg: String)(
          in: ChangeReport[File],
          out: ChangeReport[File]): Set[File] = {
        val label: String    = Reference.display(thisProjectRef.value)
        val files: Set[File] = in.modified -- in.removed

        lazy val log: Logger = streams.value.log

        Analysis
          .counted("Scala source", "", "s", files.size)
          .foreach(count => log.info(s"$msg $count in $label ..."))
        handler(files)
        files
      }

      FileFunction.cached(cache, FilesInfo.hash, FilesInfo.exists)(update(handler, msg))(
        sources
      )
    }

    def formattingHandler(files: Set[File]) =
      if (files.nonEmpty) {
        val filesArg = files.map(_.getAbsolutePath).mkString(",")
        org.scalafmt.cli.Cli.main(Array("--quiet", "--non-interactive", "--files", filesArg))
      }

    format(formattingHandler, "Formatting")
    format(_ => (), "Reformatted") // Recalculate the cache
  }

}
