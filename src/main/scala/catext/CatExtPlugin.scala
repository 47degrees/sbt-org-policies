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

import sbtcatalysts._
import sbt.Keys._
import sbt._
import scala.util.Try
import scala.collection.immutable.ListMap
import complete.DefaultParsers._
import com.typesafe.sbt.pgp.PgpKeys._

object CatExtPlugin extends AutoPlugin {

  object autoImport extends CatExtTasks with CatExtSettings

  import autoImport._

  override def requires = plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = tasksSettings
}

trait CatExtSettings {

  def guard[T](flag: Boolean)(res: Seq[T]): Seq[T] = if (flag) res else Seq.empty

  lazy val gpgFolder = sys.env.getOrElse("PGP_FOLDER", ".")

  lazy val pgpSettings = Seq(
    pgpPassphrase := Some(sys.env.getOrElse("PGP_PASSPHRASE", "").toCharArray),
    pgpPublicRing := file(s"$gpgFolder/pubring.gpg"),
    pgpSecretRing := file(s"$gpgFolder/secring.gpg")
  )

  lazy val miscSettings = Seq(
    shellPrompt := { s: State =>
      val c     = scala.Console
      val blue  = c.RESET + c.BLUE + c.BOLD
      val white = c.RESET + c.BOLD

      val projectName = Project.extract(s).currentProject.id

      s"$blue$projectName$white>${c.RESET}"
    }
  )
}

trait CatExtTasks extends CatExtKeys {

  import Dependencies._

  lazy val tasksSettings = Seq(
    allLibraries in ThisBuild := {
      ListMap(libraries.toSeq.sortBy(_._1): _*) foreach {
        case (k, (ver, org, art)) =>
          println(formatDep(k, org, art, ver))
      }
    },
    searchDep in ThisBuild := {
      val args: Seq[String] = spaceDelimited("<arg>").parsed

      args.headOption map { arg =>
        libraries.keySet.filter(_.contains(arg)) foreach {
          case k =>
            val Some((ver, org, art)) = libraries.get(k)
            println(formatDep(k, org, art, ver))
        }
      } getOrElse println("You have to specify the library string as a parameter")
    },
    allVersions in ThisBuild := {
      println(ListMap(versions.toSeq.sortBy(_._1): _*).mkString("\n"))
    },
    versionOf in ThisBuild := {
      val args: Seq[String] = spaceDelimited("<arg>").parsed

      val version = for {
        request <- args.headOption
        v       <- versions.get(request)
      } yield v

      version match {
        case Some(v) => println(s"${args.head} = $v")
        case _       => println(s"${args.head} not found")
      }
    }
  )

  private[this] def formatDep(k: String, org: String, art: String, ver: String): String =
    s"""$k -> "$org" %% "$art" % "${versions.getOrElse(ver, "<unknown>")}""""
}
