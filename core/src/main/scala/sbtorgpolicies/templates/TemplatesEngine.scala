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

  def replaceFileContentsWith(inputPath: String, replacements: Replacements): IOResult[String] =
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

  def insertIn(content: String, appendPosition: AppendPosition, section: String): IOResult[String] =
    Either
      .catchNonFatal {
        appendPosition match {
          case AppendAtTheBeginning => section + "\n" + content
          case AppendAtTheEnd       => content + "\n" + section
          case AppendAfter(regex)   => regex.replaceFirstIn(content, "$0\n" + section)
        }
      }
      .leftMap(e => IOException(s"Error inserting content", Some(e)))

  private[this] def replacementPattern(key: String): Regex = s"\\{\\{$key\\}\\}".r
}
