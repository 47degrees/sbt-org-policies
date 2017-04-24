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

import java.io.File

import cats.syntax.either._
import sbt.{file, IO}
import sbtorgpolicies.exceptions._
import sbtorgpolicies.io.syntax._

import scala.annotation.tailrec

class FileReader {

  def exists(path: String): Boolean =
    Either
      .catchNonFatal(file(path).exists()) getOrElse false

  def withFileContent[T](filePath: String, f: String => IOResult[T]): IOResult[T] =
    getFileContent(filePath) flatMap f

  def getFileContent(filePath: String): IOResult[String] =
    Either
      .catchNonFatal(IO.readLines(file(filePath)).mkString("\n"))
      .leftMap(e => IOException(s"Error loading $filePath content", Some(e)))

  def getFileBytes(file: File): IOResult[Array[Byte]] =
    Either
      .catchNonFatal(IO.readBytes(file))
      .leftMap(e => IOException(s"Error loading ${file.getAbsolutePath} content", Some(e)))

  private[this] val defaultValidDirs: (File) => Boolean = (f: File) => {
    !Set("target", "bin", "output").contains(f.getName) && !f.getName.startsWith(".")
  }

  def fetchFilesRecursivelyFromPath(
      sourcePath: String,
      isFileSupported: (File) => Boolean = _ => true,
      isDirSupported: (File) => Boolean = defaultValidDirs): IOResult[List[File]] =
    fetchFilesRecursively(List(sourcePath.toFile))

  def fetchFilesRecursively(
      in: List[File],
      isFileSupported: (File) => Boolean = _ => true,
      isDirSupported: (File) => Boolean = defaultValidDirs): IOResult[List[File]] =
    Either
      .catchNonFatal {
        @tailrec
        def findAllFiles(
            in: List[File],
            isFileSupported: (File) => Boolean,
            isDirSupported: (File) => Boolean,
            processedFiles: List[File] = Nil,
            processedDirs: List[String] = Nil): List[File] = {

          val allFiles: List[File] = processedFiles ++ in.filter(f => f.exists && f.isFile && isFileSupported(f))

          in.filter { f =>
            f.isDirectory &&
            isDirSupported(f) &&
            !processedDirs.contains(f.getCanonicalPath)
          } match {
            case Nil => allFiles
            case list =>
              val subFiles = list.flatMap(_.listFiles().toList)
              findAllFiles(
                subFiles,
                isFileSupported,
                isDirSupported,
                allFiles,
                processedDirs ++ list.map(_.getCanonicalPath))
          }
        }

        findAllFiles(in, isFileSupported, isDirSupported)
      }
      .leftMap(e => IOException(s"Error fetching files recursively", Some(e)))
}
