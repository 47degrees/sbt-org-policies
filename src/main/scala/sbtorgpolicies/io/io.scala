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

package sbtorgpolicies

import sbtorgpolicies.exceptions.IOException
import cats.syntax.either._
import scala.language.implicitConversions

package object io {

  type Replacements = Map[String, Replaceable]
  type IOResult[T]  = Either[IOException, T]

  trait Replaceable {

    def asString: String
  }

  case class ReplaceableT[T](t: T) extends Replaceable {
    override def asString: String = t.toString
  }

  case class ReplaceableList[T <: Replaceable](list: List[T]) extends Replaceable {
    override def asString: String = list.map(elem => s"* ${elem.asString}").mkString("\n")
  }

  object syntax {

    implicit def ioListSyntax[T <: Replaceable](list: List[T]): IOReplaceableListOps[T] =
      new IOReplaceableListOps(list)

    implicit def ioTSyntax[T](t: T): IOReplaceableOps[T] = new IOReplaceableOps(t)

    implicit def eitherFilterSyntax[T](either: Either[Throwable, T]): FilteredEitherOps[T] =
      new FilteredEitherOps(either)

    final class IOReplaceableOps[T](t: T) {

      def asReplaceable: ReplaceableT[T] = ReplaceableT(t)

    }

    final class IOReplaceableListOps[T <: Replaceable](list: List[T]) {

      def asReplaceable: ReplaceableList[T] = ReplaceableList[T](list)

    }

    final class FilteredEitherOps[T](either: Either[Throwable, T]) {

      def withFilter(f: T => Boolean): Either[Throwable, T] = either match {
        case Right(r) if !f(r) =>
          new IllegalStateException("Filter condition has not been satisfied").asLeft[T]
        case _ =>
          either
      }
    }
  }
}
