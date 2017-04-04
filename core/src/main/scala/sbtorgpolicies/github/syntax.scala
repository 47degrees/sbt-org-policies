package sbtorgpolicies.github

import cats.data.EitherT
import github4s.Github._
import github4s.jvm.Implicits._
import github4s.GithubResponses.{GHException, GHIO, GHResponse, GHResult}
import sbtorgpolicies.exceptions.GitHubException
import sbtorgpolicies.github.instances._

import scala.util.{Failure, Success, Try}
import scalaj.http.HttpResponse

object syntax {

  private[this] val commonHeaders = Map("user-agent" -> "sbt-org-policies")

  implicit class EitherTOps[R](eitherT: EitherT[GHIO, GHException, GHResult[R]]) {

    def execE: Either[GitHubException, R] = eitherT.value.execE

  }

  implicit class GHIOOps[R](ghIO: GHIO[Either[GHException, GHResult[R]]]) {

    def execE: Either[GitHubException, R] =
      ghIO.exec[Try, HttpResponse[String]](commonHeaders).toEither

  }

  implicit class GHResponseTrySyntax[R](m: Try[GHResponse[R]]) {

    def toEither: Either[GitHubException, R] = m match {
      case Success(Right(r)) => Right(r.result)
      case Success(Left(e))  => Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))
      case Failure(e)        => Left(GitHubException("Error making request to GitHub", Some(e)))
    }

  }

  implicit def ghResultSyntax[A](gHResult: GHResult[A]): GHResultOps[A] = new GHResultOps[A](gHResult)

  final class GHResultOps[A](gHResult: GHResult[A]) {

    def map[B](f: A => B): GHResult[B] =
      gHResult.copy(result = f(gHResult.result))

  }
}
