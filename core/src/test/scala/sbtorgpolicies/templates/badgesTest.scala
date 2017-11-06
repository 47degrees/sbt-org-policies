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

import org.scalacheck.Prop.forAll
import sbtorgpolicies.TestOps
import sbtorgpolicies.templates.badges.ScalaJSBadge
import sbtorgpolicies.arbitraries.badgeArbitraries

class badgesTest extends TestOps with badgeArbitraries {

  test("ScalaJSBadge badgeIcon works as expected for semantic versions") {

    val property = forAll { (scalaJSBadge: ScalaJSBadge) =>
      val result: Option[String] = scalaJSBadge.badgeIcon.map(_.icon)

      {
        scalaJSBadge.info.scalaJSV match {
          case Some(version) =>
            result.map(_.contains {
              version match {
                case "0.6.17" | "0.6.18" | "0.6.19" | "0.6.20" =>
                  "0.6.17"
                case "0.6.21" =>
                  "1.0.0-M1"
                case _ =>
                  version
              }
            })
          case None => Some(true)
        }
      }.fold(false)(identity)
    }

    check(property, MinSuccessful(500))
  }

}
