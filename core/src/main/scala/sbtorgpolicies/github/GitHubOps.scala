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

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import github4s.Github
import github4s.GithubResponses._
import github4s.free.domain._
import sbtorgpolicies.exceptions.GitHubException
import sbtorgpolicies.github.instances._
import sbtorgpolicies.github.syntax._

class GitHubOps(owner: String, repo: String, accessToken: Option[String]) {

  private[this] val gh = Github(accessToken)

  def fetchContributors: Either[GitHubException, List[User]] = {

    def fetchUserList: Github4sResponse[List[User]] =
      EitherT(gh.repos.listContributors(owner, repo))

    def fetchUserDetail(user: User): Github4sResponse[User] =
      EitherT(gh.users.get(user.login))

    val op: EitherT[GHIO, GHException, GHResult[List[User]]] = for {
      response         <- fetchUserList
      detailedResponse <- response.result.traverse(fetchUserDetail)
    } yield detailedResponse

    op.execE
  }

  def commitFiles(
      owner: String,
      repo: String,
      branch: String,
      message: String,
      filesAndContents: List[(String, String)]): Either[GitHubException, Ref] = {

    def fetchBaseTreeSha(commitSha: String): Github4sResponse[RefCommit] =
      EitherT(gh.gitData.getCommit(owner, repo, commitSha))

    def createTree(baseTreeSha: String): Github4sResponse[TreeResult] = {

      val treeData = filesAndContents.map {
        case (path, content) => TreeDataBlob(path, "100644", "blob", content)
      }

      EitherT(gh.gitData.createTree(owner, repo, Some(baseTreeSha), treeData))
    }

    def createCommit(treeSha: String, baseCommitSha: String): Github4sResponse[RefCommit] =
      EitherT(gh.gitData.createCommit(owner, repo, message, treeSha, List(baseCommitSha)))

    def updateHead(commitSha: String) =
      EitherT(gh.gitData.updateReference(owner, repo, s"heads/$branch", commitSha))

    val op = for {
      gHResultParentCommit <- fetchHeadCommit(branch)
      parentCommitSha = gHResultParentCommit.result.`object`.sha
      gHResultBaseTree <- fetchBaseTreeSha(parentCommitSha)
      baseTreeSha = gHResultBaseTree.result.tree.sha
      ghResultTree   <- createTree(baseTreeSha)
      ghResultCommit <- createCommit(ghResultTree.result.sha, parentCommitSha)
      ghResultUpdate <- updateHead(ghResultCommit.result.sha)
    } yield ghResultUpdate

    op.execE
  }

  def fetchReference(ref: String): Either[GitHubException, NonEmptyList[Ref]] =
    gh.gitData
      .getReference(owner, repo, ref)
      .execE

  def createTagHeadCommit(branch: String, tag: String, comment: String): Either[GitHubException, Ref] = {

    def createTag(obj: RefObject): Github4sResponse[Tag] =
      EitherT(gh.gitData.createTag(owner, repo, tag, comment, obj.sha, obj.`type`))

    def createTagReference(commitSha: String) =
      EitherT(gh.gitData.createReference(owner, repo, s"refs/tags/$tag", commitSha))

    val op = for {
      headCommit  <- fetchHeadCommit(branch)
      tagResponse <- createTag(headCommit.result.`object`)
      reference   <- createTagReference(tagResponse.result.sha)
    } yield reference

    op.execE
  }

  private[this] def fetchHeadCommit(branch: String): Github4sResponse[Ref] = {

    def findReference(gHResult: GHResult[NonEmptyList[Ref]]): GHResponse[Ref] =
      gHResult.result.toList.find(_.ref == s"refs/heads/$branch") match {
        case Some(ref) =>
          Right(GHResult(ref, gHResult.statusCode, gHResult.headers))
        case None => Left(UnexpectedException(s"Branch $branch not found"))
      }

    val result: GHIO[GHResponse[Ref]] =
      gh.gitData.getReference(owner, repo, s"heads/$branch").map {
        case Right(r) => findReference(r)
        case Left(e)  => Left(e)
      }
    EitherT(result)
  }

}
