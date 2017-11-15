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

package sbtorgpolicies.rules

import cats.instances.list._
import cats.kernel.instances.unit._
import cats.syntax.either._
import cats.syntax.foldable._
import cats.syntax.validated._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.exceptions.{IOException, ValidationException}
import sbtorgpolicies.arbitraries.ExceptionArbitraries._
import cats.laws.discipline.arbitrary._
import org.scalatest.OneInstancePerTest
import sbtorgpolicies.io.FileReader

class FileValidationTest extends TestOps with OneInstancePerTest {

  val mockFileReader: FileReader = mock[FileReader]

  val fileValidation: FileValidation = new FileValidation(mockFileReader)

  test("FileValidation.validateFile works as expected") {

    val property = forAll { (inputPath: String, content: String, validationResult: ValidationResult) =>
      (mockFileReader.getFileContent _).expects(inputPath).returns(content.asRight)
      val result = fileValidation.validateFile(inputPath, _ => validationResult)
      result shouldBeEq validationResult
    }

    check(property)
  }

  test("FileValidation.validateFile fails when FileReader throws and Exception") {

    val property = forAll { (inputPath: String, exception: IOException) =>
      (mockFileReader.getFileContent _).expects(inputPath).returns(exception.asLeft[String])
      val result = fileValidation.validateFile(inputPath, _ => ().validNel)
      result shouldBeEq ValidationException(s"Can't read $inputPath", Some(exception)).invalidNel
    }

    check(property)
  }

  test("FileValidation.validateFile should accumulate the invalid results") {

    val property = forAll { (inputPath: String, content: String, results: List[ValidationResult]) =>
      (mockFileReader.getFileContent _).expects(inputPath).returns(content.asRight)
      def validationFunction(result: ValidationResult)(s: String): ValidationResult = result
      val validations: List[(String) => ValidationResult]                           = results.map(validationFunction)
      val result                                                                    = fileValidation.validateFile(inputPath, validations: _*)
      result shouldBeEq results.combineAll
    }

    check(property)
  }

}
