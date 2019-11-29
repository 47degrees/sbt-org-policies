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

package sbtorgpolicies

import org.scalamock.scalatest.MockFactory

import scala.language.implicitConversions
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

trait TestOps extends AnyFunSuite with Matchers with Checkers with MockFactory {

  implicit def anyEq[A](a: A): AnyEq[A] = new AnyEq[A](a)

  final class AnyEq[A](a: A) {
    def shouldBeEq(b: A): Boolean = {
      a shouldBe b
      a == b
    }
  }

}
