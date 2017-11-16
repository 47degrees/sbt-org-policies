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

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import org.scalacheck.{Arbitrary, Gen}
import sbtorgpolicies.exceptions.{IOException, ValidationException}

trait ExceptionArbitraries {

  val exceptionMessage: String = "Generated Exception"

  implicit val ioExceptionArbitrary: Arbitrary[IOException] = Arbitrary {
    for {
      msg            <- Gen.alphaStr
      maybeException <- Gen.option(new RuntimeException(exceptionMessage))
    } yield IOException(msg, maybeException)
  }

  implicit val validationExceptionArbitrary: Arbitrary[ValidationException] = Arbitrary {
    for {
      msg            <- Gen.alphaStr
      maybeException <- Gen.option(new RuntimeException(exceptionMessage))
    } yield ValidationException(msg, maybeException)
  }

  implicit def nonEmptyListArbitrary[A](implicit AA: Arbitrary[A]): Arbitrary[NonEmptyList[A]] = Arbitrary {
    for {
      head <- AA.arbitrary
      tail <- Gen.listOf(AA.arbitrary)
    } yield NonEmptyList(head, tail)
  }

  def validGen[A](implicit AA: Arbitrary[A]): Gen[Valid[A]] = AA.arbitrary.map(Valid(_))

  def invalidGen[A](implicit AA: Arbitrary[A]): Gen[Invalid[A]] = AA.arbitrary.map(Invalid(_))

  implicit def validatedNelArbitrary[A, B](implicit AA: Arbitrary[A], BA: Arbitrary[B]): Arbitrary[ValidatedNel[A, B]] =
    Arbitrary(Gen.oneOf(invalidGen[NonEmptyList[A]], invalidGen[NonEmptyList[A]]))

}

object ExceptionArbitraries extends ExceptionArbitraries
