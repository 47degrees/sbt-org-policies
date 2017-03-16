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

package sbtorgpolicies.arbitraries

import cats.data.ValidatedNel
import cats.syntax.validated._
import org.scalacheck.{Arbitrary, Gen}

trait ValidationArbitraries {

  implicit def invalidNelGen[E, A](implicit E: Arbitrary[E]): Gen[ValidatedNel[E, A]] = {
    E.arbitrary map (_.invalidNel[A])
  }

  implicit def validNelGen[E, A](implicit A: Arbitrary[A]): Gen[ValidatedNel[E, A]] = {
    A.arbitrary map (_.validNel[E])
  }

  implicit def validatedNelArbitrary[E, A](implicit E: Arbitrary[E], A: Arbitrary[A]): Arbitrary[ValidatedNel[E, A]] = Arbitrary {
    Gen.oneOf(invalidNelGen[E, A], validNelGen[E, A])
  }

}

object ValidationArbitraries extends ValidationArbitraries
