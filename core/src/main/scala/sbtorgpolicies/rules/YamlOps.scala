/*
 * Copyright 2017-2020 47 Degrees, LLC. <http://www.47deg.com>
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
import net.jcazevedo.moultingyaml._
import sbtorgpolicies.exceptions.YamlException
import sbtorgpolicies.rules.syntax._

class YamlOps {

  def parseContent(str: String): YamlResult[YamlObject] =
    Either
      .catchNonFatal(str.parseYaml.asYamlObject)
      .leftMap(e => YamlException(s"Error parsing yaml string:\n$str", Some(e)))

  def getFields(yamlContent: String, fields: String*): YamlResult[Seq[YamlValue]] =
    for {
      yaml       <- parseContent(yamlContent)
      yamlValues <- yaml.getFieldsEither(fields: _*)
    } yield yamlValues

}

object YamlOps extends YamlOps
