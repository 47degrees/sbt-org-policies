/*
 * Copyright 2017-2019 47 Degrees, LLC. <http://www.47deg.com>
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
import sbtorgpolicies.exceptions.YamlException

import scala.language.implicitConversions

object syntax {

  import net.jcazevedo.moultingyaml._
  import sbtorgpolicies.model.YamlFormats._

  type YamlResult[T] = Either[YamlException, T]

  implicit def yamlStringOpsSyntax(yamlObject: YamlObject): YamlStringOps =
    new YamlStringOps(yamlObject)
  implicit def resultConversionOpsSyntax(result: YamlResult[Seq[YamlValue]]): YamlResultOps =
    new YamlResultOps(result)

  final class YamlStringOps(yamlObject: YamlObject) {

    def getFieldsEither(fields: String*): YamlResult[Seq[YamlValue]] =
      Either
        .catchNonFatal(yamlObject.getFields(fields.toList map YamlString: _*))
        .leftMap(e => YamlException(s"Error reading fields $fields from yaml", Some(e)))

  }

  final class YamlResultOps(result: YamlResult[Seq[YamlValue]]) {

    def toList: List[String] = mapToString.getOrElse(Nil)

    def mapToString: YamlResult[List[String]] =
      result.map(_.flatMap(_.convertTo[List[String]]).toList)

  }

}
