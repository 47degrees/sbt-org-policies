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

import cats.syntax.validated._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.exceptions.ValidationException

class RulesTest extends TestOps {

  import ValidationFunctions._

  test("emptyValidation should return Right for all strings") {

    val property = forAll { (randomString: String) =>
      emptyValidation(randomString) shouldBeEq ().validNel
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

      requiredStrings(randomStrings)(content) shouldBeEq ().validNel
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

      val realMissing = missing.filterNot(existing.contains).filter(_.nonEmpty)

      val result = requiredStrings(existing ++ realMissing)(content)
      if (realMissing.isEmpty) {
        result shouldBeEq ().validNel
      } else {
        result.isInvalid shouldBeEq true
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

    val validResponse = ().validNel

    def changelogValidation(s: String): ValidationResult =
      if (s.nonEmpty) validResponse else ValidationException("").invalidNel

    def contributorsValidation(s: String): ValidationResult =
      if (s.contains("User 1") && s.contains("User 2") && s.contains("User 3")) validResponse
      else ValidationException("").invalidNel

    requiredSection("\\#\\#\\#.*Changelog".r, "\\#\\#\\#".r, changelogValidation)(content) shouldBeEq validResponse
    requiredSection("\\#\\#\\#.*Contributors".r, "\\#\\#\\#".r, contributorsValidation)(content) shouldBeEq validResponse
  }

  test("requiredSection should return Left if the section is not included in the content") {

    val content =
      s"""
           |Start
           | Other stuff
           | ### Readme
           |End
         """.stripMargin

    val invalidResponse = ValidationException("Section not found").invalidNel

    def changelogValidation(s: String): ValidationResult = ().validNel

    requiredSection("\\#\\#\\#.*Changelog".r, "\\#\\#\\#".r, changelogValidation)(content) shouldBeEq invalidResponse
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

    val invalidResponse = ValidationException("Section not valid").invalidNel

    def contributorsValidation(s: String): ValidationResult =
      ValidationException("Section not valid").invalidNel

    requiredSection("\\#\\#\\#.*Contributors".r, "\\#\\#\\#".r, contributorsValidation)(content) shouldBeEq invalidResponse
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

    val validResponse = ().validNel

    def changelogValidation(s: String): ValidationResult =
      if (s.nonEmpty) validResponse else ValidationException("").invalidNel

    def contributorsValidation(s: String): ValidationResult =
      requiredStrings(List("User 1", "User 2", "User 3"))(s)

    requiredSection("\\#\\#\\#.*Changelog".r, "\\#\\#\\#".r, changelogValidation)(content) shouldBeEq validResponse
    requiredSection("\\#\\#\\#.*Contributors".r, "\\#\\#\\#".r, contributorsValidation)(content) shouldBeEq validResponse
  }

}
