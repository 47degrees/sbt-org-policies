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
      .catchNonFatal(path.toFile.exists()) getOrElse false

  def withFileContent[T](filePath: String, f: String => IOResult[T]): IOResult[T] =
    getFileContent(filePath) flatMap f

  def getFileContent(filePath: String): IOResult[String] =
    Either
      .catchNonFatal(IO.readLines(file(filePath)).mkString)
      .leftMap(e => IOException(s"Error loading $filePath content", Some(e)))

  def fetchFilesRecursivelyFromPath(sourcePath: String, acceptedExtensions: List[String] = Nil): IOResult[List[File]] =
    fetchFilesRecursively(sourcePath.toFile, acceptedExtensions)

  def fetchFilesRecursively(sourceFile: File, acceptedExtensions: List[String] = Nil): IOResult[List[File]] =
    Either
      .catchNonFatal {

        @tailrec
        def innerRecursive(files: List[File], accum: List[File]): List[File] = {

          val acceptedFiles    = files filter acceptedExtension
          val filesDirectories = files filter (_.isDirectory) flatMap (_.listFiles().toList)

          val newAccum = accum ++ acceptedFiles

          filesDirectories match {
            case Nil => newAccum
            case _   => innerRecursive(filesDirectories, newAccum)
          }
        }

        def acceptedExtension(file: File): Boolean =
          file.isFile &&
            (acceptedExtensions.isEmpty ||
              acceptedExtensions.foldLeft(false) { (b, ext) =>
                b || file.getName.endsWith(ext)
              })

        (sourceFile.exists(), sourceFile.isFile, acceptedExtension(sourceFile)) match {
          case (false, _, _) =>
            Nil
          case (_, true, false) =>
            Nil
          case (_, true, true) =>
            List(sourceFile)
          case _ =>
            innerRecursive(Option(sourceFile.listFiles.toList).toList.flatten, Nil)
        }
      }
      .leftMap(e => IOException(s"Error fetching files recursively from ${sourceFile.getAbsolutePath}", Some(e)))
}
