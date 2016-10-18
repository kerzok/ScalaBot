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

package scalabot.telegram

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Rejected
import com.typesafe.config.Config
import org.json4s.native.JsonMethods._

import scalabot.Implicits._
import scalabot.common.ApiClient
import scalabot.common.message.incoming.SourceMessage
import scalabot.common.message.{incoming, outcoming}
import scalabot.{common, telegram}
import spray.http.{HttpMethod, HttpRequest, Uri}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Try
import scalabot.common.web.AddRoute

/**
  * Created by Nikolay.Smelik on 7/11/2016.
  */
class TelegramSource(config: Config) extends common.Source {
  override val id: String = Try(config.getString("id")).toOption getOrElse(throw new IllegalArgumentException("Telegram id is not defined in config"))
  override val sourceType: String = getClass.getSimpleName
  private val client: TelegramApiClient = TelegramApiClient(id)(context.system)

  val pathToWebhook = path("telegram" / Remaining) { botId => pathEnd {
    post {
      decodeRequest {
        entity(as[String]) { stringUpdate =>
            complete {
              val update = parse(stringUpdate).extract[telegram.Update]
              update.message.foreach(message => self ! update)
              "Update received"
            }
        }
      }
    }
  }
  }
  botRef ! AddRoute(sourceType, pathToWebhook)

  protected override def sendReply(message: outcoming.OutgoingMessage, to: common.chat.Chat): Unit = message match {
    case message: outcoming.TextMessage =>
      client.post[telegram.Message](id, "sendMessage", Map("chat_id" -> to.id.toString, "text" -> message.value, "parse_mode" -> "HTML"))
    case _ => throw new IllegalArgumentException("Not supported type of message")
  }

  protected override def handleUpdate[T <: SourceMessage](update: T): Unit = update match{
    case telegramUpdate: telegram.Update =>
      val message = telegramUpdate.message.getOrElse(throw new IllegalArgumentException("Message is empty"))
      val chat = getChat(message)
      if (message.text.isDefined) {
        var messageText = message.text.getOrElse("")
        if (messageText.startsWith("/")) {
          messageText = messageText.substring(1)
        }
        val commonMessage = incoming.TextMessage(chat, messageText)
        botRef ! commonMessage
      } else {
        //TODO add other types of messages
        throw new IllegalArgumentException("Unknown message")
      }
    case _ => throw new IllegalArgumentException("Wrong message type for this source")

  }

  private def getChat(message: telegram.Message): common.chat.Chat = message.chat.`type` match {
    case ChatType.`private` => common.chat.UserChat(message.chat.id.toString, sourceType, getUser(message.from))
    case _ =>
      val user = getUser(message.from)
      common.chat.GroupChat(message.chat.id.toString, sourceType, user, message.chat.title, Seq(user))
  }

  private def getUser(user: User): common.chat.User =
    common.chat.User(user.first_name + " " + user.last_name.getOrElse(""), user.username)

  private final case class TelegramApiClient(private val id: String)
                                            (override implicit val actorSystem: ActorSystem) extends ApiClient {
    type TIn = Map[String, String]

    override protected def request[T](method: HttpMethod,
                                      id: String, endpoint: String,
                                      params: Map[String, String])(implicit manifest: Manifest[T]): Future[T] =
      pipeline(HttpRequest(method, Uri(apiUrl(endpoint)).withQuery(params)))
        .map(_.entity.asString)
        .map(parse(_).extract[TelegramObject[T]])
        .map(_.result)

    override protected def apiUrl(endpoint: String): String =
      (Try(scalabot.common.BotConfig.get("api.telegram.url")).toOption getOrElse "https://api.telegram.org/bot") +
        id + "/" + endpoint
  }
}
