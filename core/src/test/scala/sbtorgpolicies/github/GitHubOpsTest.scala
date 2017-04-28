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

import java.io.File

import cats.data.NonEmptyList
import cats.free.Free
import cats.syntax.either._
import github4s.GithubResponses.{GHException, GHResponse, GHResult, UnexpectedException}
import github4s.app.GitHub4s
import github4s._
import github4s.free.domain._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalacheck.Prop._
import sbtorgpolicies.TestOps
import sbtorgpolicies.arbitraries.GitHubArbitraries._
import sbtorgpolicies.exceptions.{GitHubException, IOException, OrgPolicyException}
import sbtorgpolicies.io.FileReader

import scala.language.postfixOps

class GitHubOpsTest extends TestOps {

  val fileReaderMock: FileReader = mock[FileReader]

  val githubMock: Github             = mock[Github]
  val ghGitData: GHGitData           = mock[GHGitData]
  val ghPullRequests: GHPullRequests = mock[GHPullRequests]
  val ghRepos: GHRepos               = mock[GHRepos]
  val ghUsers: GHUsers               = mock[GHUsers]

  when(githubMock.gitData).thenReturn(ghGitData)
  when(githubMock.pullRequests).thenReturn(ghPullRequests)
  when(githubMock.repos).thenReturn(ghRepos)
  when(githubMock.users).thenReturn(ghUsers)

  val githubOps = new GitHubOps(owner, repo, None) {
    override val fileReader: FileReader = fileReaderMock
    override val gh: Github             = githubMock
  }

  def toLeftResult[T](e: GHException): Either[GitHubException, T] =
    Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))

  def resetMocks(): Unit = Mockito.reset(ghGitData, ghPullRequests, ghRepos, ghUsers, fileReaderMock)

  test("GithubOps.fetchContributors works as expected") {
    val property = forAll(genSimpleAndFullUserLists) {
      case (list1: GHResponse[List[User]], list2: List[GHResponse[User]]) =>
        resetMocks()

        when(ghRepos.listContributors(any[String], any[String], any[Option[String]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[List[User]]](list1))

        list1 match {
          case Right(r) =>
            r.result.zip(list2) foreach {
              case (user1, response) =>
                when(ghUsers.get(user1.login))
                  .thenReturn(Free.pure[GitHub4s, GHResponse[User]](response))
            }
          case Left(_) =>
        }

        val result: Either[GitHubException, List[User]] = githubOps.fetchContributors

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
    val property = forAll {
      (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
        resetMocks()

        import sbtorgpolicies.io.syntax._
        filesAndContents foreach {
          case (s1, s2) =>
            when(fileReaderMock.getFileContent(baseDir.getAbsolutePath.ensureFinalSlash + s1)).thenReturn(Right(s2))
        }

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

        when(ghGitData.getCommit(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitResponse))

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
            when(ghRepos.getContents(owner, repo, s1, maybeParentCommit))
              .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](response))
        }

        val result: Either[OrgPolicyException, Option[Ref]] =
          githubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

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
        resetMocks()

        import sbtorgpolicies.io.syntax._
        filesAndContents foreach {
          case (s1, s2) =>
            when(fileReaderMock.getFileContent(baseDir.getAbsolutePath.ensureFinalSlash + s1)).thenReturn(Right(s2))
        }

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        when(ghGitData.getCommit(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            when(ghRepos.getContents(owner, repo, s1, maybeParentCommit))
              .thenReturn(
                Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](UnexpectedException("Not Found").asLeft))
        }

        when(ghGitData.createTree(any[String], any[String], any[Option[String]], any[List[TreeData]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        when(ghGitData
          .createCommit(any[String], any[String], any[String], any[String], any[List[String]], any[Option[RefAuthor]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        when(ghGitData.updateReference(any[String], any[String], any[String], any[String], any[Option[Boolean]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Option[Ref]] =
          githubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

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

    resetMocks()

    val ioException: IOException = IOException("Test error")

    when(fileReaderMock.getFileContent(any[String])).thenReturn(ioException.asLeft)

    val result: Either[OrgPolicyException, Option[Ref]] =
      githubOps.commitFiles(baseDir, branch, sampleMessage, filesAndContents.map(t => new File(baseDir, t._1)))

    result shouldBe ioException.asLeft

  }

  test("GithubOps.commitFilesAndContents works as expected when all files are updated") {
    val property = forAll {
      (nelRefResponse: GHResponse[NonEmptyList[Ref]], refCommitResponse: GHResponse[RefCommit]) =>
        resetMocks()

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

        when(ghGitData.getCommit(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitResponse))

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
            when(ghRepos.getContents(owner, repo, s1, maybeParentCommit))
              .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](response))
        }

        val result: Either[OrgPolicyException, Option[Ref]] =
          githubOps.commitFilesAndContents(branch, sampleMessage, filesAndContents)

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
        resetMocks()

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        when(ghGitData.getCommit(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        val maybeParentCommit: Option[String] = nelRefR.toOption.map(_.result.head.`object`.sha)

        filesAndContents foreach {
          case (s1, _) =>
            when(ghRepos.getContents(owner, repo, s1, maybeParentCommit))
              .thenReturn(
                Free.pure[GitHub4s, GHResponse[NonEmptyList[Content]]](UnexpectedException("Not Found").asLeft))
        }

        when(ghGitData.createTree(any[String], any[String], any[Option[String]], any[List[TreeData]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        when(ghGitData
          .createCommit(any[String], any[String], any[String], any[String], any[List[String]], any[Option[RefAuthor]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        when(ghGitData.updateReference(any[String], any[String], any[String], any[String], any[Option[Boolean]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Option[Ref]] =
          githubOps.commitFilesAndContents(branch, sampleMessage, filesAndContents)

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
      resetMocks()

      when(ghGitData.getReference(any[String], any[String], any[String]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](refResponse))

      val result: Either[GitHubException, NonEmptyList[Ref]] = githubOps.fetchReference(ref)

      verify(ghGitData).getReference(owner, repo, ref)

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
        resetMocks()

        val files: List[(File, String)] = filesAndContents.map(t => (new File(baseDir, t._1), t._2))

        when(
          fileReaderMock
            .fetchDirsRecursively(any[List[File]], any[Function1[File, Boolean]].apply))
          .thenReturn(files.map(_._1).asRight)

        files foreach {
          case (file, content) =>
            when(fileReaderMock.getFileBytes(file)).thenReturn(content.getBytes.asRight)
        }

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefR))

        when(ghGitData.getCommit(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](refCommitR))

        when(ghGitData.createBlob(any[String], any[String], any[String], any[Option[String]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefInfo]](refInfoR))

        when(ghGitData.createTree(any[String], any[String], any[Option[String]], any[List[TreeData]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[TreeResult]](treeResultR))

        when(ghGitData
          .createCommit(any[String], any[String], any[String], any[String], any[List[String]], any[Option[RefAuthor]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[RefCommit]](createCommitR))

        when(ghGitData.updateReference(any[String], any[String], any[String], any[String], any[Option[Boolean]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Ref]](updateReferenceR))

        val result: Either[OrgPolicyException, Ref] =
          githubOps.commitDir(branch, sampleMessage, baseDir)

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

    resetMocks()

    val ioException: IOException = IOException("Test error")

    when(
      fileReaderMock
        .fetchDirsRecursively(any[List[File]], any[Function1[File, Boolean]].apply))
      .thenReturn(ioException.asLeft)

    val result: Either[OrgPolicyException, Ref] =
      githubOps.commitDir(branch, sampleMessage, baseDir)

    result.isLeft shouldBe true

  }

  test("GithubOps.createTagRelease works as expected") {
    val property = forAll {
      (
          nelRefResponse: GHResponse[NonEmptyList[Ref]],
          tagResponse: GHResponse[Tag],
          refResponse: GHResponse[Ref],
          releaseResponse: GHResponse[Release]) =>
        resetMocks()

        when(ghGitData.getReference(any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](nelRefResponse))

        when(
          ghGitData.createTag(
            any[String],
            any[String],
            any[String],
            any[String],
            any[String],
            any[String],
            any[Option[RefAuthor]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Tag]](tagResponse))

        when(ghGitData.createReference(any[String], any[String], any[String], any[String]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Ref]](refResponse))

        when(
          ghRepos.createRelease(
            any[String],
            any[String],
            any[String],
            any[String],
            any[String],
            any[Option[String]],
            any[Option[Boolean]],
            any[Option[Boolean]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[Release]](releaseResponse))

        val result: Either[GitHubException, Release] =
          githubOps.createTagRelease(branch, tag, sampleMessage, releaseDescription)

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
      resetMocks()

      when(
        ghRepos.listCommits(
          any[String],
          any[String],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[Pagination]]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[List[Commit]]](Right(GHResult(Nil, 200, Map.empty))))

      when(ghPullRequests.list(any[String], any[String], any[List[PRFilter]]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

      val result: Either[GitHubException, List[PullRequest]] = githubOps.latestPullRequests(branch, "", "")

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
      resetMocks()

      when(
        ghRepos.listCommits(
          any[String],
          any[String],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[Pagination]]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[List[Commit]]](commitsResponse))

      when(ghPullRequests.list(any[String], any[String], any[List[PRFilter]]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

      val result: Either[GitHubException, List[PullRequest]] =
        githubOps.latestPullRequests(branch, "", nonExistingMessage)

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

  test(
    "GithubOps.latestPullRequests should return all merged pull requests with date greater than the selected commit") {

    val property = forAll(genGHResPRListMergedFrom2015, ghResponseCommitListArbitrary.arbitrary) {
      (prResponse: GHResponse[List[PullRequest]], commitsResponse: GHResponse[List[Commit]]) =>
        resetMocks()

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

        when(
          ghRepos.listCommits(
            any[String],
            any[String],
            any[Option[String]],
            any[Option[String]],
            any[Option[String]],
            any[Option[String]],
            any[Option[String]],
            any[Option[Pagination]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[List[Commit]]](newCommitsResponse))

        when(ghPullRequests.list(any[String], any[String], any[List[PRFilter]]))
          .thenReturn(Free.pure[GitHub4s, GHResponse[List[PullRequest]]](prResponse))

        val result: Either[GitHubException, List[PullRequest]] =
          githubOps.latestPullRequests(branch, "", nonExistingMessage)

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
      resetMocks()

      import syntax._

      when(ghGitData.getReference(any[String], any[String], any[String]))
        .thenReturn(Free.pure[GitHub4s, GHResponse[NonEmptyList[Ref]]](refResponse))

      val result: Either[GitHubException, Ref] = githubOps.fetchHeadCommit(branch).execE

      verify(ghGitData).getReference(owner, repo, ref)

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
