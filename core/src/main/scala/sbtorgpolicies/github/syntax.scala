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

package sbtorgpolicies.github

import cats.data.EitherT
import github4s.Github._
import github4s.jvm.Implicits._
import github4s.GithubResponses._
import sbtorgpolicies.exceptions.GitHubException
import sbtorgpolicies.github.instances._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}
import scalaj.http.HttpResponse

object syntax {

  type EitherTGHResult[A] = EitherT[GHIO, GHException, GHResult[A]]

  implicit def ghEitherTSyntax[A](eitherT: EitherTGHResult[A]): EitherTOps[A] =
    new EitherTOps[A](eitherT)

  implicit def ghResponseSyntax[A](ghResponse: GHIO[GHResponse[A]]): GHResponseOps[A] =
    new GHResponseOps(ghResponse)

  implicit def ghResultSyntax[A](gHResult: GHResult[A]): GHResultOps[A] =
    new GHResultOps[A](gHResult)

  final class EitherTOps[A](eitherT: EitherTGHResult[A]) {

    def execE: Either[GitHubException, A] =
      eitherT.value.execE

  }

  final class GHResponseOps[A](ghResponse: GHIO[GHResponse[A]]) {

    val commonHeaders: Map[String, String] = Map("user-agent" -> "sbt-org-policies")

    def execE: Either[GitHubException, A] =
      ghResponse.exec[Try, HttpResponse[String]](commonHeaders) match {
        case Success(Right(r)) => Right(r.result)
        case Success(Left(e)) =>
          Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))
        case Failure(e) => Left(GitHubException("Error making request to GitHub", Some(e)))
      }

  }

  final class GHResultOps[A](gHResult: GHResult[A]) {

    def map[B](f: A => B): GHResult[B] =
      gHResult.copy(result = f(gHResult.result))

  }

  def newGHResult[T](v: T): GHResult[T] = GHResult(v, 200, Map.empty)
}
