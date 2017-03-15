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
import sbtorgpolicies.exceptions._

import scala.io.BufferedSource
import scala.util.Try

class FileReader {

  def withFileContent[T](filePath: String, f: String => T): IOResult[T] =
    getFileContent(filePath) match {
      case Right(content) => f(content).asRight
      case Left(e)        => e.asLeft
    }

  private[this] def getFileContent(filePath: String): IOResult[String] =
    Either
      .fromTry {
        Try {
          val source: BufferedSource = scala.io.Source.fromFile(filePath)
          val content: String        = source.mkString
          source.close()
          content
        }
      }
      .leftMap(e => IOException(s"Error loading $filePath content", Some(e)))

}
