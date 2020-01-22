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

import org.joda.time.DateTime
import sbtorgpolicies.rules.ValidationFunction
import sbtorgpolicies.templates.syntax._

import scala.util.matching.Regex

trait Replaceable {

  def asString: String
}

case class ReplaceableT[T](t: T) extends Replaceable {
  override def asString: String = t.toString
}

case class ReplaceableList[T](list: List[T]) extends Replaceable {
  override def asString: String =
    list.map(elem => s"* ${elem.asReplaceable.asString}").mkString("\n")
}

case class FileType(mandatory: Boolean,
                    overWritable: Boolean,
                    finalVersionOnly: Boolean,
                    templatePath: String,
                    outputPath: String,
                    replacements: Replacements,
                    fileSections: List[FileSection] = Nil,
                    validations: List[ValidationFunction] = Nil)

case class FileSection(appendPosition: AppendPosition,
                       template: String,
                       replacements: Replacements,
                       shouldAppend: (String) => Boolean = _ => true)

sealed trait AppendPosition
case object AppendAtTheBeginning    extends AppendPosition
case object AppendAtTheEnd          extends AppendPosition
case class AppendAfter(line: Regex) extends AppendPosition
case class ReplaceSection(from: Regex,
                          to: Regex,
                          insertIfNotFound: Boolean = true,
                          defaultTop: Boolean = true)
    extends AppendPosition

case class NewReleaseSection(date: DateTime, version: String, changes: String)
