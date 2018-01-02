/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.data.NonEmptyList
import cats.free.Free
import cats.syntax.either._
import github4s.GithubResponses.{GHException, GHResponse, GHResult, UnexpectedException}
import github4s.app.GitHub4s
import github4s._
import github4s.free.domain._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.GitHubArbitraries._
import sbtorgpolicies.exceptions.{GitHubException, IOException, OrgPolicyException}
import sbtorgpolicies.io.FileReader

class GitHubOpsTest extends TestOps {

  def newGitHubOps: (GitHubOps, FileReader, GHGitData, GHPullRequests, GHRepos, GHUsers) = {

    val fileReaderMock: FileReader = stub[FileReader]

    val ghGitData: GHGitData           = stub[GHGitData]
    val ghPullRequests: GHPullRequests = stub[GHPullRequests]
    val ghRepos: GHRepos               = stub[GHRepos]
    val ghUsers: GHUsers               = stub[GHUsers]

    val githubMock: Github = new Github() {
      override lazy val users: GHUsers               = ghUsers
      override lazy val repos: GHRepos               = ghRepos
      override lazy val gitData: GHGitData           = ghGitData
      override lazy val pullRequests: GHPullRequests = ghPullRequests
    }

    val gitHubOps = new GitHubOps(owner, repo, None, fileReaderMock) {
      override val gh: Github = githubMock
    }
    (gitHubOps, fileReaderMock, ghGitData, ghPullRequests, ghRepos, ghUsers)
  }

  def toLeftResult[T](e: GHException): Either[GitHubException, T] =
    Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))

  test("GithubOps.fetchContributors works as expected") {
    val property = forAll(genSimpleAndFullUserLists) {
      case (list1: GHResponse[List[User]], list2: List[GHResponse[User]]) =>
        val (gitHubOps, _, _, _, ghRepos, ghUsers) = newGitHubOps
        (ghRepos.listContributors _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[List[User]]](list1))

        list1 match {
          case Right(r) =>
            r.result.zip(list2) foreach {
              case (user1, response) =>
                (ghUsers.get _)
                  .when(user1.login)
                  .returns(Free.pure[GitHub4s, GHResponse[User]](response))
            }
          case Left(_) =>
        }

        val result: Either[GitHubException, List[User]] = gitHubOps.fetchContributors

        (list1.left.toOption, list2.find(_.isLeft).flatMap(_.left.toOption)) match {
          case (Some(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, Some(e)) =>
            result shouldBeEq toLeftResult(e)
          case _ =>
            val resultList = list2.collect {
              case Right(gHResult) => gHResult.result
            }
            result shouldBeEq Right(resultList)
        }
    }

    check(property)
  }

  test("GithubOps.commitFiles works as expected when all files are updated") {

    val property = forAll { (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
      val (gitHubOps, fileReader, ghGitData, _, ghRepos, _) = newGitHubOps
      import sbtorgpolicies.io.syntax._
      filesAndContents foreach {
        case (s1, s2) =>
          (fileReader.getFileContent _)
            .when(baseDir.getAbsolutePath.ensureFinalSlash + s1)
            .returns(Right(s2))
      }

      (ghGitData.getReference _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

      (ghGitData.getCommit _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitResponse))

      val maybeParentCommit: Option[String] = nelRefResponse.toOption.map(_.result.head.`object`.sha)

      val contents = filesAndContents.map {
        case (s1, s2) =>
          val content =
            Content("file", Some("base64"), None, None, 5432, s1, s1, Some(s2), s"sha-$s1", "", "", "", None)
          (s1, content)
      }

      contents foreach {
        case (s1, content) =>
          val response: GHResponse[NonEmptyList[Content]] =
            GHResult(NonEmptyList(content, Nil), 200, Map.empty).asRight
          (ghRepos.getContents _)
            .when(owner, repo, s1, maybeParentCommit)
            .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](response))
      }

      val result: Either[OrgPolicyException, Option[Ref]] =
        gitHubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

      (nelRefResponse, refCommitResponse) match {
        case (Left(e), _) =>
          result shouldBeEq toLeftResult(e)
        case (Right(gHResult), _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
          val e = UnexpectedException(s"Branch $branch not found")
          result shouldBeEq toLeftResult(e)
        case (_, Left(e)) =>
          result shouldBeEq toLeftResult(e)
        case _ =>
          result shouldBeEq Right(None)
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
          updateReferenceR: GHResponse[Ref]) =>
        val (gitHubOps, fileReader, ghGitData, _, ghRepos, _) = newGitHubOps
        import sbtorgpolicies.io.syntax._
        filesAndContents foreach {
          case (s1, s2) =>
            (fileReader.getFileContent _)
              .when(baseDir.getAbsolutePath.ensureFinalSlash + s1)
              .returns(Right(s2))
        }

        (ghGitData.getReference _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit)
              .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](UnexpectedException("Not Found").asLeft))
        }

        (ghGitData.createTree _)
          .when(*, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Option[Ref]] =
          gitHubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

        (nelRefR, refCommitR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e)) =>
            result shouldBeEq toLeftResult(e)
          case _ =>
            result shouldBeEq Right(None)
        }
    }

    check(property)
  }

  test("GithubOps.commitFiles should return an error when the file reader returns an error") {

    val (gitHubOps, fileReader, _, _, _, _) = newGitHubOps

    val ioException: IOException = IOException("Test error")

    (fileReader.getFileContent _).when(*).returns(ioException.asLeft)

    val result: Either[OrgPolicyException, Option[Ref]] =
      gitHubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

    result shouldBe ioException.asLeft

  }

  test("GithubOps.commitFilesAndContents works as expected when all files are updated") {
    val property = forAll { (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
      val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

      (ghGitData.getReference _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

      (ghGitData.getCommit _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitResponse))

      val maybeParentCommit: Option[String] = nelRefResponse.toOption.map(_.result.head.`object`.sha)

      val contents = filesAndContents.map {
        case (s1, s2) =>
          val content =
            Content("file", Some("base64"), None, None, 5432, s1, s1, Some(s2), s"sha-$s1", "", "", "", None)
          (s1, content)
      }

      contents foreach {
        case (s1, content) =>
          val response: GHResponse[NonEmptyList[Content]] =
            GHResult(NonEmptyList(content, Nil), 200, Map.empty).asRight
          (ghRepos.getContents _)
            .when(owner, repo, s1, maybeParentCommit)
            .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](response))
      }

      val result: Either[OrgPolicyException, Option[Ref]] =
        gitHubOps.commitFilesAndContents(branch, sampleMessage, filesAndContents)

      (nelRefResponse, refCommitResponse) match {
        case (Left(e), _) =>
          result shouldBeEq toLeftResult(e)
        case (Right(gHResult), _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
          val e = UnexpectedException(s"Branch $branch not found")
          result shouldBeEq toLeftResult(e)
        case (_, Left(e)) =>
          result shouldBeEq toLeftResult(e)
        case _ =>
          result shouldBeEq Right(None)
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
          updateReferenceR: GHResponse[Ref]) =>
        val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

        (ghGitData.getReference _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            (ghRepos.getContents _)
              .when(owner, repo, s1, maybeParentCommit)
              .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](UnexpectedException("Not Found").asLeft))
        }

        (ghGitData.createTree _)
          .when(*, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Option[Ref]] =
          gitHubOps.commitFilesAndContents(branch, sampleMessage, filesAndContents)

        (nelRefR, refCommitR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e)) =>
            result shouldBeEq toLeftResult(e)
          case _ =>
            result shouldBeEq Right(None)
        }
    }

    check(property)
  }

  test("GithubOps.fetchReference works as expected") {
    val property = forAll { refResponse: GHResponse[NonEmptyList[Ref]] =>
      val (gitHubOps, _, ghGitData, _, _, _) = newGitHubOps

      (ghGitData.getReference _)
        .when(owner, repo, ref)
        .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](refResponse))

      val result: Either[GitHubException, NonEmptyList[Ref]] = gitHubOps.fetchReference(ref)

      refResponse match {
        case Left(e) =>
          result shouldBeEq toLeftResult(e)
        case Right(gHResult) =>
          result shouldBeEq Right(gHResult.result)
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
          updateReferenceR: GHResponse[Ref]) =>
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
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        (ghGitData.getCommit _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        (ghGitData.createBlob _)
          .when(*, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefInfo]](refInfoR))

        (ghGitData.createTree _)
          .when(*, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        (ghGitData.createCommit _)
          .when(*, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        (ghGitData.updateReference _)
          .when(*, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Ref] =
          gitHubOps.commitDir(branch, sampleMessage, baseDir)

        (nelRefR, refCommitR, refInfoR, treeResultR, createCommitR, updateReferenceR) match {
          case (Left(e), _, _, _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _, _, _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e), _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, _, Left(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, _, _, Left(e)) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, _, _, Right(r)) =>
            result shouldBeEq r.result.asRight
        }
    }
    check(property)

  }

  test("GithubOps.commitDir should return an error when the file reader returns an error") {

    val (gitHubOps, fileReader, _, _, _, _) = newGitHubOps

    val ioException: IOException = IOException("Test error")

    (fileReader.fetchDirsRecursively _).when(*, *).returns(ioException.asLeft)

    val result: Either[OrgPolicyException, Ref] =
      gitHubOps.commitDir(branch, sampleMessage, baseDir)

    result.isLeft shouldBe true

  }

  test("GithubOps.createTagRelease works as expected") {
    val property = forAll {
      (
          nelRefResponse: GHResponse[NonEmptyList[Ref]],
          tagResponse: GHResponse[Tag],
          refResponse: GHResponse[Ref],
          releaseResponse: GHResponse[Release]) =>
        val (gitHubOps, _, ghGitData, _, ghRepos, _) = newGitHubOps

        (ghGitData.getReference _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

        (ghGitData.createTag _)
          .when(*, *, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Tag]](tagResponse))

        (ghGitData.createReference _)
          .when(*, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Ref]](refResponse))

        (ghRepos.createRelease _)
          .when(*, *, *, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[Release]](releaseResponse))

        val result: Either[GitHubException, Release] =
          gitHubOps.createTagRelease(branch, tag, sampleMessage, releaseDescription)

        (nelRefResponse, tagResponse, refResponse, releaseResponse) match {
          case (Left(e), _, _, _) =>
            result shouldBeEq toLeftResult(e)
          case (Right(gHResult), _, _, _) if !gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
            val e = UnexpectedException(s"Branch $branch not found")
            result shouldBeEq toLeftResult(e)
          case (_, Left(e), _, _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, Left(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, Left(e)) =>
            result shouldBeEq toLeftResult(e)
          case (_, _, _, Right(gHResult)) =>
            result shouldBeEq Right(gHResult.result)
        }
    }

    check(property)
  }

  test("GithubOps.latestPullRequests should return all merged pull requests if there isn't any commit") {

    val property = forAll { prResponse: GHResponse[List[PullRequest]] =>
      val (gitHubOps, _, _, ghPullRequests, ghRepos, _) = newGitHubOps

      (ghRepos.listCommits _)
        .when(*, *, *, *, *, *, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[List[Commit]]](Right(GHResult(Nil, 200, Map.empty))))

      (ghPullRequests.list _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

      val result: Either[GitHubException, List[PullRequest]] = gitHubOps.latestPullRequests(branch, "", "")

      prResponse match {
        case Right(gHResult) =>
          result.map(_.toSet) shouldBeEq Right(gHResult.result.filter(_.merged_at.nonEmpty).toSet)
        case Left(e) =>
          result shouldBeEq toLeftResult(e)
      }
    }
    check(property)
  }

  test(
    "GithubOps.latestPullRequests should return all merged pull requests if there are commits but the message doesn't match") {

    val property = forAll { (prResponse: GHResponse[List[PullRequest]], commitsResponse: GHResponse[List[Commit]]) =>
      val (gitHubOps, _, _, ghPullRequests, ghRepos, _) = newGitHubOps

      (ghRepos.listCommits _)
        .when(*, *, *, *, *, *, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[List[Commit]]](commitsResponse))

      (ghPullRequests.list _)
        .when(*, *, *)
        .returns(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

      val result: Either[GitHubException, List[PullRequest]] =
        gitHubOps.latestPullRequests(branch, "", nonExistingMessage)

      (commitsResponse, prResponse) match {
        case (Left(e), _) =>
          result shouldBeEq toLeftResult(e)
        case (_, Left(e)) =>
          result shouldBeEq toLeftResult(e)
        case (Right(_), Right(gHResult)) =>
          result.map(_.toSet) shouldBeEq Right(gHResult.result.filter(_.merged_at.nonEmpty).toSet)
      }
    }
    check(property)
  }

  test("GithubOps.latestPullRequests should return all merged pull requests with date greater than the selected commit") {

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
          author_url = None)

        val newCommitsResponse = commitsResponse map { ghResult =>
          ghResult.copy(result = commit :: ghResult.result)
        }

        (ghRepos.listCommits _)
          .when(*, *, *, *, *, *, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[List[Commit]]](newCommitsResponse))

        (ghPullRequests.list _)
          .when(*, *, *)
          .returns(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

        val result: Either[GitHubException, List[PullRequest]] =
          gitHubOps.latestPullRequests(branch, "", nonExistingMessage)

        (commitsResponse, prResponse) match {
          case (Left(e), _) =>
            result shouldBeEq toLeftResult(e)
          case (_, Left(e)) =>
            result shouldBeEq toLeftResult(e)
          case (Right(_), Right(gHResult)) =>
            val expected =
              gHResult.result.filter(_.merged_at.exists(s => dateTimeFormat.parseDateTime(s).isAfter(date2015))).toSet
            result.map(_.toSet) shouldBeEq Right(expected)
        }
    }
    check(property)
  }

  test("GithubOps.fetchHeadCommit works as expected") {
    val property = forAll { refResponse: GHResponse[NonEmptyList[Ref]] =>
      val (gitHubOps, _, ghGitData, _, _, _) = newGitHubOps

      import syntax._

      (ghGitData.getReference _)
        .when(owner, repo, ref)
        .returns(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](refResponse))

      val result: Either[GitHubException, Ref] = gitHubOps.fetchHeadCommit(branch).execE

      refResponse match {
        case Left(e) =>
          result shouldBeEq toLeftResult(e)
        case Right(gHResult) if gHResult.result.exists(_.ref == s"refs/heads/$branch") =>
          result shouldBeEq Right(gHResult.result.head)
        case _ =>
          val e = UnexpectedException(s"Branch $branch not found")
          result shouldBeEq toLeftResult(e)
      }
    }

    check(property)
  }

}
