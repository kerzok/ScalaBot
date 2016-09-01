/*
 * Copyright 2016 Nikolay Smelik
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

package scalabot.common

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Nikolay.Smelik on 7/6/2016.
  */

trait ApiClient {
  type TIn
  implicit val actorSystem: ActorSystem

  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def get[TOut](id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut] = request[TOut](HttpMethods.GET, id, endpoint, params)

  def post[TOut](id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut] = request[TOut](HttpMethods.POST, id, endpoint, params)

  protected def request[TOut](method: HttpMethod, id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut]

  protected def apiUrl(endpoint: String): String
}

