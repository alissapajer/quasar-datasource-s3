/*
 * Copyright 2014–2019 SlamData Inc.
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

package quasar.physical.s3

import slamdata.Predef._

import scala.concurrent.ExecutionContext

import cats.effect.IO
import cats.data.OptionT
import org.http4s.Uri
import org.http4s.client.blaze.BlazeClientBuilder
import org.specs2.mutable.Specification
import pathy.Path
import scalaz.{-\/, \/-}

final class ChildrenSpec extends Specification {
  "lists all resources at the root of the bucket, one per request" >> {
    implicit val cs = IO.contextShift(ExecutionContext.global)
    // Force S3 to return a single element per page in ListObjects,
    // to ensure pagination works correctly
    val bucket = Uri.uri("https://slamdata-public-test.s3.amazonaws.com").withQueryParam("max-keys", "1")

    val dir = Path.rootDir
    val client = BlazeClientBuilder[IO](ExecutionContext.global).resource

    OptionT(client.use(impl.children(_, bucket, dir)))
      .getOrElseF(IO.raiseError(new Exception("Could not list children under the root")))
      .flatMap(_.compile.toList).map { children =>
        children.length must_== 4
        children.toSet must_==
          Set(\/-(Path.FileName("extraSmallZips.data")),
              -\/(Path.DirName("dir1")),
              -\/(Path.DirName("prefix3")),
              -\/(Path.DirName("testData")))
      }.unsafeRunSync
  }
}
