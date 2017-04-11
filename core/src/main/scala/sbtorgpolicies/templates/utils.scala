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

object utils {

  def str(s: String, scape: Boolean): String = if (scape) s"\\$s" else s

  def sectionSep(title: String, start: Boolean = true, scape: Boolean = false): String =
    (if (start) "\n" else "") +
      str("[", scape) + "comment" + str("]", scape) +
      ": " +
      str("#", scape) + " " +
      str("(", scape) + (if (start) "Start" else "End") + " " + title + str(")", scape)

  def replaceSection(title: String, top: Boolean): ReplaceSection =
    ReplaceSection(
      from = sectionSep(title, scape = true).r,
      to = sectionSep(title, start = false, scape = true).r,
      defaultTop = top)

}
