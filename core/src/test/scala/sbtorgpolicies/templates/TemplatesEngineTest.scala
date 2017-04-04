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

package sbtorgpolicies.templates

import cats.syntax.either._
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.ExceptionArbitraries._
import sbtorgpolicies.arbitraries.TemplateArbitraries._
import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.io._
import sbtorgpolicies.templates.syntax._
import sbtorgpolicies.io.syntax._

import scala.util.matching.Regex

class TemplatesEngineTest extends TestOps {

  val mockFileReader: FileReader = mock[FileReader]
  val mockFileWriter: FileWriter = mock[FileWriter]

  val templatesEngine: TemplatesEngine = new TemplatesEngine {

    override val fileReader: FileReader = mockFileReader

    override val fileWriter: FileWriter = mockFileWriter
  }

  test("TemplatesEngine.replaceFileContentsWith works as expected") {

    val property = forAll { (inputPath: String, content: String, replacements: Replacements) =>
      Mockito.reset(mockFileReader)

      when(
        mockFileReader
          .withFileContent(any[String], any[String => IOResult[String]]()))
        .thenReturn(content.asRight)

      val result = templatesEngine.replaceFileContentsWith(inputPath, replacements)

      verify(mockFileReader).withFileContent(any[String], any[(String) => IOResult[String]]())

      result.isRight && result.right.get == content
    }

    check(property)
  }

  test("TemplatesEngine.replaceFileContentsWith fails when FileReader throws and Exception") {

    val property = forAll { (inputPath: String, exception: IOException, replacements: Replacements) =>
      Mockito.reset(mockFileReader)

      when(
        mockFileReader
          .withFileContent(any[String], any[String => IOResult[String]]()))
        .thenReturn(exception.asLeft)

      val result = templatesEngine.replaceFileContentsWith(inputPath, replacements)

      verify(mockFileReader).withFileContent(any[String], any[(String) => IOResult[String]]())

      result.isLeft
    }

    check(property)
  }

  test("TemplatesEngine.replaceWith works as expected") {

    val originalContent =
      """
        |{{year}} {{organizationName}}.
        |<{{organizationHomePage}}>
        |
        |{{contributors}}
      """.stripMargin

    val replacements = Map(
      "year"                 -> 2017.asReplaceable,
      "organizationName"     -> "47 Degrees".asReplaceable,
      "organizationHomePage" -> "http://www.47deg.com".asReplaceable,
      "contributors"         -> List("47 Deg", "Developers").asReplaceable
    )

    val expectedContent = originalContent
      .replace("{{year}}", "2017")
      .replace("{{organizationName}}", "47 Degrees")
      .replace("{{organizationHomePage}}", "http://www.47deg.com")
      .replace(
        "{{contributors}}",
        """* 47 Deg
          |* Developers""".stripMargin)

    val result = templatesEngine.replaceWith(originalContent, replacements)

    result.isRight shouldBe true
    result.right.get shouldBe expectedContent
  }

  test("TemplatesEngine.replaceWith returns the same content when no macros are found") {

    val originalContent =
      """
        |2017 47 Degrees.
      """.stripMargin

    val replacements = Map(
      "year"                 -> 2017.asReplaceable,
      "organizationName"     -> "47 Degrees".asReplaceable,
      "organizationHomePage" -> "http://www.47deg.com".asReplaceable
    )

    val result = templatesEngine.replaceWith(originalContent, replacements)

    result.isRight shouldBe true
    result.right.get shouldBe originalContent
  }

  test("TemplatesEngine.replaceWith returns the same content when the replacements map is empty") {

    val originalContent =
      """
        |{{year}} 47 Degrees.
      """.stripMargin

    val replacements = Map.empty[String, Replaceable]

    val result = templatesEngine.replaceWith(originalContent, replacements)

    result.isRight shouldBe true
    result.right.get shouldBe originalContent
  }

  test("TemplatesEngine.insertIn works as expected when passing AppendAtTheBeginning") {
    check {
      forAll { (content: String, section: String) =>
        val result = templatesEngine.insertIn(content, AppendAtTheBeginning, section)

        (result.isRight && result.right.get.startsWith(section) && result.right.get.endsWith(content)) shouldBeEq true
      }
    }
  }

  test("TemplatesEngine.insertIn works as expected when passing AppendAtTheEnd") {
    check {
      forAll { (content: String, section: String) =>
        val result = templatesEngine.insertIn(content, AppendAtTheEnd, section)

        (result.isRight && result.right.get.endsWith(section) && result.right.get.startsWith(content)) shouldBeEq true
      }
    }
  }

  test("TemplatesEngine.insertIn works as expected when passing AppendAfter") {

    val originalContent =
      """
        | Title
        |
        | Other Stuff
      """.stripMargin

    val template = "\n2017 (47 Degrees)"

    val replacements = Map(
      "year" -> 2017.asReplaceable,
      "name" -> "47 Degrees".asReplaceable
    )

    val expectedContent =
      """
        | Title
        |
        |2017 (47 Degrees)
        |
        | Other Stuff
      """.stripMargin

    val result = templatesEngine.insertIn(originalContent, AppendAfter(" Title".r), template)

    result.isRight shouldBe true
    result.right.get shouldBe expectedContent
  }
}
