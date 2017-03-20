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

package sbtorgpolicies.settings

import cats.data.Validated.{Invalid, Valid}
import sbt._
import sbt.Keys._
import sbtorgpolicies.rules._
import sbtorgpolicies.rules.FileValidation
import sbtorgpolicies.{PolicyError, PolicyWarning, Validation, ValidationRule}

trait validationFilesKeys {

  val validationList: SettingKey[List[Validation]] = settingKey[List[Validation]]("Validation list")

  val validateFiles: TaskKey[Unit] = taskKey[Unit]("Validate all files")

}

trait validationFiles extends validationFilesKeys {

  val validationSettings = Seq(
    validationList := List(
      validation(
        new File(baseDirectory.value, "README.md").getAbsolutePath,
        List(emptyValidation)),
      validation(
        new File(baseDirectory.value, "LICENSE").getAbsolutePath,
        List(requiredStringsValidation(List(licenses.value.headOption.map(_._1).getOrElse("UNKNOWN LICENSE")))))
    ),
    validateFiles := Def.task {
      validationFilesTask(validationList.value, streams.value.log)
    }.value
  )

  def validation(path: String, list: List[ValidationFunction]): Validation =
    Validation(PolicyError, ValidationRule(path, list))

  def validationFilesTask(list: List[Validation], log: Logger): Unit = {
    list foreach (validationFileTask(_, log))
  }

  def validationFileTask(validation: Validation, log: Logger): Unit = {

    val fileValidation = new FileValidation

    def logError(msg: String): Unit =
      if (validation.policyLevel == PolicyWarning) log.warn(msg) else log.error(msg)

    fileValidation.validateFile(validation.validationRule.inputPath, validation.validationRule.validationList: _*) match {
      case Valid(_)        => log.info(s"File ${validation.validationRule.inputPath} was validated successfully")
      case Invalid(errors) =>
        logError(s"Some errors where found while validating ${validation.validationRule.inputPath}:")
        errors.toList foreach (e => logError(s" - ${e.message}"))
    }

  }

}
