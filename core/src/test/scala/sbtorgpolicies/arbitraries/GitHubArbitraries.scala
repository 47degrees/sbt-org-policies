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

package sbtorgpolicies.arbitraries

import java.io.File

import cats.data.NonEmptyList
import cats.syntax.either._
import com.fortysevendeg.scalacheck.datetime.instances.joda._
import com.fortysevendeg.scalacheck.datetime.joda.GenJoda._
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import github4s.GithubResponses._
import github4s.free.domain._
import org.joda.time._
import org.joda.time.format._
import org.scalacheck.{Arbitrary, Gen}

import scala.language.postfixOps

trait GitHubArbitraries {

  val dateTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  val owner: String              = "47deg"
  val repo: String               = "sbt-org-policies"
  val branch: String             = "master"
  val ref: String                = s"heads/$branch"
  val tag: String                = "v0.0.1"
  val sampleMessage: String      = "Hello World"
  val releaseDescription: String = "Release description"
  val nonExistingMessage: String = "this is a commit message"
  val baseDir: File              = new File("/tmp/")
  val date2015: DateTime         = dateTimeFormat.parseDateTime("2015-01-01T00:00:00.000Z")
  val filesAndContents: List[(String, String)] = 1 to 5 map { index =>
    (s"file$index", s"Content file $index")
  } toList

  def genGHResponse[T](gen: Gen[T]): Gen[GHResponse[T]] =
    Gen.oneOf(
      genGHException.map(_.asLeft[GHResult[T]]),
      gen.map(v => GHResult(v, 200, Map.empty).asRight[GHException]))

  val genGHException: Gen[GHException] = {
    val message = "Generated Exception"
    Gen.oneOf(JsonParsingException(message, """{"val": "value"}"""), UnexpectedException(message))
  }

  val genEmail: Gen[String] =
    for {
      login       <- Gen.identifier
      domainName  <- Gen.identifier
      domainChars <- Gen.chooseNum[Int](1, 3)
      domain      <- Gen.listOfN(domainChars, Gen.choose[Char]('a', 'z'))
    } yield s"$login@$domainName.$domain"

  val genURL: Gen[String] =
    Gen.identifier map (id => s"http://github.com/$id")

  val genDateTimeString: Gen[String] =
    genDateTime.map(dateTimeFormat.print)

  val genSimpleUser: Gen[User] = {
    for {
      id        <- Gen.posNum[Int]
      login     <- Gen.uuid.map(_.toString)
      avatarUrl <- genURL
      htmlUrl   <- genURL
    } yield User(id, login, avatarUrl, htmlUrl, None, None, None, None, None, None)
  }

  val genFullUser: Gen[User] = {
    for {
      user          <- genSimpleUser
      maybeName     <- Gen.option(Gen.alphaStr)
      maybeEmail    <- Gen.option(genEmail)
      maybeCompany  <- Gen.option(Gen.alphaStr)
      maybeBlog     <- Gen.option(genURL)
      maybeLocation <- Gen.option(Gen.alphaStr)
      maybeBio      <- Gen.option(Gen.alphaStr)
    } yield
      user.copy(
        name = maybeName,
        email = maybeEmail,
        company = maybeCompany,
        blog = maybeBlog,
        location = maybeLocation,
        bio = maybeBio
      )
  }

  val genSimpleAndFullUserLists: Gen[(GHResponse[List[User]], List[GHResponse[User]])] =
    for {
      n     <- Gen.posNum[Int]
      list1 <- genGHResponse(Gen.listOfN(n, genSimpleUser))
      list2 <- Gen.listOfN(n, genGHResponse(genFullUser))
    } yield (list1, list2)

  val genRefObject: Gen[RefObject] =
    for {
      refObjectType <- Gen.oneOf("commit", "tag")
      sha           <- Gen.identifier
      url           <- genURL
    } yield RefObject(refObjectType, sha, url)

  def genRef(refPrefix: String): Gen[Ref] =
    for {
      ref       <- Gen.alphaStr
      url       <- genURL
      refObject <- genRefObject
    } yield Ref(s"$refPrefix$ref", url, refObject)

  def genNonEmptyListRef(refPrefix: String): Gen[NonEmptyList[Ref]] =
    for {
      head <- genRef(refPrefix)
      tail <- Gen.listOf(genRef(refPrefix))
    } yield NonEmptyList(head, tail)

  val genRefAuthor: Gen[RefAuthor] =
    for {
      date  <- genDateTimeString
      name  <- Gen.alphaStr
      email <- genEmail
    } yield RefAuthor(date, name, email)

  val genTag: Gen[Tag] =
    for {
      sha       <- Gen.identifier
      url       <- genURL
      refAuthor <- genRefAuthor
      refObject <- genRefObject
    } yield Tag(tag, sha, url, sampleMessage, refAuthor, refObject)

  val genRelease: Gen[Release] =
    for {
      id          <- Gen.posNum[Int]
      target      <- Gen.identifier
      name        <- Gen.alphaStr
      body        <- Gen.alphaStr
      draft       <- Gen.oneOf(true, false)
      prerelease  <- Gen.oneOf(true, false)
      createdAt   <- genDateTimeString
      publishedAt <- genDateTimeString
      author      <- genSimpleUser
      url         <- genURL
      htmlUrl     <- genURL
      assetsUrl   <- genURL
      uploadUrl   <- genURL
      tarballUrl  <- genURL
      zipballUrl  <- genURL
    } yield
      Release(
        id = id,
        tag_name = tag,
        target_commitish = target,
        name = name,
        body = body,
        draft = draft,
        prerelease = prerelease,
        created_at = createdAt,
        published_at = publishedAt,
        author = author,
        url = url,
        html_url = htmlUrl,
        assets_url = assetsUrl,
        upload_url = uploadUrl,
        tarball_url = tarballUrl,
        zipball_url = zipballUrl
      )

  def genPullRequest(genMergedAt: Gen[Option[String]] = Gen.option(genDateTimeString)): Gen[PullRequest] =
    for {
      id        <- Gen.posNum[Int]
      number    <- Gen.posNum[Int]
      state     <- Gen.oneOf("open", "closed")
      title     <- Gen.alphaStr
      body      <- Gen.alphaStr
      locked    <- Gen.oneOf(true, false)
      url       <- genURL
      createdAt <- genDateTimeString
      updatedAt <- Gen.option(genDateTimeString)
      closedAt  <- Gen.option(genDateTimeString)
      mergedAt  <- genMergedAt
    } yield
      PullRequest(
        id = id,
        number = number,
        state = state,
        title = title,
        body = body,
        locked = locked,
        html_url = url,
        created_at = createdAt,
        updated_at = updatedAt,
        closed_at = closedAt,
        merged_at = mergedAt,
        base = None,
        user = None,
        assignee = None,
        head = None
      )

  val genCommit: Gen[Commit] =
    for {
      sha       <- Gen.identifier
      message   <- Gen.alphaStr.filter(_ != nonExistingMessage)
      date      <- genDateTimeString
      url       <- genURL
      login     <- Gen.option(Gen.identifier)
      avatarUrl <- Gen.option(genURL)
      authorUrl <- Gen.option(genURL)
    } yield Commit(sha, message, date, url, login, avatarUrl, authorUrl)

  val genRefCommit: Gen[RefCommit] =
    for {
      sha       <- Gen.identifier
      url       <- genURL
      refAuthor <- genRefAuthor
      message   <- Gen.alphaStr.filter(_ != nonExistingMessage)
      tree      <- genRefObject
      parents   <- Gen.listOf(genRefObject)
    } yield RefCommit(sha, url, refAuthor, refAuthor, message, tree, parents)

  val genTreeDataResult: Gen[TreeDataResult] =
    for {
      path     <- Gen.alphaStr
      treeType <- Gen.oneOf("tree", "blob")
      size     <- Gen.option(Gen.posNum[Int])
      sha      <- Gen.identifier
      url      <- genURL
    } yield TreeDataResult(path, "100644", treeType, size, sha, url)

  val genTreeResult: Gen[TreeResult] =
    for {
      sha  <- Gen.identifier
      url  <- genURL
      tree <- Gen.listOf(genTreeDataResult)
    } yield TreeResult(sha, url, tree)

  def genPullRequestsMergedAt(from: DateTime, range: Period): Gen[PullRequest] =
    genPullRequest(genDateTimeWithinRange(from, range).map(d => Some(dateTimeFormat.print(d))))

  val genGHResPRListMergedFrom2015: Gen[GHResponse[List[PullRequest]]] =
    genGHResponse(Gen.listOf(genPullRequestsMergedAt(date2015, Period.months(11))))

  val genGHResPRListMergedAt2016: Gen[GHResponse[List[PullRequest]]] =
    genGHResponse(Gen.listOf(genPullRequestsMergedAt(date2015.withYear(2016), Period.months(11))))

  // Implicits
  implicit val ghResponseRefArbitrary: Arbitrary[GHResponse[Ref]] = Arbitrary {
    genGHResponse(genRef(ref))
  }

  implicit val ghResponseNonEmptyListRefArbitrary: Arbitrary[GHResponse[NonEmptyList[Ref]]] = Arbitrary {
    genGHResponse(genNonEmptyListRef(ref))
  }

  implicit val ghResponseTagArbitrary: Arbitrary[GHResponse[Tag]] = Arbitrary {
    genGHResponse(genTag)
  }

  implicit val ghResponseReleaseArbitrary: Arbitrary[GHResponse[Release]] = Arbitrary {
    genGHResponse(genRelease)
  }

  implicit val ghResponsePullRequestListArbitrary: Arbitrary[GHResponse[List[PullRequest]]] = Arbitrary {
    genGHResponse(Gen.listOf(genPullRequest()))
  }

  implicit val ghResponseRefCommitArbitrary: Arbitrary[GHResponse[RefCommit]] = Arbitrary {
    genGHResponse(genRefCommit)
  }

  implicit val ghResponseCommitListArbitrary: Arbitrary[GHResponse[List[Commit]]] = Arbitrary {
    genGHResponse(Gen.listOf(genCommit))
  }

  implicit val ghResponseTreeResultArbitrary: Arbitrary[GHResponse[TreeResult]] = Arbitrary {
    genGHResponse(genTreeResult)
  }

  implicit val ghResponseRefObjectArbitrary: Arbitrary[GHResponse[RefInfo]] = Arbitrary {
    genGHResponse(genRefObject)
  }

}

object GitHubArbitraries extends GitHubArbitraries
