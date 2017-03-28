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

package sbtorgpolicies.github

import cats.data.EitherT
import cats.implicits._
import github4s.Github
import github4s.Github._
import github4s.free.domain.User
import github4s.jvm.Implicits._
import sbtorgpolicies.exceptions.GitHubException
import sbtorgpolicies.github.instances._

import scala.util.{Failure, Success, Try}
import scalaj.http.HttpResponse

class GitHubOps(owner: String, repo: String, accessToken: Option[String]) {

  private[this] val gh = Github(accessToken)

  def fetchContributors: Either[GitHubException, List[User]] = {

    def fetchUserList: Github4sResponse[List[User]] =
      EitherT(gh.repos.listContributors(owner, repo))

    def fetchUserDetail(user: User): Github4sResponse[User] =
      EitherT(gh.users.get(user.login))

    val op = for {
      response         <- fetchUserList
      detailedResponse <- response.result.traverse(fetchUserDetail)
    } yield detailedResponse

    op.value.exec[Try, HttpResponse[String]](Map("user-agent" -> "sbt-org-policies")) match {
      case Success(Right(r)) => Right(r.result)
      case Success(Left(e))  => Left(GitHubException("GitHub returned an error", Some(e)))
      case Failure(e)        => Left(GitHubException("Error making request to GitHub", Some(e)))
    }
  }

  def commitFiles(filesAndContents: List[(String, String)]): Either[GitHubException, Unit] = ???

}
