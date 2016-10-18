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

package scalabot.skype

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.Config
import org.json4s.Extraction
import org.json4s.native.JsonMethods._

import scalabot.Implicits._
import scalabot.common.ApiClient
import scalabot.common.message.incoming.SourceMessage
import scalabot.common.message.{incoming, outcoming}
import scalabot.{common, skype}
import spray.http.CacheDirectives.`no-cache`
import spray.http.HttpHeaders.{Authorization, `Cache-Control`}
import spray.http._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Try
import scalabot.common.web.AddRoute

/**
  * Created by Nikolay.Smelik on 7/11/2016.
  */
class SkypeSource(config: Config) extends common.Source {
  override val id: String = Try(config.getString("id")).toOption getOrElse(throw new IllegalArgumentException("Skype id is not defined in config"))
  val clientSecret: String = Try(config.getString("secret")).toOption getOrElse(throw new IllegalArgumentException("Skype secret is not defined in config"))
  override val sourceType: String = getClass.getSimpleName
  private val client: SkypeApiClient = SkypeApiClient(id, clientSecret)(context.system)


  val pathToWebhook = path("skype" / Remaining) { botId => pathEnd {
    post {
      decodeRequest {
        entity(as[String]) { stringUpdate =>
          complete {
            parse(stringUpdate).extract[Seq[skype.Update]].foreach(update => self ! update)
            "Update received"
          }
        }
      }
    }
  }
  }
  botRef ! AddRoute(sourceType, pathToWebhook)

  override def sendReply(message: outcoming.OutgoingMessage, to: common.chat.Chat): Unit = message match {
    case message: outcoming.TextMessage =>
      client.post[Empty]("", s"conversations/${to.id}/activities/", pretty(render(Extraction.decompose(skype.Activities(skype.Message(message.value))))))
    case _ => throw new IllegalArgumentException("Not supported type of message")
  }

  override protected def handleUpdate[T <: SourceMessage](update: T): Unit = update match {
    case skypeUpdate: skype.Update =>
      val chat = common.chat.UserChat(skypeUpdate.from, sourceType, getUser(skypeUpdate))
      skypeUpdate.activity match {
        case ActivityType.message =>
          val message = incoming.TextMessage(chat, skypeUpdate.content.get)
          botRef ! message
        case _ =>
      }
    case _ => throw new IllegalArgumentException("Wrong message type for this source")
  }

  private def getUser(update: Update): common.chat.User = update.fromDisplayName.
    map(displayName => common.chat.User(displayName, update.name))  getOrElse common.chat.User("", None)

  private final case class SkypeApiClient(private val clientId: String, private val clientSecret: String)(override implicit val actorSystem: ActorSystem) extends ApiClient {
    type TIn = String
    val authorizationUrl = Uri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
    var expiredIn: Long = 0
    var accessToken: Future[String] = _

    override protected def request[TOut](method: HttpMethod, id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut] = getAccessToken flatMap
      (accessToken =>
        pipeline(HttpRequest(method, Uri(apiUrl(endpoint)), List(Authorization(OAuth2BearerToken(accessToken))), HttpEntity(ContentTypes.`application/json`, params)))
          .map(_.entity.asString)
          .map(parse(_).extract[TOut])) recover { case ex =>
      println(ex)
      throw ex
    }

    override protected def apiUrl(endpoint: String): String =
      (Try(scalabot.common.BotConfig.get("api.skype.url")).toOption getOrElse "https://apis.skype.com/v2/") + endpoint

    private def getAccessToken: Future[String] =
      if (System.currentTimeMillis() <= expiredIn) {
        accessToken
      } else {
        accessToken = pipeline(HttpRequest(HttpMethods.POST, authorizationUrl, List(`Cache-Control`(`no-cache`)),
          HttpEntity(MediaType.custom("application/x-www-form-urlencoded"), (s"client_id=$clientId&client_secret=$clientSecret&" +
            s"grant_type=client_credentials&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default").getBytes)))
          .map(_.entity.asString)
          .map(parse(_).extract[AccessTokenObject])
          .map(accessObject => {
            if (accessObject.access_token.isDefined) {
              expiredIn = System.currentTimeMillis() + (accessObject.expires_in.getOrElse(0) * 1000)
              accessObject.access_token.get
            } else {
              throw new SkypeException(accessObject.error.get, accessObject.error_description.get)
            }
          })
        accessToken
      }
  }
}
