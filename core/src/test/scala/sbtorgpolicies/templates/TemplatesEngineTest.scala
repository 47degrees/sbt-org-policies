/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.ExceptionArbitraries._
import sbtorgpolicies.arbitraries.TemplateArbitraries._
import sbtorgpolicies.exceptions.IOException
import sbtorgpolicies.io._
import sbtorgpolicies.templates.syntax._

class TemplatesEngineTest extends TestOps {

  def newTemplatesEngine: (TemplatesEngine, FileReader, FileWriter) = {

    val mockFileReader: FileReader = stub[FileReader]
    val mockFileWriter: FileWriter = stub[FileWriter]

    val templatesEngine: TemplatesEngine = new TemplatesEngine(mockFileReader, mockFileWriter)

    (templatesEngine, mockFileReader, mockFileWriter)
  }

  val templatesEngine: TemplatesEngine = new TemplatesEngine

  test("TemplatesEngine.replaceFileContentsWith works as expected") {

    val property = forAll { (inputPath: String, content: String, replacements: Replacements) =>
      val (te, fileReader, _) = newTemplatesEngine
      (fileReader.withFileContent[String] _).when(*, *).returns(content.asRight)
      val result = te.replaceFileContentsWith(inputPath, replacements)
      result.isRight && result.right.get == content
    }

    check(property)
  }

  test("TemplatesEngine.replaceFileContentsWith fails when FileReader throws and Exception") {

    val property = forAll { (inputPath: String, exception: IOException, replacements: Replacements) =>
      val (te, fileReader, _) = newTemplatesEngine
      (fileReader.withFileContent[String] _).when(*, *).returns(exception.asLeft)
      te.replaceFileContentsWith(inputPath, replacements).isLeft
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

    val section = "\n2017 (47 Degrees)"

    val expectedContent =
      """
        | Title
        |
        |2017 (47 Degrees)
        |
        | Other Stuff
      """.stripMargin

    val result = templatesEngine.insertIn(originalContent, AppendAfter(" Title".r), section)

    result.isRight shouldBe true
    result.right.get shouldBe expectedContent
  }

  test("TemplatesEngine.insertIn works as expected when passing ReplaceSection") {

    val originalContent =
      """
        | (Start section)
        |
        | Old text
        |
        | (End section)
        |
        | Other Stuff
      """.stripMargin

    val originalContentWithoutSection =
      """
        | Other Stuff
      """.stripMargin

    val section =
      """
        | (Start section)
        |
        | New text
        |
        | (End section)""".stripMargin

    val expectedContent =
      """
        | (Start section)
        |
        | New text
        |
        | (End section)
        |
        | Other Stuff
      """.stripMargin

    val from = "\n \\(Start section\\)".r
    val to   = "\\(End section\\)".r

    val result1 = templatesEngine.insertIn(originalContent, ReplaceSection(from, to), section)
    result1 shouldBe Right(expectedContent)

    val result2 = templatesEngine.insertIn(originalContentWithoutSection, ReplaceSection(from, to), section)
    result2 shouldBe Right(expectedContent)

    val result3 = templatesEngine.insertIn(
      originalContentWithoutSection,
      ReplaceSection(from, to, insertIfNotFound = false),
      section)
    result3 shouldBe Right(originalContentWithoutSection)
  }
}
