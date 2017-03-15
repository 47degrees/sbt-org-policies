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

package sbtorgpolicies.io

import cats.syntax.either._
import sbt.{file, IO}
import sbtorgpolicies.exceptions._

import scala.util.Try

class FileWriter {

  def writeContentToFile(content: String, outputPath: String): IOResult[Unit] =
    createFile(outputPath) match {
      case Right(b) if b =>
        Either
          .fromTry {
            Try(IO.write(file(outputPath), content))
          }
          .leftMap(e => IOException(s"Error writing to file $outputPath", Some(e)))
      case _ =>
        IOException(s"Unexpected error writing to file $outputPath").asLeft
    }

  def createFile(output: String): IOResult[Boolean] =
    Either
      .catchNonFatal {
        val f = file(output)

        if (!f.exists()) {
          if (!f.getParentFile.exists())
            f.getParentFile.mkdirs()
          f.createNewFile()
        } else true
      }
      .leftMap(e => IOException(s"Error creating file", Some(e)))

}
