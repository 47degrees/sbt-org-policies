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

import java.io._
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths.get

import cats.syntax.either._
import sbtorgpolicies.exceptions.IOException

import scala.io.Source
import scala.language.implicitConversions

package object io {

  type IOResult[T] = Either[IOException, T]

  object syntax {

    implicit def eitherFilterSyntax[T](either: Either[Throwable, T]): FilteredEitherOps[T] =
      new FilteredEitherOps(either)

    implicit def fileNameSyntax(fileName: String): FileNameOps = new FileNameOps(fileName)

    final class FilteredEitherOps[T](either: Either[Throwable, T]) {

      def withFilter(f: T => Boolean): Either[Throwable, T] = either match {
        case Right(r) if !f(r) =>
          new IllegalStateException("Filter condition has not been satisfied").asLeft[T]
        case _ =>
          either
      }
    }

    final class FileNameOps(filename: String) {

      def toPath: Path = get(filename)

      def toFile: File = new File(filename.fixPath)

      def fixPath: String = filename.replaceAll("/", File.separator)

      def ensureFinalSlash: String =
        filename +
          (if (filename.endsWith(File.separator)) ""
           else File.separator)
    }
  }

  object IO {

    def file(path: String): File = new File(path)

    def url(address: String): URL = new URL(address)

    def readLines(file: File): Iterator[String] =
      Source.fromFile(file).getLines()

    def readBytes(file: File): Array[Byte] = {
      val is: InputStream    = new FileInputStream(file)
      val array: Array[Byte] = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
      is.close()
      array
    }

    def write(file: File, content: String, charset: Charset = Charset.forName("UTF-8")): Unit = {
      val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), charset))
      writer.write(content)
      writer.close()
    }

    def relativize(base: File, file: File): Option[String] = {

      def ensureEndingSlash: Option[String] = {
        val path = base.getAbsolutePath
        path.lastOption.map {
          case c if c == File.separatorChar => path
          case _                            => path + File.separatorChar
        }
      }

      val baseFileString = if (base.isDirectory) ensureEndingSlash else None
      val pathString     = file.getAbsolutePath
      baseFileString flatMap {
        case baseString if pathString.startsWith(baseString) =>
          Some(pathString.substring(baseString.length))
        case _ => None
      }
    }

  }
}
