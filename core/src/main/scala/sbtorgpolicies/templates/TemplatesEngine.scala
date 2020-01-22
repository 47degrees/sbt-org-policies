/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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
import sbtorgpolicies.templates.utils._

import scala.util.matching.Regex

class TemplatesEngine(fileReader: FileReader = FileReader, fileWriter: FileWriter = FileWriter) {

  def replaceFileContentsWith(inputPath: String, replacements: Replacements): IOResult[String] =
    fileReader.withFileContent(inputPath, replaceWith(_, replacements))

  def replaceWith(content: String, replacements: Replacements): IOResult[String] =
    Either
      .catchNonFatal {
        replacements.foldLeft(content) {
          case (str, (key, replaceable)) =>
            replacementPattern(key).replaceAllIn(str, escapeGroupRef(replaceable.asString))
        }
      }
      .leftMap(e => IOException(s"Error replacing content", Some(e)))

  def insertIn(content: String,
               appendPosition: AppendPosition,
               section: String): IOResult[String] = {

    def insertBetween(content: String, from: Regex, to: Regex, section: String): Option[String] =
      for {
        startMatch <- from.findFirstMatchIn(content)
        endContent = safeSubStr(content, startMatch.end)
        endMatch <- to.findFirstMatchIn(endContent)
      } yield
        content.substring(0, startMatch.start) + section + safeSubStr(endContent, endMatch.end)

    def replaceSection(content: String,
                       from: Regex,
                       to: Regex,
                       section: String,
                       insertIfNotFound: Boolean,
                       defaultTop: Boolean): String =
      insertBetween(content, from, to, section) match {
        case Some(s)                             => s
        case _ if insertIfNotFound && defaultTop => section + "\n" + content
        case _ if insertIfNotFound               => content + "\n" + section
        case _                                   => content
      }

    Either
      .catchNonFatal {
        appendPosition match {
          case AppendAtTheBeginning => section + "\n" + content
          case AppendAtTheEnd       => content + "\n" + section
          case AppendAfter(regex)   => regex.replaceFirstIn(content, "$0\n" + section)
          case ReplaceSection(from, to, insert, top) =>
            replaceSection(content, from, to, section, insert, top)
        }
      }
      .leftMap(e => IOException(s"Error inserting content", Some(e)))
  }

  private[this] def replacementPattern(key: String): Regex = s"\\{\\{$key\\}\\}".r

  // The '$' char is interpreted by the `replaceAll` method as a substitution of a group
  // Since we're not using groups in the regex above, it'll fail. If we found a '$' we want literally that char
  private[this] def escapeGroupRef(str: String): String = {
    val first = str.headOption
      .map {
        case '$' => "\\$"
        case s   => s.toString
      }
      .getOrElse("")

    val tail = str.sliding(2, 1).map(_.splitAt(1)).map {
      case (s, "$") if s != "\\" => "\\$"
      case (_, s)                => s
    }

    first ++ tail.mkString("")
  }
}
