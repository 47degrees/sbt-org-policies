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

import java.io.File

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.either._
import github4s.GithubResponses.{GHException, GHResponse, GHResult, UnexpectedException}
import github4s._
import github4s.algebras._
import github4s.domain._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.GitHubArbitraries._
import sbtorgpolicies.exceptions.{GitHubException, IOException, OrgPolicyException}
import sbtorgpolicies.io.FileReader

import scala.concurrent.ExecutionContext

class GitHubOpsTest extends TestOps {

  def newGitHubOps: (
      GitHubOps[IO],
      FileReader,
      GitData[IO],
      PullRequests[IO],
      Repositories[IO],
      Users[IO]
  ) = {

    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val t: Timer[IO]         = IO.timer(ExecutionContext.global)

    val fileReaderMock: FileReader = stub[FileReader]

    val ghGitData: GitData[IO]           = stub[GitData[IO]]
    val ghPullRequests: PullRequests[IO] = stub[PullRequests[IO]]
    val ghRepos: Repositories[IO]        = stub[Repositories[IO]]
    val ghUsers: Users[IO]               = stub[Users[IO]]

    val githubMock: Github[IO] = new Github[IO](None, None) {
      override lazy val users: Users[IO]               = ghUsers
      override lazy val repos: Repositories[IO]        = ghRepos
      override lazy val gitData: GitData[IO]           = ghGitData
      override lazy val pullRequests: PullRequests[IO] = ghPullRequests
    }

    val gitHubOps = new GitHubOps[IO](owner, repo, None, fileReaderMock) {
      override val gh: Github[IO] = githubMock
    }
    (gitHubOps, fileReaderMock, ghGitData, ghPullRequests, ghRepos, ghUsers)
  }

  def toLeftResult[T](e: GHException): Either[OrgPolicyException, T] =
    Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)): OrgPolicyException)

  test("GithubOps.fetchContributors works as expected") {
    val property = forAll(genSimpleAndFullUserLists) {
      case (list1: GHResponse[List[User]], list2: List[GHResponse[User]]) =>
        val (gitHubOps, _, _, _, ghRepos, ghUsers) = newGitHubOps
        (ghRepos.listContributors _)
          .when(*, *, *, *)
          .returns(IO.pure(list1))

        list1 match {
          case Right(r) =>
            r.result.zip(list2) foreach {
              case (user1, response) =>
                (ghUsers.get _)
                  .when(user1.login, *)
                  .returns(IO.pure(response))
            }
          case Left(_) =>
        }

        val result: EitherT[IO, OrgPolicyException, List[User]] =
          gitHubOps.fetchContributors.leftMap[OrgPolicyException](identity)

        (list1.left.toOption, list2.find(_.isLeft).flatMap(_.left.toOption)) match {
          case (Some(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Some(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case _ =>
            val resultList = list2.collect {
              case Right(gHResult) => gHResult.result
            }
            result.value.unsafeRunSync() shouldBeEq Right(resultList)
        }
    }

    check(property)
  }

  test("GithubOps.commitFiles works as expected when all files are updated") {

    val property = forAll {
      (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
        val (gitHubOps, fileReader, ghGitData, _, ghRepos, _) = newGitHubOps
        import sbtorgpolicies.io.syntax._
        filesAndContents foreach {
          case (s1, s2) =>
            (fileReader.getFileContent _)
              .when(baseDir.getAbsolutePath.ensureFinalSlash + s1)
              .returns(Right(s2))
        }

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefResponse))

        (ghGitData.getCommit _)
          .when(*, *, *, *)
          .returns(IO.pure(refCommitResponse))

        val maybeParentCommit: Option[String] =
          nelRefResponse.toOption.map(_.result.head.`object`.sha)

        val contents = filesAndContents.map {
          case (s1, s2) =>
            val content =
              Content(
                "file",
                Some("base64"),
                None,
                None,
                5432,
                s1,
                s1,
                Some(s2),
                s"sha-$s1",
                "",
                "",
                "",
                None
              )
            (s1, content)
        }

        contents foreach {
          case (s1, content) =>
            val response: GHResponse[NonEmptyList[Content]] =
              GHResult(NonEmptyList(content, Nil), 200, Map.empty).asRight
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit, *)
              .returns(IO.pure(response))
        }

        val result: EitherT[IO, OrgPolicyException, Option[Ref]] =
          gitHubOps.commitFiles(
            baseDir,
            branch,
            sampleMessage,
            filesAndContents.map(t => new File(baseDir, t._1))
          )

        (nelRefResponse, refCommitResponse) match {
          case (Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case _ =>
            result.value.unsafeRunSync() shouldBeEq Right(None)
        }

    }

    check(property)
  }

  test("GithubOps.commitFiles works as expected when some files need to be updated") {
    val property = forAll {
      (
          nelRefR: GHResponse[NonEmptyList[Ref]],
          refCommitR: GHResponse[RefCommit],
          treeResultR: GHResponse[TreeResult],
          createCommitR: GHResponse[RefCommit],
          updateReferenceR: GHResponse[Ref]
      ) =>
        val (gitHubOps, fileReader, ghGitData, _, ghRepos, _) = newGitHubOps
        import sbtorgpolicies.io.syntax._
        filesAndContents foreach {
          case (s1, s2) =>
            (fileReader.getFileContent _)
              .when(baseDir.getAbsolutePath.ensureFinalSlash + s1)
              .returns(Right(s2))
        }

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *, *)
          .returns(IO.pure(refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit, *)
              .returns(
                IO.pure(UnexpectedException("Not Found").asLeft)
              )
        }

        (ghGitData.createTree _)
          .when(*, *, *, *, *)
          .returns(IO.pure(treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *, *)
          .returns(IO.pure(createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *, *)
          .returns(IO.pure(updateReferenceR))

        val result: EitherT[IO, OrgPolicyException, Option[Ref]] =
          gitHubOps.commitFiles(
            baseDir,
            branch,
            sampleMessage,
            filesAndContents.map(t => new File(baseDir, t._1))
          )

        (nelRefR, refCommitR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _)
              if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case _ =>
            result shouldBeEq EitherT.rightT(None)
        }
    }

    check(property)
  }

  test("GithubOps.commitFiles should return an error when the file reader returns an error") {

    val (gitHubOps, fileReader, _, _, _, _) = newGitHubOps

    val ioException: IOException = IOException("Test error")

    (fileReader.getFileContent _).when(*).returns(ioException.asLeft)

    val result: EitherT[IO, OrgPolicyException, Option[Ref]] =
      gitHubOps.commitFiles(
        baseDir,
        branch,
        sampleMessage,
        filesAndContents.map(t => new File(baseDir, t._1))
      )

    result.value.unsafeRunSync() shouldBe ioException.asLeft

  }

  test("GithubOps.commitFilesAndContents works as expected when all files are updated") {
    val property = forAll {
      (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
        val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefResponse))

        (ghGitData.getCommit _)
          .when(*, *, *, *)
          .returns(IO.pure(refCommitResponse))

        val maybeParentCommit: Option[String] =
          nelRefResponse.toOption.map(_.result.head.`object`.sha)

        val contents = filesAndContents.map {
          case (s1, s2) =>
            val content =
              Content(
                "file",
                Some("base64"),
                None,
                None,
                5432,
                s1,
                s1,
                Some(s2),
                s"sha-$s1",
                "",
                "",
                "",
                None
              )
            (s1, content)
        }

        contents foreach {
          case (s1, content) =>
            val response: GHResponse[NonEmptyList[Content]] =
              GHResult(NonEmptyList(content, Nil), 200, Map.empty).asRight
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit, *)
              .returns(IO.pure(response))
        }

        val result: EitherT[IO, OrgPolicyException, Option[Ref]] =
          gitHubOps
            .commitFilesAndContents(branch, sampleMessage, filesAndContents)
            .leftMap[OrgPolicyException](identity)

        (nelRefResponse, refCommitResponse) match {
          case (Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case _ =>
            result shouldBeEq EitherT.rightT(None)
        }
    }

    check(property)
  }

  test("GithubOps.commitFilesAndContents works as expected when some files need to be updated") {
    val property = forAll {
      (
          nelRefR: GHResponse[NonEmptyList[Ref]],
          refCommitR: GHResponse[RefCommit],
          treeResultR: GHResponse[TreeResult],
          createCommitR: GHResponse[RefCommit],
          updateReferenceR: GHResponse[Ref]
      ) =>
        val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *, *)
          .returns(IO.pure(refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit, *)
              .returns(
                IO.pure(UnexpectedException("Not Found").asLeft)
              )
        }

        (ghGitData.createTree _)
          .when(*, *, *, *, *)
          .returns(IO.pure(treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *, *)
          .returns(IO.pure(createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *, *)
          .returns(IO.pure(updateReferenceR))

        val result: EitherT[IO, OrgPolicyException, Option[Ref]] =
          gitHubOps
            .commitFilesAndContents(branch, sampleMessage, filesAndContents)
            .leftMap[OrgPolicyException](identity)

        (nelRefR, refCommitR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _)
              if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case _ =>
            result shouldBeEq EitherT.rightT(None)
        }
    }

    check(property)
  }

  test("GithubOps.fetchReference works as expected") {
    val property = forAll { refResponse: GHResponse[NonEmptyList[Ref]] =>
      val (gitHubOps, _, ghGitData, _, _, _) = newGitHubOps

      (ghGitData.getReference _)
        .when(owner, repo, ref, *)
        .returns(IO.pure(refResponse))

      val result: EitherT[IO, OrgPolicyException, NonEmptyList[Ref]] =
        gitHubOps.fetchReference(ref).leftMap[OrgPolicyException](identity)

      refResponse match {
        case Left(e) =>
          result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
        case Right(gHResult) =>
          result.value.unsafeRunSync() shouldBeEq Right(gHResult.result)
      }

    }

    check(property)
  }

  test("GithubOps.commitDir works as expected") {
    val property = forAll {
      (
          refInfoR: GHResponse[RefInfo],
          refCommitR: GHResponse[RefCommit],
          treeResultR: GHResponse[TreeResult],
          nelRefR: GHResponse[NonEmptyList[Ref]],
          createCommitR: GHResponse[RefCommit],
          updateReferenceR: GHResponse[Ref]
      ) =>
        val (gitHubOps, fileReader, ghGitData, _, _, _) = newGitHubOps

        val files: List[(File, String)] = filesAndContents.map(t => (new File(baseDir, t._1), t._2))

        (fileReader.fetchDirsRecursively _)
          .when(*, *)
          .returns(files.map(_._1).asRight)

        files foreach {
          case (file, content) =>
            (fileReader.getFileBytes _).when(file).returns(content.getBytes.asRight)
        }

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *, *)
          .returns(IO.pure(refCommitR))

        (ghGitData.createBlob _)
          .when(*, *, *, *, *)
          .returns(IO.pure(refInfoR))

        (ghGitData.createTree _)
          .when(*, *, *, *, *)
          .returns(IO.pure(treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *, *)
          .returns(IO.pure(createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *, *)
          .returns(IO.pure(updateReferenceR))

        val result: EitherT[IO, OrgPolicyException, Ref] =
          gitHubOps.commitDir(branch, sampleMessage, baseDir)

        (nelRefR, refCommitR, refInfoR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _, _)
              if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, _, _, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, _, _, Right(r)) =>
            result shouldBeEq EitherT.rightT(r.result)
        }
    }
    check(property)

  }

  test("GithubOps.commitDir should return an error when the file reader returns an error") {
    val (gitHubOps, fileReader, _, _, _, _) = newGitHubOps

    val ioException: IOException = IOException("Test error")

    (fileReader.fetchDirsRecursively _).when(*, *).returns(ioException.asLeft)

    val result: EitherT[IO, OrgPolicyException, Ref] =
      gitHubOps.commitDir(branch, sampleMessage, baseDir)

    result.value.unsafeRunSync().isLeft shouldBe true
  }

  test("GithubOps.createTagRelease works as expected") {
    val property = forAll {
      (
          nelRefResponse: GHResponse[NonEmptyList[Ref]],
          tagResponse: GHResponse[Tag],
          refResponse: GHResponse[Ref],
          releaseResponse: GHResponse[Release]
      ) =>
        val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

        (ghGitData.getReference _)
          .when(*, *, *, *)
          .returns(IO.pure(nelRefResponse))

        (ghGitData.createTag _)
          .when(*, *, *, *, *, *, *, *)
          .returns(IO.pure(tagResponse))

        (ghGitData.createReference _)
          .when(*, *, *, *, *)
          .returns(IO.pure(refResponse))

        (ghRepos.createRelease _)
          .when(*, *, *, *, *, *, *, *, *)
          .returns(IO.pure(releaseResponse))

        val result: EitherT[IO, OrgPolicyException, Release] =
          gitHubOps
            .createTagRelease(branch, tag, sampleMessage, releaseDescription)
            .leftMap[OrgPolicyException](identity)

        (nelRefResponse, tagResponse, refResponse, releaseResponse) match {
          case (Left(e), _, _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _)
              if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, _, _, Right(gHResult)) =>
            result shouldBeEq EitherT.rightT(gHResult.result)
        }
    }

    check(property)
  }

  test(
    "GithubOps.latestPullRequests should return all merged pull requests if there isn't any commit"
  ) {

    val property = forAll { prResponse: GHResponse[List[PullRequest]] =>
      val (gitHubOps, _, _, ghPullRequests, ghRepos, _) = newGitHubOps

      (ghRepos.listCommits _)
        .when(*, *, *, *, *, *, *, *, *)
        .returns(IO.pure(Right(GHResult(Nil, 200, Map.empty))))

      (ghPullRequests.listPullRequests _)
        .when(*, *, *, *, *)
        .returns(IO.pure(prResponse))

      val result: EitherT[IO, OrgPolicyException, List[PullRequest]] =
        gitHubOps
          .latestPullRequests(branch, "", "")
          .leftMap[OrgPolicyException](identity)

      prResponse match {
        case Right(gHResult) =>
          result.map(_.toSet).value.unsafeRunSync() shouldBeEq Right(
            gHResult.result.filter(_.merged_at.nonEmpty).toSet
          )
        case Left(e) =>
          result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
      }
    }
    check(property)
  }

  test(
    "GithubOps.latestPullRequests should return all merged pull requests if there are commits but the message doesn't match"
  ) {

    val property = forAll {
      (prResponse: GHResponse[List[PullRequest]], commitsResponse: GHResponse[List[Commit]]) =>
        val (gitHubOps, _, _, ghPullRequests, ghRepos, _) = newGitHubOps

        (ghRepos.listCommits _)
          .when(*, *, *, *, *, *, *, *, *)
          .returns(IO.pure(commitsResponse))

        (ghPullRequests.listPullRequests _)
          .when(*, *, *, *, *)
          .returns(IO.pure(prResponse))

        val result: EitherT[IO, OrgPolicyException, List[PullRequest]] =
          gitHubOps
            .latestPullRequests(branch, "", nonExistingMessage)
            .leftMap[OrgPolicyException](identity)

        (commitsResponse, prResponse) match {
          case (Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(_), Right(gHResult)) =>
            result.map(_.toSet).value.unsafeRunSync() shouldBeEq Right(
              gHResult.result.filter(_.merged_at.nonEmpty).toSet
            )
        }
    }
    check(property)
  }

  test(
    "GithubOps.latestPullRequests should return all merged pull requests with date greater than the selected commit"
  ) {

    val property = forAll(genGHResPRListMergedFrom2015, ghResponseCommitListArbitrary.arbitrary) {
      (prResponse: GHResponse[List[PullRequest]], commitsResponse: GHResponse[List[Commit]]) =>
        val (gitHubOps, _, _, ghPullRequests, ghRepos, _) = newGitHubOps

        val commit = Commit(
          sha = "sha",
          message = nonExistingMessage,
          date = dateTimeFormat.print(date2015),
          url = "http://github.com",
          login = None,
          avatar_url = None,
          author_url = None
        )

        val newCommitsResponse = commitsResponse map { ghResult =>
          ghResult.copy(result = commit :: ghResult.result)
        }

        (ghRepos.listCommits _)
          .when(*, *, *, *, *, *, *, *, *)
          .returns(IO.pure(newCommitsResponse))

        (ghPullRequests.listPullRequests _)
          .when(*, *, *, *, *)
          .returns(IO.pure(prResponse))

        val result: EitherT[IO, OrgPolicyException, List[PullRequest]] =
          gitHubOps
            .latestPullRequests(branch, "", nonExistingMessage)
            .leftMap[OrgPolicyException](identity)

        (commitsResponse, prResponse) match {
          case (Left(e), _) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (_, Left(e)) =>
            result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
          case (Right(_), Right(gHResult)) =>
            val expected =
              gHResult.result
                .filter(_.merged_at.exists(s => dateTimeFormat.parseDateTime(s).isAfter(date2015)))
                .toSet
            result.map(_.toSet).value.unsafeRunSync() shouldBeEq Right(expected)
        }
    }
    check(property)
  }

  test("GithubOps.fetchHeadCommit works as expected") {
    val property = forAll { refResponse: GHResponse[NonEmptyList[Ref]] =>
      val (gitHubOps, _, ghGitData, _, _, _) = newGitHubOps

      (ghGitData.getReference _)
        .when(owner, repo, ref, *)
        .returns(IO.pure(refResponse))

      val result: EitherT[IO, OrgPolicyException, Ref] =
        gitHubOps.fetchHeadCommit(branch).leftMap[OrgPolicyException](identity)

      refResponse match {
        case Left(e) =>
          result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
        case Right(gHResult) if gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
          result.value.unsafeRunSync() shouldBeEq Right(gHResult.result.head)
        case _ =>
          val e = UnexpectedException(s"Branch $branch not found")
          result.value.unsafeRunSync() shouldBeEq toLeftResult(e)
      }
    }

    check(property)
  }
}
