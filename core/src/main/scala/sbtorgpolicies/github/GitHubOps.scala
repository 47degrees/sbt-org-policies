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

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import com.github.marklister.base64.Base64._
import github4s.Github
import github4s.GithubResponses._
import github4s.free.domain._
import sbtorgpolicies.exceptions.{GitHubException, IOException, OrgPolicyException}
import sbtorgpolicies.github.config._
import sbtorgpolicies.github.instances._
import sbtorgpolicies.github.syntax._
import sbtorgpolicies.io.syntax._
import sbtorgpolicies.io.{FileReader, IO, IOResult}

class GitHubOps(owner: String, repo: String, accessToken: Option[String], fileReader: FileReader = FileReader) {

  val gh: Github = Github(accessToken)

  def fetchContributors: Either[GitHubException, List[User]] = {

    def fetchUserList: Github4sResponse[List[User]] =
      EitherT(gh.repos.listContributors(owner, repo))

    def fetchUserDetail(user: User): Github4sResponse[User] =
      EitherT(gh.users.get(user.login))

    val op: Github4sResponse[List[User]] = for {
      response         <- fetchUserList
      detailedResponse <- response.result.traverse(fetchUserDetail)
    } yield detailedResponse

    op.execE
  }

  def commitFiles(
      baseDir: File,
      branch: String,
      message: String,
      files: List[File]): Either[OrgPolicyException, Option[Ref]] = {

    def relativePath(file: File): String = IO.relativize(baseDir, file).getOrElse(file.getName)

    def readFileContents: IOResult[List[(String, String)]] =
      files.traverse { file =>
        fileReader.getFileContent(file.getAbsolutePath).tupleLeft(relativePath(file))
      }

    readFileContents.leftMap[OrgPolicyException](identity).flatMap { filesAndContents =>
      commitFilesAndContents(branch, message, filesAndContents)
    }

  }

  def commitFilesAndContents(
      branch: String,
      message: String,
      filesAndContents: List[(String, String)]): Either[OrgPolicyException, Option[Ref]] = {

    def fetchBaseTreeSha(commitSha: String): Github4sResponse[RefCommit] =
      EitherT(gh.gitData.getCommit(owner, repo, commitSha))

    def fetchFilesContents(commitSha: String): Github4sResponse[List[(String, Option[String])]] = {

      def fetchFileContents(path: String, commitSha: String): Github4sResponse[(String, Option[String])] =
        EitherT(gh.repos.getContents(owner = owner, repo = repo, path = path, ref = Some(commitSha)))
          .map(ghRes => ghRes.map(contents => path -> contents.head.content))
          .orElse((path, none[String]).pure[Github4sResponse])

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

    def createTree(baseTreeSha: String, filteredFilesContent: List[(String, String)]): Github4sResponse[TreeResult] = {

      def treeData: List[TreeDataBlob] = filteredFilesContent.map {
        case (path, content) => TreeDataBlob(path, blobMode, blobType, content)
      }

      EitherT(gh.gitData.createTree(owner, repo, Some(baseTreeSha), treeData))
    }

    def createCommit(treeSha: String, baseCommitSha: String): Github4sResponse[RefCommit] =
      EitherT(gh.gitData.createCommit(owner, repo, message, treeSha, List(baseCommitSha)))

    def commitFilesIfChanged(
        baseTreeSha: String,
        parentCommitSha: String,
        filteredFilesContent: List[(String, String)]): Github4sResponse[Option[Ref]] =
      filteredFilesContent match {
        case Nil =>
          none[Ref].pure[Github4sResponse]
        case list =>
          for {
            ghResultTree   <- createTree(baseTreeSha, list)
            ghResultCommit <- createCommit(ghResultTree.result.sha, parentCommitSha)
            ghResultUpdate <- updateHead(branch, ghResultCommit.result.sha)
          } yield ghResultUpdate.map(Option(_))
      }

    val op = for {
      gHResultParentCommit <- fetchHeadCommit(branch)
      parentCommitSha = gHResultParentCommit.result.`object`.sha
      gHResultBaseTree <- fetchBaseTreeSha(parentCommitSha)
      baseTreeSha = gHResultBaseTree.result.tree.sha
      ghResultFilesContent <- fetchFilesContents(parentCommitSha)
      ghResultUpdate <- commitFilesIfChanged(
        baseTreeSha,
        parentCommitSha,
        filterNonChangedFiles(ghResultFilesContent.result))
    } yield ghResultUpdate

    op.execE
  }

  def commitDir(branch: String, message: String, dir: File): Either[OrgPolicyException, Ref] =
    fileReader.fetchDirsRecursively(List(dir)) match {
      case Right(Nil) => Left(IOException(s"Nothing to commit in dir ${dir.getAbsolutePath}"))
      case Right(head :: list) =>
        commitDir(branch, message, dir, NonEmptyList(head, list))
      case Left(e) => Left(e)
    }

  def commitDir(
      branch: String,
      message: String,
      baseDir: File,
      dirList: NonEmptyList[File],
      blobConfig: BlobConfig = defaultBlobConfig): Either[OrgPolicyException, Ref] = {

    def updateCommitDirH(dir: File, sha: Option[String]): Github4sResponse[RefCommit] =
      updateCommitDir(branch, message, baseDir, dir, blobConfig, sha)

    def processAllFiles: Github4sResponse[RefCommit] =
      dirList.reduceLeftM(dirHead => updateCommitDirH(dirHead, None)) { (commit, dir) =>
        updateCommitDirH(dir, Some(commit.sha))
      }

    val op = for {
      lastCommit <- processAllFiles
      headRef    <- updateHead(branch, lastCommit.result.sha)
    } yield headRef

    op.execE
  }

  private[this] def updateCommitDir(
      branch: String,
      message: String,
      baseDir: File,
      dirToCommit: File,
      blobConfig: BlobConfig = defaultBlobConfig,
      commitSha: Option[String] = None): Github4sResponse[RefCommit] = {

    def fetchBaseTreeSha: Github4sResponse[Option[RefCommit]] =
      commitSha map { sha =>
        EitherT(gh.gitData.getCommit(owner, repo, sha)).map(result => result.map(Option(_)))
      } getOrElse none[RefCommit].pure[Github4sResponse]

    def getAllFiles: List[File] = Option(dirToCommit.listFiles()).toList.flatten.filter(_.isFile)

    def createTreeDataList(files: List[File]): Github4sResponse[List[TreeData]] = {

      def readFileAsGithub4sResponse(file: File): Github4sResponse[Array[Byte]] =
        EitherT.fromEither[GHIO] {
          fileReader
            .getFileBytes(file)
            .bimap(e => UnexpectedException(e.getMessage), newGHResult)
        }

      def path(file: File): Github4sResponse[String] =
        EitherT.fromOption[GHIO](
          IO.relativize(baseDir, file).map(newGHResult),
          UnexpectedException(s"Can't determine path for ${file.getAbsolutePath}")
        )

      def createTreeDataSha(filePath: String, array: Array[Byte]): Github4sResponse[TreeData] =
        EitherT(ghWithRateLimit.gitData.createBlob(owner, repo, array.toBase64, Some("base64"))) map { refInfo =>
          refInfo.map(v => TreeDataSha(filePath, blobMode, blobType, v.sha))
        }

      def createTreeDataBlob(filePath: String, array: Array[Byte]): Github4sResponse[TreeData] =
        (TreeDataBlob(filePath, blobMode, blobType, new String(array)): TreeData).pure[Github4sResponse]

      def createTreeData(file: File, filePath: String, array: Array[Byte]): Github4sResponse[TreeData] =
        if (blobConfig.acceptedExtensions.exists(s => file.getName.toLowerCase.endsWith(s)) &&
          array.length < blobConfig.maximumSize) {
          createTreeDataBlob(filePath, array)
        } else {
          createTreeDataSha(filePath, array)
        }

      def processFile(file: File): Github4sResponse[TreeData] =
        for {
          filePath <- path(file)
          array    <- readFileAsGithub4sResponse(file)
          treeData <- createTreeData(file, filePath.result, array.result)
        } yield treeData

      files.traverse(processFile)
    }

    def createTree(baseTreeSha: Option[String], treeData: List[TreeData]): Github4sResponse[TreeResult] =
      EitherT(ghWithRateLimit.gitData.createTree(owner, repo, baseTreeSha, treeData))

    def createCommit(treeSha: String, parentCommit: String): Github4sResponse[RefCommit] =
      EitherT(ghWithRateLimit.gitData.createCommit(owner, repo, message, treeSha, List(parentCommit)))

    def parentCommitSha: Github4sResponse[String] =
      commitSha.map(sha => sha.pure[Github4sResponse]).getOrElse {
        fetchHeadCommit(branch) map (_.map(_.`object`.sha))
      }

    for {
      parentCommit <- parentCommitSha
      baseTree     <- fetchBaseTreeSha
      treeDataList <- createTreeDataList(getAllFiles)
      treeResult   <- createTree(baseTree.result.map(_.tree.sha), treeDataList.result)
      refCommit    <- createCommit(treeResult.result.sha, parentCommit.result)
    } yield refCommit
  }

  def fetchReference(ref: String): Either[GitHubException, NonEmptyList[Ref]] =
    gh.gitData.getReference(owner, repo, ref).execE

  def createTagRelease(
      branch: String,
      tag: String,
      message: String,
      releaseDescription: String): Either[GitHubException, Release] = {

    def createTag(obj: RefObject): Github4sResponse[Tag] =
      EitherT(gh.gitData.createTag(owner, repo, tag, message, obj.sha, obj.`type`))

    def createTagReference(commitSha: String): Github4sResponse[Ref] =
      EitherT(gh.gitData.createReference(owner, repo, s"refs/tags/$tag", commitSha))

    def createRelease: Github4sResponse[Release] =
      EitherT(gh.repos.createRelease(owner, repo, tag, tag, releaseDescription))

    val op = for {
      headCommit  <- fetchHeadCommit(branch)
      tagResponse <- createTag(headCommit.result.`object`)
      _           <- createTagReference(tagResponse.result.sha)
      release     <- createRelease
    } yield release

    op.execE
  }

  def latestPullRequests(
      branch: String,
      inPath: String,
      message: String): Either[GitHubException, List[PullRequest]] = {

    def fetchLastCommit: Github4sResponse[Option[Commit]] = {

      def findCommit(list: List[Commit]): Option[Commit] =
        list.sortBy(_.date)(Ordering[String].reverse).find(_.message.contains(message))

      EitherT(gh.repos.listCommits(owner = owner, repo = repo, path = Some(inPath))).map(ghResult =>
        ghResult.map(findCommit))
    }

    def fetchPullRequests(maybeDate: Option[String]): Github4sResponse[List[PullRequest]] = {

      def orderAndFilter(list: List[PullRequest]): List[PullRequest] = {
        val date = maybeDate.getOrElse("")
        list.mapFilter(pr => pr.merged_at.filter(_ > date).as(pr)).reverse
      }

      val filters = List(PRFilterClosed, PRFilterBase(branch), PRFilterSortUpdated, PRFilterOrderDesc)
      EitherT(gh.pullRequests.list(owner, repo, filters)).map(ghResult => ghResult.map(orderAndFilter))
    }

    val op = for {
      maybeCommit <- fetchLastCommit
      list        <- fetchPullRequests(maybeCommit.result.map(_.date))
    } yield list

    op.execE
  }

  def fetchHeadCommit(branch: String): Github4sResponse[Ref] = {

    def findReference(gHResult: GHResult[NonEmptyList[Ref]]): GHResponse[Ref] =
      gHResult.result.toList.find(_.ref == s"refs/heads/$branch") match {
        case Some(ref) => Right(gHResult.map(_ => ref))
        case None      => Left(UnexpectedException(s"Branch $branch not found"))
      }

    EitherT(gh.gitData.getReference(owner, repo, s"heads/$branch")).subflatMap(findReference)
  }

  def updateHead(branch: String, commitSha: String): Github4sResponse[Ref] =
    EitherT(gh.gitData.updateReference(owner, repo, s"heads/$branch", commitSha))

  def ghWithRateLimit: Github = {
    // Due to GitHub abuse rate limits, we should wait 1 sec between each request
    // https://developer.github.com/guides/best-practices-for-integrators/#dealing-with-abuse-rate-limits
    Thread.sleep(1000)
    gh
  }

}
