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

package sbtorgpolicies.arbitraries

import org.scalacheck.{Arbitrary, Gen}
import sbtorgpolicies.model.License
import sbtorgpolicies.templates.badges.{BadgeInformation, ScalaJSBadge}

trait badgeArbitraries extends modelArbitratries {

  implicit val badgeInformationArbitrary: Arbitrary[BadgeInformation] =
    Arbitrary {
      for {
        owner              <- Arbitrary.arbitrary[String]
        repo               <- Arbitrary.arbitrary[String]
        branch             <- Arbitrary.arbitrary[String]
        sbtPlugin          <- Arbitrary.arbitrary[Boolean]
        libOrg             <- Arbitrary.arbitrary[String]
        libName            <- Arbitrary.arbitrary[String]
        libVersion         <- Arbitrary.arbitrary[String]
        scalaBinaryVersion <- Arbitrary.arbitrary[String]
        sbtBinaryVersion   <- Arbitrary.arbitrary[String]
        scalaJSV           <- Gen.option(scalaJSVArbitrary.arbitrary)
        license            <- Arbitrary.arbitrary[Option[License]]
      } yield
        BadgeInformation(
          owner,
          repo,
          branch,
          sbtPlugin,
          libOrg,
          libName,
          libVersion,
          scalaBinaryVersion,
          sbtBinaryVersion,
          scalaJSV,
          license)
    }

  val scalaJSVArbitrary: Arbitrary[String] = {
    val controlledArbitrary: Arbitrary[String] = Arbitrary {
      for {
        major <- Gen.oneOf(Seq(0))
        minor <- Gen.oneOf(Seq(6))
        patch <- Gen.chooseNum(0, 100)
      } yield s"$major.$minor.$patch"
    }

    val randomArbitrary: Arbitrary[String] = Arbitrary {
      for {
        major <- Gen.posNum[Int]
        minor <- Gen.posNum[Int]
        patch <- Gen.posNum[Int]
      } yield s"$major.$minor.$patch"
    }

    Arbitrary {
      Gen.oneOf[String](controlledArbitrary.arbitrary, randomArbitrary.arbitrary)
    }
  }

  implicit val scalaJSBadgeArbitrary: Arbitrary[ScalaJSBadge] =
    Arbitrary {
      for {
        info <- Arbitrary.arbitrary[BadgeInformation]
      } yield ScalaJSBadge(info)
    }

}
