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
import sbt.Keys._
import sbt._
import sbtorgpolicies.exceptions.ValidationException
import sbtorgpolicies.model.Dev
import sbtorgpolicies.rules._
import sbtorgpolicies.OrgPoliciesKeys._

trait fileValidation extends ValidationFunctions {

  val fileValidation = new FileValidation
  import fileValidation.fileReader._

  val fileValidationDefaultSettings = Seq(
    orgValidationListSetting := List(
      mkValidation(getChildPath(baseDirectory.value, "README.md"), List(requiredStrings(readmeSections(name.value)))),
      mkValidation(getChildPath(baseDirectory.value, "CONTRIBUTING.md"), List(emptyValidation)),
      mkValidation(
        getChildPath(baseDirectory.value, "AUTHORS.md"),
        List(requiredStrings(devListStrings(orgMaintainersSetting.value ++ orgContributorsSetting.value)))),
      mkValidation(
        getChildPath(baseDirectory.value, "LICENSE"),
        List(requiredStrings(List(orgLicenseSetting.value.name)))
      ),
      mkValidation(
        getChildPath(baseDirectory.value, "NOTICE.md"),
        List(requiredStrings(List(name.value, orgLicenseSetting.value.name)))
      ),
      mkValidation(getChildPath(baseDirectory.value, sbtorgpolicies.templates.versionFilePath), List(emptyValidation)),
      mkValidation(
        getChildPath(baseDirectory.value, ".travis.yml"),
        List(
          validTravisFile(
            crossScalaVersions.value,
            Seq(orgScriptCICommandKey),
            Seq(orgAfterCISuccess.key.label)
          )
        )
      )
    )
  )

  val orgFileValidationTasks = Seq(
    orgValidateFiles := Def.task {
      validationFilesTask(orgValidationListSetting.value, streams.value.log)
    }.value
  )

  private[this] def readmeSections(name: String): List[String] =
    List(s"$name in the wild")

  private[this] def devListStrings(list: List[Dev]): List[String] =
    list.map(_.id) ++ list.flatMap(_.name)

  private[this] def validationFilesTask(list: List[Validation], log: Logger): Unit =
    list foreach (validationFileTask(_, log))

  private[this] def validationFileTask(validation: Validation, log: Logger): Unit = {

    def errorHandler(description: String, errorList: List[ValidationException]): Unit = {
      val errorMessage =
        s"""$description
           |${errorList map (e => s" - ${e.message}") mkString "\n"}
         """.stripMargin
      if (validation.policyLevel == PolicyWarning) log.warn(errorMessage)
      else {
        throw ValidationException(errorMessage)
      }
    }

    fileValidation.validateFile(validation.validationRule.inputPath, validation.validationRule.validationList: _*) match {
      case Valid(_) => log.info(s"File ${validation.validationRule.inputPath} was validated successfully")
      case Invalid(errors) =>
        errorHandler(
          s"Some errors where found while validating ${validation.validationRule.inputPath}:",
          errors.toList)
    }

  }

}
