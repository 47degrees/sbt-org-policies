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

import cats.instances.either._
import cats.instances.list._
import cats.syntax.cartesian._
import cats.syntax.either._
import cats.syntax.foldable._
import cats.syntax.traverse._
import cats.syntax.traverseFilter._
import sbt.io._
import sbt.io.syntax._
import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.io.syntax._
import sbtorgpolicies.templates._

class FileHelper {

  val fileReader: FileReader = new FileReader

  val fileWriter: FileWriter = new FileWriter

  val templatesEngine: TemplatesEngine = new TemplatesEngine

  def getPluginUrl: IOResult[URL] =
    Either
      .catchNonFatal(getClass.getProtectionDomain.getCodeSource.getLocation)
      .leftMap(e => IOException(s"Error obtaining the plugin URL", Some(e)))

  def createResources(userTemplatesDir: File, outputDir: File): IOResult[Unit] = {

    val templatesOutput = s"${outputDir.getAbsolutePath.ensureFinalSlash}"

    for {
      pluginUrl <- getPluginUrl
      _         <- fileWriter.copyJARResourcesTo(pluginUrl, templatesOutput, "templates")
      _         <- fileWriter.copyFilesRecursively(userTemplatesDir.getAbsolutePath, s"${templatesOutput}templates")
    } yield ()
  }

  def checkOrgFiles(projectDir: File, baseDir: File, fileList: List[FileType]): IOResult[List[FileType]] = {

    def templatePath(f: FileType): String =
      baseDir.getAbsolutePath.ensureFinalSlash + f.templatePath

    def outputPath(f: FileType): String =
      projectDir.getAbsolutePath.ensureFinalSlash + f.outputPath

    def checkFiles(): IOResult[Unit] =
      fileList.traverseU_ { f =>
        if (!fileReader.exists(templatePath(f)))
          IOException(s"File not found: ${f.templatePath}").asLeft
        else ().asRight
      }

    def prepareFileContent(file: FileType): IOResult[Option[String]] =
      if (!fileReader.exists(file.outputPath) || file.overWritable) {
        templatesEngine.replaceFileContentsWith(templatePath(file), file.replacements) map (Option(_))
      } else if (file.fileSections.nonEmpty) {
        fileReader.getFileContent(file.outputPath) map (Option(_))
      } else Right(None)

    def replaceSection(fileContent: String, fileSection: FileSection): IOResult[String] =
      if (fileSection.shouldAppend(fileContent)) {
        for {
          section <- templatesEngine.replaceWith(fileSection.template, fileSection.replacements)
          content <- templatesEngine.insertIn(fileContent, fileSection.appendPosition, section)
        } yield content
      } else Right(fileContent)

    def replaceSections(fileContent: String, fileSections: List[FileSection]): IOResult[String] =
      fileSections.foldM(fileContent)(replaceSection)

    def processSectionsIfWritable(maybeContent: Option[String], fileType: FileType): IOResult[Option[String]] =
      maybeContent map { c =>
        replaceSections(c, fileType.fileSections) map (Option(_))
      } getOrElse None.asRight

    def writeToFileIfWritable(maybeContent: Option[String], fileType: FileType): IOResult[Option[FileType]] =
      maybeContent map { c =>
        fileWriter.writeContentToFile(c, outputPath(fileType)) map (_ => Option(fileType))
      } getOrElse None.asRight

    def processFile(fileType: FileType): IOResult[Option[FileType]] =
      for {
        fileContent   <- prepareFileContent(fileType)
        newContent    <- processSectionsIfWritable(fileContent, fileType)
        maybeFileType <- writeToFileIfWritable(newContent, fileType)
      } yield maybeFileType

    def processFiles(fileTypes: List[FileType]): IOResult[List[FileType]] =
      fileTypes.traverseFilter(processFile)

    checkFiles *> processFiles(fileList)
  }

}
