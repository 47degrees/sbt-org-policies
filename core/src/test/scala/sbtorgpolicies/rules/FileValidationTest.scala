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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.exceptions.{IOException, ValidationException}
import sbtorgpolicies.arbitraries.ExceptionArbitraries._
import cats.laws.discipline.arbitrary._
import sbtorgpolicies.io.FileReader

class FileValidationTest extends TestOps {

  val mockFileReader: FileReader = mock[FileReader]

  val fileValidation: FileValidation = new FileValidation {

    override val fileReader: FileReader = mockFileReader
  }

  test("FileValidation.validateFile works as expected") {

    val property = forAll { (inputPath: String, content: String, validationResult: ValidationResult) =>
      Mockito.reset(mockFileReader)

      when(mockFileReader.getFileContent(any[String])).thenReturn(content.asRight)

      val result = fileValidation.validateFile(inputPath, _ => validationResult)

      verify(mockFileReader).getFileContent(any[String])

      result shouldBeEq validationResult
    }

    check(property)
  }

  test("FileValidation.validateFile fails when FileReader throws and Exception") {

    val property = forAll { (inputPath: String, exception: IOException) =>
      Mockito.reset(mockFileReader)

      val left = exception.asLeft[String]

      when(mockFileReader.getFileContent(any[String])).thenReturn(left)

      val result = fileValidation.validateFile(inputPath, _ => ().validNel)

      verify(mockFileReader).getFileContent(any[String])

      result shouldBeEq ValidationException(s"Can't read $inputPath", Some(exception)).invalidNel
    }

    check(property)
  }

  test("FileValidation.validateFile should accumulate the invalid results") {

    val property = forAll { (inputPath: String, content: String, results: List[ValidationResult]) =>
      Mockito.reset(mockFileReader)

      when(mockFileReader.getFileContent(any[String])).thenReturn(content.asRight)

      def validationFunction(result: ValidationResult)(s: String): ValidationResult = result

      val validations: List[(String) => ValidationResult] = results.map(validationFunction)

      val result = fileValidation.validateFile(inputPath, validations: _*)

      verify(mockFileReader).getFileContent(any[String])

      result shouldBeEq results.combineAll
    }

    check(property)
  }

}
