/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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
import java.net.URL

import cats.instances.either._
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.foldable._
import cats.syntax.traverseFilter._
import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.io.syntax._
import sbtorgpolicies.templates._
import FileReader._
import FileWriter._

class FileHelper {

  val templatesEngine: TemplatesEngine = new TemplatesEngine

  def getPluginUrl: IOResult[URL] =
    Either
      .catchNonFatal(getClass.getProtectionDomain.getCodeSource.getLocation)
      .leftMap(e => IOException(s"Error obtaining the plugin URL", Some(e)))

  def createResources(userTemplatesDir: File, outputDir: File): IOResult[Unit] = {

    val templatesOutput = s"${outputDir.getAbsolutePath.ensureFinalSlash}"

    for {
      pluginUrl <- getPluginUrl
      _         <- copyJARResourcesTo(pluginUrl, templatesOutput, "templates")
      _         <- copyFilesRecursively(userTemplatesDir.getAbsolutePath, s"${templatesOutput}templates")
    } yield ()
  }

  def checkOrgFiles(projectDir: File, baseDir: File, fileList: List[FileType]): IOResult[List[FileType]] = {

    def templatePath(f: FileType): String =
      baseDir.getAbsolutePath.ensureFinalSlash + f.templatePath

    def outputPath(f: FileType): String =
      projectDir.getAbsolutePath.ensureFinalSlash + f.outputPath

    def checkFiles(): IOResult[Unit] =
      fileList.traverse_ { f =>
        if (!exists(templatePath(f)))
          IOException(s"File not found: ${f.templatePath}").asLeft
        else ().asRight
      }

    def prepareFileContent(file: FileType): IOResult[Option[String]] =
      if (!exists(file.outputPath) || file.overWritable) {
        templatesEngine.replaceFileContentsWith(templatePath(file), file.replacements) map (Option(_))
      } else if (file.fileSections.nonEmpty) {
        getFileContent(file.outputPath) map (Option(_))
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
        writeContentToFile(c, outputPath(fileType)) map (_ => Option(fileType))
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
