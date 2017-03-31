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

package sbtorgpolicies.templates

import cats.syntax.either._
import sbtorgpolicies.io._
import sbtorgpolicies.exceptions._

import scala.util.matching.Regex

class TemplatesEngine {

  val fileReader: FileReader = new FileReader

  val fileWriter: FileWriter = new FileWriter

  def run(inputPath: String, outputPath: String, replacements: Replacements): IOResult[Unit] =
    for {
      content <- replaceFileWith(inputPath, replacements)
      _       <- fileWriter.writeContentToFile(content, outputPath)
    } yield ()

  def runInsert(
      inputPath: String,
      outputPath: String,
      regexpLine: Regex,
      template: String,
      replacements: Replacements): IOResult[Unit] =
    for {
      fileContent     <- readFileOr(outputPath, inputPath)
      replacedContent <- insertAfter(fileContent, regexpLine, template, replacements)
      _               <- fileWriter.writeContentToFile(replacedContent, outputPath)
    } yield ()

  def replaceFileWith(inputPath: String, replacements: Replacements): IOResult[String] =
    fileReader.withFileContent(inputPath, replaceWith(_, replacements))

  def replaceWith(content: String, replacements: Replacements): IOResult[String] =
    Either
      .catchNonFatal {
        replacements.foldLeft(content) {
          case (str, (key, replaceable)) =>
            replacementPattern(key).replaceAllIn(str, replaceable.asString)
        }
      }
      .leftMap(e => IOException(s"Error replacing content", Some(e)))

  def readFileOr(inputPath: String, templatePath: String): IOResult[String] =
    fileReader.getFileContent(inputPath) match {
      case Right(c) => Right(c)
      case Left(_)  => fileReader.getFileContent(templatePath)
    }

  def insertAfter(content: String, regexpLine: Regex, template: String, replacements: Replacements): IOResult[String] =
    Either
      .catchNonFatal {
        val replaced = replacements.foldLeft(template) {
          case (str, (key, replaceable)) =>
            replacementPattern(key).replaceAllIn(str, replaceable.asString)
        }

        regexpLine.replaceFirstIn(content, "$0\n" + replaced)
      }
      .leftMap(e => IOException(s"Error inserting content", Some(e)))

  private[this] def replacementPattern(key: String): Regex = s"\\{\\{$key\\}\\}".r
}
