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

  implicit def ghEitherTSyntax[A](eitherT: EitherTGHResult[A]): EitherTOps[A] = new EitherTOps[A](eitherT)

  implicit def ghResponseSyntax[A](ghResponse: GHIO[GHResponse[A]]): GHResponseOps[A] = new GHResponseOps(ghResponse)

  implicit def ghResultSyntax[A](gHResult: GHResult[A]): GHResultOps[A] = new GHResultOps[A](gHResult)

  final class EitherTOps[A](eitherT: EitherTGHResult[A]) {

    def execE: Either[GitHubException, A] =
      eitherT.value.execE

  }

  final class GHResponseOps[A](ghResponse: GHIO[GHResponse[A]]) {

    val commonHeaders: Map[String, String] = Map("user-agent" -> "sbt-org-policies")

    def execE: Either[GitHubException, A] = ghResponse.exec[Try, HttpResponse[String]](commonHeaders) match {
      case Success(Right(r)) => Right(r.result)
      case Success(Left(e))  => Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))
      case Failure(e)        => Left(GitHubException("Error making request to GitHub", Some(e)))
    }

  }

  final class GHResultOps[A](gHResult: GHResult[A]) {

    def map[B](f: A => B): GHResult[B] =
      gHResult.copy(result = f(gHResult.result))

  }
}
