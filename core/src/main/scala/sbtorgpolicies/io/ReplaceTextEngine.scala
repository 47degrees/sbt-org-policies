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

package sbtorgpolicies.io

import java.io.File

import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.templates.utils._

import scala.annotation.tailrec
import scala.util.matching.Regex

class ReplaceTextEngine {

  import ReplaceTextEngine._
  import textSyntax._
  import FileReader._
  import FileWriter._

  final def replaceBlocks(
      startBlockRegex: Regex,
      endBlockRegex: Regex,
      replacements: Map[String, String],
      in: List[File],
      isFileSupported: (File) => Boolean
  ): IOResult[List[ProcessedFile]] =
    fetchFilesRecursively(in, isFileSupported) map { files =>
      files.map(replaceBlocksInFile(Some(startBlockRegex), Some(endBlockRegex), replacements, _))
    }

  final def replaceTexts(
      replacements: Map[String, String],
      in: List[File],
      isFileSupported: (File) => Boolean
  ): IOResult[List[ProcessedFile]] =
    fetchFilesRecursively(in, isFileSupported) map { files =>
      files.map(replaceBlocksInFile(None, None, replacements, _))
    }

  private[this] def replaceBlocksInFile(
      startBlockRegex: Option[Regex],
      endBlockRegex: Option[Regex],
      replacements: Map[String, String],
      in: File
  ): ProcessedFile = {

    val result: IOResult[Boolean] = for {
      content <- getFileContent(in.getAbsolutePath)
      replaced = replaceContent(content, startBlockRegex, endBlockRegex, replacements)
      modified <- if (replaced != content) {
        writeContentToFile(replaced, in.getAbsolutePath).map(_ => true)
      } else Right(false)
    } yield modified

    result match {
      case Right(m) => ProcessedFile(in, successStatus(modified = m))
      case Left(e)  => ProcessedFile(in, failureStatus(e))
    }
  }

  @tailrec
  private[this] final def replaceContent(
      unprocessed: String,
      startBlockRegex: Option[Regex],
      endBlockRegex: Option[Regex],
      replacements: Map[String, String],
      replaced: String = ""
  ): String = {

    case class TextBlock(startEnd: Int, text: String, endStart: Int, endEnd: Int)

    def textBetween(startR: Regex, endR: Regex): Option[TextBlock] =
      (startR.findFirstMatchIn(unprocessed), endR.findFirstMatchIn(unprocessed)) match {
        case (Some(startMatch), Some(endMatch)) if startMatch.end < endMatch.start =>
          Some(
            TextBlock(
              startMatch.end,
              unprocessed.subStr(startMatch.end, endMatch.start),
              endMatch.start,
              endMatch.end
            )
          )
        case _ => None
      }

    def textToReplace: Option[TextBlock] =
      (unprocessed.trim.nonEmpty, startBlockRegex, endBlockRegex) match {
        case (true, Some(startR), Some(endR)) => textBetween(startR, endR)
        case (true, None, None) =>
          Some(TextBlock(0, unprocessed, unprocessed.length, unprocessed.length))
        case _ => None
      }

    def tryToReplace: Option[(String, Int)] =
      textToReplace.map { textBlock =>
        val replaced = replacements.foldLeft(textBlock.text) {
          case (text, (target, replacement)) => text.replaceAll(target, replacement)
        }
        val newContent = unprocessed.subStr(0, textBlock.startEnd) + replaced + unprocessed.subStr(
          textBlock.endStart,
          textBlock.endEnd
        )
        (newContent, textBlock.endEnd)
      }

    tryToReplace match {
      case None => replaced + unprocessed
      case Some((replacedBlock, endPosition)) =>
        replaceContent(
          unprocessed.subStr(endPosition),
          startBlockRegex,
          endBlockRegex,
          replacements,
          replaced + replacedBlock
        )
    }
  }

}

object ReplaceTextEngine {

  case class ProcessedFileStatus(success: Boolean, modified: Boolean, error: Option[IOException]) {
    def failure: Boolean = !success
  }

  def successStatus(modified: Boolean): ProcessedFileStatus =
    ProcessedFileStatus(success = true, modified, error = None)
  def failureStatus(e: IOException): ProcessedFileStatus =
    ProcessedFileStatus(success = false, modified = false, error = Some(e))

  case class ProcessedFile(file: File, status: ProcessedFileStatus)

}

object textSyntax {

  implicit def stringOps(string: String): StringOps = new StringOps(string)

  final class StringOps(string: String) {

    def subStr(pos: Int): String = safeSubStr(string, pos)

    def subStr(start: Int, end: Int): String = safeSubStr(string, start, end)

  }

}
