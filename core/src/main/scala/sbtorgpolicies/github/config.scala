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

object config {

  val blobMode: String = "100644"
  val blobType: String = "blob"

  val defaultTextExtensions: Set[String] = Set(".md", ".css", ".html", ".properties", ".txt", ".scala", ".sbt")
  val defaultMaximumSize: Int            = 4048

  case class BlobConfig(acceptedExtensions: Set[String], maximumSize: Int)

  val defaultBlobConfig: BlobConfig = BlobConfig(defaultTextExtensions, defaultMaximumSize)

}
