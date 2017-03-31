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
import sbt.{File, URL}
import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.io.syntax._
import sbtorgpolicies.templates.{AppendableFileType, FileType, ReplaceableFileType, TemplatesEngine}

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

  def checkOrgFiles(projectDir: File, baseDir: File, fileList: List[FileType]): IOResult[Unit] = {

    def checkFiles(): Unit = fileList foreach { f =>
      if (!fileReader.exists(baseDir.getAbsolutePath.ensureFinalSlash + f.templatePath))
        throw new IllegalArgumentException(s"File not found: ${f.templatePath}")
    }

    Either
      .catchNonFatal {
        checkFiles()
        fileList
          .filter(f => !fileReader.exists(f.outputPath) || f.overWritable)
          .foreach {
            case f: ReplaceableFileType =>
              templatesEngine.run(
                inputPath = baseDir.getAbsolutePath.ensureFinalSlash + f.templatePath,
                outputPath = projectDir.getAbsolutePath.ensureFinalSlash + f.outputPath,
                replacements = f.replacements)
            case f: AppendableFileType =>
              templatesEngine.runInsert(
                inputPath = baseDir.getAbsolutePath.ensureFinalSlash + f.templatePath,
                outputPath = projectDir.getAbsolutePath.ensureFinalSlash + f.outputPath,
                regexpLine = f.afterLine,
                template = f.template,
                replacements = f.replacements)
          }
      }
      .leftMap(e => IOException(s"Error checking files ${fileList.map(_.outputPath).mkString(",")}", Some(e)))
  }

}
