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
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.IOArbitraries._
import sbtorgpolicies.exceptions.IOException

class TemplatesEngineTest extends TestOps {

  val mockFileReader: FileReader = mock[FileReader]
  val mockFileWriter: FileWriter = mock[FileWriter]

  val templatesEngine: TemplatesEngine = new TemplatesEngine {

    override val fileReader: FileReader = mockFileReader

    override val fileWriter: FileWriter = mockFileWriter
  }

  test("TemplatesEngine.run works as expected") {

    val property = forAll { (inputPath: String, content: String, outputPath: String, replacements: Replacements) =>
      Mockito.reset(mockFileReader, mockFileWriter)

      when(
        mockFileReader
          .withFileContent(any[String], any[String => IOResult[String]]()))
        .thenReturn(content.asRight.asRight)

      when(mockFileWriter.writeContentToFile(content, outputPath)).thenReturn(().asRight)

      val result = templatesEngine.run(inputPath, replacements, outputPath)

      verify(mockFileReader).withFileContent(any[String], any[(String) => String]())
      verify(mockFileWriter).writeContentToFile(content, outputPath)

      result.isRight
    }

    check(property)
  }

  test("TemplatesEngine.run fails when FileReader throws and Exception") {

    val property = forAll {
      (inputPath: String, content: String, exceptionMsg: String, outputPath: String, replacements: Replacements) =>
        Mockito.reset(mockFileReader, mockFileWriter)

        when(
          mockFileReader
            .withFileContent(any[String], any[String => IOResult[String]]()))
          .thenReturn(
            IOException(exceptionMsg)
              .asLeft[String]
              .asRight[IOException])

        when(mockFileWriter.writeContentToFile(content, outputPath)).thenReturn(().asRight)

        val result = templatesEngine.run(inputPath, replacements, outputPath)

        verify(mockFileReader).withFileContent(any[String], any[(String) => String]())
        verifyZeroInteractions(mockFileWriter)

        result.isLeft
    }

    check(property)
  }

  test("TemplatesEngine.run fails when FileWriter throws and Exception") {

    val property = forAll {
      (inputPath: String, content: String, exceptionMsg: String, outputPath: String, replacements: Replacements) =>
        Mockito.reset(mockFileReader, mockFileWriter)

        when(
          mockFileReader
            .withFileContent(any[String], any[String => IOResult[String]]()))
          .thenReturn(content.asRight.asRight)

        when(mockFileWriter.writeContentToFile(content, outputPath)).thenReturn(IOException(exceptionMsg)
          .asLeft[Unit])

        val result = templatesEngine.run(inputPath, replacements, outputPath)

        verify(mockFileReader).withFileContent(any[String], any[(String) => String]())
        verify(mockFileWriter).writeContentToFile(content, outputPath)

        result.isLeft
    }

    check(property)
  }

  test("TemplatesEngine.replaceFileWith works as expected") {

    val property = forAll { (inputPath: String, content: String, replacements: Replacements) =>
      Mockito.reset(mockFileReader)

      when(
        mockFileReader
          .withFileContent(any[String], any[String => IOResult[String]]()))
        .thenReturn(content.asRight.asRight)

      val result = templatesEngine.replaceFileWith(inputPath, replacements)

      verify(mockFileReader).withFileContent(any[String], any[(String) => String]())

      result.isRight && result.right.get == content
    }

    check(property)
  }

  test("TemplatesEngine.replaceFileWith fails when FileReader throws and Exception") {

    val property = forAll { (inputPath: String, exceptionMsg: String, replacements: Replacements) =>
      Mockito.reset(mockFileReader)

      when(
        mockFileReader
          .withFileContent(any[String], any[String => IOResult[String]]()))
        .thenReturn(
          IOException(exceptionMsg)
            .asLeft[String]
            .asRight[IOException])

      val result = templatesEngine.replaceFileWith(inputPath, replacements)

      verify(mockFileReader).withFileContent(any[String], any[(String) => String]())

      result.isLeft
    }

    check(property)
  }

  test("TemplatesEngine.replaceWith works as expected") {

    import sbtorgpolicies.io.syntax._

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
      "contributors"         -> List("47 Deg".asReplaceable, "Developers".asReplaceable).asReplaceable
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

    import sbtorgpolicies.io.syntax._

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
}
