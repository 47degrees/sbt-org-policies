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

package sbtorgpolicies.arbitraries

import org.scalacheck.{Arbitrary, Gen}
import sbtorgpolicies.templates._

trait TemplateArbitraries {

  implicit val replaceableArbitrary: Arbitrary[ReplaceableT[String]] =
    Arbitrary(Gen.alphaStr map (ReplaceableT(_)))

  implicit val replacementsTupleArbitrary: Arbitrary[(String, Replaceable)] =
    Arbitrary {
      for {
        key         <- Gen.identifier
        replaceable <- replaceableArbitrary.arbitrary
      } yield (key, replaceable)
    }

  implicit val replacementsArbitrary: Arbitrary[Replacements] =
    Arbitrary(Gen.mapOf(replacementsTupleArbitrary.arbitrary))
}

object TemplateArbitraries extends TemplateArbitraries
