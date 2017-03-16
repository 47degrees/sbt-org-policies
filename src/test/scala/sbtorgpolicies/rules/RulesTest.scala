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

import cats.syntax.either._
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.ExceptionArbitraries._
import sbtorgpolicies.exceptions.{IOException, ValidationException}
import sbtorgpolicies.io.FileReader

class RulesTest extends TestOps {

  test("emptyValidation should return Right for all strings") {

    val property = forAll { (randomString: String) =>
      emptyValidation(randomString) shouldBeEq ().asRight[ValidationException]
    }

    check(property)
  }

  test("requiredStrings should return Right if all strings are included in the content") {

    val property = forAll { (randomStrings: List[String]) =>
      val content =
        s"""
           |Start
           |${randomStrings.map(s => s"start string\n $s \n end string \n")}
           |End
         """.stripMargin

      requiredStrings(randomStrings)(content) shouldBeEq ().asRight[ValidationException]
    }

    check(property)
  }

  test("requiredStrings should return Left if some strings aren't included in the content") {

    val property = forAll { (existing: List[String], missing: List[String]) =>
      val content =
        s"""
           |Start
           |${existing.map(s => s"start string\n $s \n end string \n")}
           |End
         """.stripMargin

      val realMissing = missing.filterNot(existing.contains)

      val result = requiredStrings(existing ++ realMissing)(content)
      if (realMissing.isEmpty) {
        result shouldBeEq ().asRight[ValidationException]
      } else {
        result.isLeft shouldBeEq true
      }
    }

    check(property)
  }

  test("requiredSection should return Right if the section is included in the content") {

    val content =
      s"""
           |Start
           | ### Changelog
           |
           | ### Contributors
           |  * User 1
           |  * User 2
           |  * User 3
           |End
         """.stripMargin

    val rightResponse = ().asRight[ValidationException]

    def changelogValidation(s: String): ValidationResult =
      if (s.nonEmpty) rightResponse else ValidationException("").asLeft[Unit]

    requiredSection("\\#\\#\\#.*Changelog", "\\#\\#\\#", changelogValidation)(content) shouldBeEq rightResponse

    def contributorsValidation(s: String): ValidationResult =
      if (s.contains("User 1") && s.contains("User 2") && s.contains("User 3")) rightResponse else ValidationException("").asLeft[Unit]

    requiredSection("\\#\\#\\#.*Contributors", "\\#\\#\\#", contributorsValidation)(content) shouldBeEq rightResponse
  }

  test("requiredSection should return Left if the section is not included in the content") {

    val content =
      s"""
           |Start
           | Other stuff
           | ### Readme
           |End
         """.stripMargin

    val leftResponse = ValidationException("Section not found").asLeft[Unit]

    def changelogValidation(s: String): ValidationResult = ().asRight[ValidationException]

    requiredSection("\\#\\#\\#.*Changelog", "\\#\\#\\#", changelogValidation)(content) shouldBeEq leftResponse
  }

  test("requiredSection should return Left if the section is not valid") {

    val content =
      s"""
         |Start
         | ### Changelog
         |
         | ### Contributors
         |  * User 1
         |  * User 2
         |  * User 3
         |End
         """.stripMargin

    val leftResponse = ValidationException("Section not valid").asLeft[Unit]

    def contributorsValidation(s: String): ValidationResult =
      ValidationException("Section not valid").asLeft[Unit]

    requiredSection("\\#\\#\\#.*Contributors", "\\#\\#\\#", contributorsValidation)(content) shouldBeEq leftResponse
  }

  test("requiredStrings and requiredSection should works as expected when combined") {

    val content =
      s"""
           |Start
           | ### Changelog
           |
           | ### Contributors
           |  * User 1
           |  * User 2
           |  * User 3
           |End
         """.stripMargin

    val rightResponse = ().asRight[ValidationException]

    def changelogValidation(s: String): ValidationResult =
      if (s.nonEmpty) rightResponse else ValidationException("").asLeft[Unit]

    requiredSection("\\#\\#\\#.*Changelog", "\\#\\#\\#", changelogValidation)(content) shouldBeEq rightResponse

    def contributorsValidation(s: String): ValidationResult =
      requiredStrings(List("User 1", "User 2", "User 3"))(s)

    requiredSection("\\#\\#\\#.*Contributors", "\\#\\#\\#", contributorsValidation)(content) shouldBeEq rightResponse
  }

}
