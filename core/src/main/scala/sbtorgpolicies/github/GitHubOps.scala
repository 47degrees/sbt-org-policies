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
import cats.effect.{ConcurrentEffect, Sync, Timer}
import cats.implicits._
import com.github.marklister.base64.Base64._
import github4s.Github
import github4s.GithubResponses._
import github4s.domain._
import sbtorgpolicies.exceptions.{GitHubException, IOException, OrgPolicyException}
import sbtorgpolicies.github.config._
import sbtorgpolicies.io.{FileReader, IO => FIO, IOResult}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class GitHubOps[F[_]: ConcurrentEffect: Timer](
    owner: String,
    repo: String,
    accessToken: Option[String],
    fileReader: FileReader = FileReader
)(implicit ec: ExecutionContext) {

  val gh: Github[F] = Github[F](accessToken)
  val headers: Map[String, String] = Map("user-agent" -> "sbt-org-policies")

  def fetchContributors: EitherT[F, GitHubException, List[User]] = {
    def fetchUserList: EitherT[F, GitHubException, List[User]] =
      run(gh.repos.listContributors(owner, repo, headers = headers))

    def fetchUserDetail(user: User): EitherT[F, GitHubException, User] =
      run(gh.users.get(user.login, headers = headers))

    for {
      response         <- fetchUserList
      detailedResponse <- response.traverse(fetchUserDetail)
    } yield detailedResponse
  }

  def commitFiles(
      baseDir: File,
      branch: String,
      message: String,
      files: List[File]
  ): EitherT[F, OrgPolicyException, Option[Ref]] = {

    def relativePath(file: File): String = FIO.relativize(baseDir, file).getOrElse(file.getName)

    def readFileContents: IOResult[List[(String, String)]] =
      files.traverse { file =>
        fileReader.getFileContent(file.getAbsolutePath).tupleLeft(relativePath(file))
      }

    EitherT.fromEither[F](readFileContents)
      .leftMap[OrgPolicyException](identity)
      .flatMap { filesAndContents =>
        commitFilesAndContents(branch, message, filesAndContents)
          .leftMap[OrgPolicyException](identity)
      }
  }

  def commitFilesAndContents(
      branch: String,
      message: String,
      filesAndContents: List[(String, String)]
  ): EitherT[F, GitHubException, Option[Ref]] = {

    def fetchBaseTreeSha(commitSha: String): EitherT[F, GitHubException, RefCommit] =
      run(gh.gitData.getCommit(owner, repo, commitSha))

    def fetchFilesContents(commitSha: String): EitherT[F, GitHubException, List[(String, Option[String])]] = {
      def fetchFileContents(
          path: String,
          commitSha: String
      ): EitherT[F, GitHubException, (String, Option[String])] =
        run(
          gh.repos.getContents(owner = owner, repo = repo, path = path, ref = Some(commitSha))
        ).map(res => res.map(content => path -> content.content).head)

      filesAndContents.map(_._1).traverse(fetchFileContents(_, commitSha))
    }

    def filterNonChangedFiles(remote: List[(String, Option[String])]): List[(String, String)] = {
      val remoteMap = remote.collect {
        case (path, Some(c)) => path -> c
      }.toMap
      filesAndContents.filterNot {
        case (path, content) =>
          remoteMap.get(path).exists { remoteContent =>
            remoteContent.trim.replaceAll("\n", "") == content.getBytes.toBase64.trim
          }
      }
    }

    def createTree(
        baseTreeSha: String,
        filteredFilesContent: List[(String, String)]
    ): EitherT[F, GitHubException, TreeResult] = {
      val treeData: List[TreeDataBlob] = filteredFilesContent.map {
        case (path, content) => TreeDataBlob(path, blobMode, blobType, content)
      }

      run(gh.gitData.createTree(owner, repo, Some(baseTreeSha), treeData))
    }

    def createCommit(treeSha: String, baseCommitSha: String): EitherT[F, GitHubException, RefCommit] =
      run(gh.gitData.createCommit(owner, repo, message, treeSha, List(baseCommitSha), None))

    def commitFilesIfChanged(
        baseTreeSha: String,
        parentCommitSha: String,
        filteredFilesContent: List[(String, String)]
    ): EitherT[F, GitHubException, Option[Ref]] =
      filteredFilesContent match {
        case Nil =>
          EitherT.rightT(none[Ref])
        case list =>
          for {
            ghResultTree   <- createTree(baseTreeSha, list)
            ghResultCommit <- createCommit(ghResultTree.sha, parentCommitSha)
            ghResultUpdate <- updateHead(branch, ghResultCommit.sha)
          } yield Option(ghResultUpdate)
      }

    for {
      gHResultParentCommit <- fetchHeadCommit(branch)
      parentCommitSha = gHResultParentCommit.`object`.sha
      gHResultBaseTree <- fetchBaseTreeSha(parentCommitSha)
      baseTreeSha = gHResultBaseTree.tree.sha
      ghResultFilesContent <- fetchFilesContents(parentCommitSha)
      ghResultUpdate <- commitFilesIfChanged(
        baseTreeSha,
        parentCommitSha,
        filterNonChangedFiles(ghResultFilesContent)
      )
    } yield ghResultUpdate
  }

  def commitDir(branch: String, message: String, dir: File): EitherT[F, OrgPolicyException, Ref] =
    fileReader.fetchDirsRecursively(List(dir)) match {
      case Right(Nil) => EitherT.leftT(IOException(s"Nothing to commit in dir ${dir.getAbsolutePath}"))
      case Right(head :: list) => commitDir(branch, message, dir, NonEmptyList(head, list))
        .leftMap[OrgPolicyException](identity)
      case Left(e) => EitherT.leftT(e)
    }

  def commitDir(
      branch: String,
      message: String,
      baseDir: File,
      dirList: NonEmptyList[File],
      blobConfig: BlobConfig = defaultBlobConfig
  ): EitherT[F, GitHubException, Ref] = {

    def updateCommitDirH(dir: File, sha: Option[String]): EitherT[F, GitHubException, RefCommit] =
      updateCommitDir(branch, message, baseDir, dir, blobConfig, sha)

    val processAllFiles: EitherT[F, GitHubException, RefCommit] =
      dirList.reduceLeftM(dirHead => updateCommitDirH(dirHead, None)) { (commit, dir) =>
        updateCommitDirH(dir, Some(commit.sha))
      }

    for {
      lastCommit <- processAllFiles
      headRef    <- updateHead(branch, lastCommit.sha)
    } yield headRef
  }

  private[this] def updateCommitDir(
      branch: String,
      message: String,
      baseDir: File,
      dirToCommit: File,
      blobConfig: BlobConfig,
      commitSha: Option[String]
  ): EitherT[F, GitHubException, RefCommit] = {

    def fetchBaseTreeSha: EitherT[F, GitHubException, Option[RefCommit]] =
      commitSha.map { sha =>
        run(gh.gitData.getCommit(owner, repo, sha)).map(Option.apply)
      }.getOrElse(EitherT.rightT(none[RefCommit]))

    def getAllFiles: List[File] = Option(dirToCommit.listFiles()).toList.flatten.filter(_.isFile)

    def createTreeDataList(files: List[File]): EitherT[F, GitHubException, List[TreeData]] = {

      def readFileAsGithub4sResponse(file: File): EitherT[F, GitHubException, Array[Byte]] =
        EitherT.fromEither[F] {
          fileReader
            .getFileBytes(file)
            .leftMap(e => GitHubException(e.getMessage))
        }

      def path(file: File): EitherT[F, GitHubException, String] =
        EitherT.fromOption[F](
          FIO.relativize(baseDir, file),
          GitHubException(s"Can't determine path for ${file.getAbsolutePath}")
        )

      def createTreeDataSha(filePath: String, array: Array[Byte]): EitherT[F, GitHubException, TreeData] =
        for {
          gh <- EitherT.right(ghWithRateLimit)
          res <- run(gh.gitData.createBlob(owner, repo, array.toBase64, Some("base64")))
            .map(refInfo => TreeDataSha(filePath, blobMode, blobType, refInfo.sha))
        } yield res

      def createTreeDataBlob(filePath: String, array: Array[Byte]): EitherT[F, GitHubException, TreeData] =
        EitherT.rightT(TreeDataBlob(filePath, blobMode, blobType, new String(array)): TreeData)

      def createTreeData(
          file: File,
          filePath: String,
          array: Array[Byte]
      ): EitherT[F, GitHubException, TreeData] =
        if (blobConfig.acceptedExtensions.exists(s => file.getName.toLowerCase.endsWith(s)) &&
            array.length < blobConfig.maximumSize) {
          createTreeDataBlob(filePath, array)
        } else {
          createTreeDataSha(filePath, array)
        }

      def processFile(file: File): EitherT[F, GitHubException, TreeData] =
        for {
          filePath <- path(file)
          array    <- readFileAsGithub4sResponse(file)
          treeData <- createTreeData(file, filePath, array)
        } yield treeData

      files.traverse(processFile)
    }

    def createTree(
        baseTreeSha: Option[String],
        treeData: List[TreeData]
    ): EitherT[F, GitHubException, TreeResult] =
      for {
        gh <- EitherT.right(ghWithRateLimit)
        res <- run(gh.gitData.createTree(owner, repo, baseTreeSha, treeData))
      } yield res

    def createCommit(treeSha: String, parentCommit: String): EitherT[F, GitHubException, RefCommit] =
      for {
        gh <- EitherT.right(ghWithRateLimit)
        res <- run(gh.gitData.createCommit(owner, repo, message, treeSha, List(parentCommit), author = None))
      } yield res

    def parentCommitSha: EitherT[F, GitHubException, String] = commitSha match {
      case Some(sha) => EitherT.rightT(sha)
      case None => fetchHeadCommit(branch).map(_.`object`.sha)
    }

    for {
      parentCommit <- parentCommitSha
      baseTree     <- fetchBaseTreeSha
      treeDataList <- createTreeDataList(getAllFiles)
      treeResult   <- createTree(baseTree.map(_.tree.sha), treeDataList)
      refCommit    <- createCommit(treeResult.sha, parentCommit)
    } yield refCommit
  }

  def fetchReference(ref: String): EitherT[F, GitHubException, NonEmptyList[Ref]] =
    run(gh.gitData.getReference(owner, repo, ref))

  def createTagRelease(
      branch: String,
      tag: String,
      message: String,
      releaseDescription: String
  ): EitherT[F, GitHubException, Release] = {
    def createTag(obj: RefObject): EitherT[F, GitHubException, Tag] =
      run(gh.gitData.createTag(owner, repo, tag, message, obj.sha, obj.`type`))

    def createTagReference(commitSha: String): EitherT[F, GitHubException, Ref] =
      run(gh.gitData.createReference(owner, repo, s"refs/tags/$tag", commitSha))

    def createRelease: EitherT[F, GitHubException, Release] =
      run(gh.repos.createRelease(owner, repo, tag, tag, releaseDescription))

    for {
      headCommit  <- fetchHeadCommit(branch)
      tagResponse <- createTag(headCommit.`object`)
      _           <- createTagReference(tagResponse.sha)
      release     <- createRelease
    } yield release
  }

  def latestPullRequests(
      branch: String,
      inPath: String,
      message: String
  ): EitherT[F, GitHubException, List[PullRequest]] = {

    def fetchLastCommit: EitherT[F, GitHubException, Option[Commit]] = {
      def findCommit(list: List[Commit]): Option[Commit] =
        list.sortBy(_.date)(Ordering[String].reverse).find(_.message.contains(message))

      run(gh.repos.listCommits(owner = owner, repo = repo, path = Some(inPath)))
        .map(findCommit)
    }

    def fetchPullRequests(maybeDate: Option[String]): EitherT[F, GitHubException, List[PullRequest]] = {
      def orderAndFilter(list: List[PullRequest]): List[PullRequest] = {
        val date = maybeDate.getOrElse("")
        list.mapFilter(pr => pr.merged_at.filter(_ > date).as(pr)).reverse
      }

      val filters =
        List(PRFilterClosed, PRFilterBase(branch), PRFilterSortUpdated, PRFilterOrderDesc)
      run(gh.pullRequests.listPullRequests(owner, repo, filters)).map(orderAndFilter)
    }

    for {
      maybeCommit <- fetchLastCommit
      list        <- fetchPullRequests(maybeCommit.map(_.date))
    } yield list
  }

  def fetchHeadCommit(branch: String): EitherT[F, GitHubException, Ref] = {
    def findReference(refs: NonEmptyList[Ref]): EitherT[F, GitHubException, Ref] =
      refs.find(_.ref == s"refs/heads/$branch") match {
        case Some(ref) => EitherT.rightT(ref)
        case None      => EitherT.leftT(GitHubException(s"Branch $branch not found"))
      }

    run(gh.gitData.getReference(owner, repo, s"heads/$branch")).flatMap(findReference)
  }

  def updateHead(branch: String, commitSha: String): EitherT[F, GitHubException, Ref] =
    run(gh.gitData.updateReference(owner, repo, s"heads/$branch", commitSha, force = false))

  def run[A](f: F[GHResponse[A]]): EitherT[F, GitHubException, A] = EitherT(
    Sync[F].attempt(f).map {
      case Right(Right(r)) => Right(r.result)
      case Right(Left(e)) =>
        Left(GitHubException(s"GitHub returned an error: ${e.getMessage}", Some(e)))
      case Left(e) => Left(GitHubException("Error making request to GitHub", Some(e)))
    }
  )

  // Due to GitHub abuse rate limits, we should wait 1 sec between each request
  // https://developer.github.com/guides/best-practices-for-integrators/#dealing-with-abuse-rate-limits
  def ghWithRateLimit: F[Github[F]] = Timer[F].sleep(1.second).as(gh)
}
