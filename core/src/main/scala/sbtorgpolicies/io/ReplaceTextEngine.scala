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
import java.io.File

import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.templates.utils._

import scala.annotation.tailrec
import scala.util.matching.Regex

class ReplaceTextEngine {

  import ReplaceTextEngine._
  import textSyntax._

  val fileReader: FileReader = new FileReader
  val fileWriter: FileWriter = new FileWriter

  final def replaceBlocks(
      startBlockRegex: Regex,
      endBlockRegex: Regex,
      replacements: Map[String, String],
      in: List[File],
      isFileSupported: (File) => Boolean): List[ProcessedFile] =
    findAllFiles(in, isFileSupported) map {
      replaceBlocksInFile(startBlockRegex, endBlockRegex, replacements, _)
    }

  @tailrec
  private[this] final def findAllFiles(
      in: List[File],
      isFileSupported: (File) => Boolean,
      processedFiles: List[File] = Nil,
      processedDirs: List[String] = Nil): List[File] = {

    val files: List[File] = in.filter(file => file.isFile && isFileSupported(file))
    val allFiles          = processedFiles ++ files

    in.filter(f => f.isDirectory && !processedDirs.contains(f.getCanonicalPath)) match {
      case Nil => allFiles
      case list =>
        val subFiles = list.flatMap(_.listFiles().toList)
        findAllFiles(subFiles, isFileSupported, allFiles, processedDirs ++ list.map(_.getCanonicalPath))
    }
  }

  private[this] def replaceBlocksInFile(
      startBlockRegex: Regex,
      endBlockRegex: Regex,
      replacements: Map[String, String],
      in: File): ProcessedFile = {

    val result: IOResult[Boolean] = for {
      content <- fileReader.getFileContent(in.getAbsolutePath)
      replaced = replaceContent(content, startBlockRegex, endBlockRegex, replacements)
      modified <- if (replaced != content) {
        fileWriter.writeContentToFile(replaced, in.getAbsolutePath).map(_ => true)
      } else Right(false)
    } yield modified

    result match {
      case Right(m) => ProcessedFile(in, SuccessfullyProcessed(modified = m))
      case Left(e)  => ProcessedFile(in, ErrorProcessing(e))
    }
  }

  @tailrec
  private[this] final def replaceContent(
      unprocessed: String,
      startBlockRegex: Regex,
      endBlockRegex: Regex,
      replacements: Map[String, String],
      replaced: String = ""): String = {

    def tryToReplace: Option[(String, Int)] =
      (startBlockRegex.findFirstMatchIn(unprocessed), endBlockRegex.findFirstMatchIn(unprocessed)) match {
        case (Some(startMatch), Some(endMatch)) if startMatch.end < endMatch.start =>
          val textToBeReplaced = unprocessed.subStr(startMatch.end, endMatch.start)
          val replaced = replacements.foldLeft(textToBeReplaced) {
            case (text, (target, replacement)) =>
              text.replaceAllLiterally(target, replacement)
          }
          val newContent = unprocessed.subStr(0, startMatch.end) + replaced + unprocessed.subStr(
            endMatch.start,
            endMatch.end)
          Some((newContent, endMatch.end))
        case _ => None
      }

    tryToReplace match {
      case None => replaced + unprocessed
      case Some((replacedBlock, endPosition)) =>
        replaceContent(
          unprocessed.subStr(endPosition),
          startBlockRegex,
          endBlockRegex,
          replacements,
          replaced + replacedBlock)
    }
  }

}

object ReplaceTextEngine {

  sealed trait ProcessedFileStatus
  case class SuccessfullyProcessed(modified: Boolean) extends ProcessedFileStatus
  case class ErrorProcessing(exception: IOException)  extends ProcessedFileStatus

  case class ProcessedFile(file: File, status: ProcessedFileStatus)

}

object textSyntax {

  implicit def stringOps(string: String): StringOps = new StringOps(string)

  final class StringOps(string: String) {

    def subStr(pos: Int): String = safeSubStr(string, pos)

    def subStr(start: Int, end: Int): String = safeSubStr(string, start, end)

  }

}
