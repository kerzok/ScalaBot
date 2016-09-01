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

package scalabot.common.extensions

import org.json4s.native.JsonMethods._
import scalabot.Implicits._
import scalabot.common.chat.Chat
import scalabot.common.message.Intent
import spray.client.pipelining._
import spray.http._
import spray.caching.{Cache, LruCache}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
/**
  * Created by Nikolay.Smelik on 8/3/2016.
  */
trait SocketExtension extends BotExtension {
  var cache: Cache[Any] = LruCache(timeToLive = cacheExpiration)
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  def cacheExpiration: Duration = 1 day
  final def makeRequest[T](intent: SocketIntent)(implicit manifest: Manifest[T]): Unit = {
    val uri = Uri(intent.url).withQuery(intent.requestParams.params)
    if (intent.requestParams.canCache) {
      val result = cache(uri) {
        pipeline(HttpRequest(intent.requestParams.method, uri, intent.requestParams.headers))
          .map(_.entity.asString)
          .map(parse(_).extract[T])
      }
      result.foreach(result => self ! ResultIntent(intent.sender, result))
    } else {
      pipeline(HttpRequest(intent.requestParams.method, uri, intent.requestParams.headers))
        .map(_.entity.asString)
        .map(parse(_).extract[T])
        .foreach(result => self ! ResultIntent(intent.sender, result))
    }
  }

  final def makeRequest(intent: SocketIntent): Unit = {
    val uri = Uri(intent.url).withQuery(intent.requestParams.params)
    if (intent.requestParams.canCache) {
      val result = cache(uri) {
        pipeline(HttpRequest(intent.requestParams.method, uri, intent.requestParams.headers))
          .map(_.entity.asString)
      }
      result.foreach(result => self ! ResultIntent(intent.sender, result))
    } else {
      pipeline(HttpRequest(intent.requestParams.method, uri, intent.requestParams.headers))
        .map(_.entity.asString)
        .foreach(result => self ! ResultIntent(intent.sender, result))
    }
  }
}

case class SocketIntent(sender: Chat, url: String, requestParams: RequestParams = RequestParams()) extends Intent
case class ResultIntent(sender: Chat, result: Any) extends Intent

case class RequestParams(method: HttpMethod = HttpMethods.GET, canCache: Boolean = false) {
  var params: Map[String, String] = Map.empty
  var headers: List[HttpHeader] = List.empty

  def putParam(name: String, value: String): RequestParams = {
    params = params + (name -> value)
    this
  }

  def putHeader(httpHeader: HttpHeader): RequestParams = {
    headers = headers :+ httpHeader
    this
  }

  def ++=(requestParams: RequestParams): Unit = {
    params = params ++ requestParams.params
  }
}