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

package scalabot.slack

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props}
import akka.contrib.throttle.TimerBasedThrottler
import akka.contrib.throttle.Throttler.{RateInt, SetTarget}
import com.typesafe.config.Config
import org.json4s.Extraction
import org.json4s.native.JsonMethods._

import scalabot.Implicits._
import scalabot.common.ApiClient
import scalabot.common.message.incoming.SourceMessage
import scalabot.common.message.{incoming, outcoming}
import scalabot.common.web.WebSocket
import scalabot.common.web.WebSocketHelper.{Connect, Send}
import scalabot.{common, slack}
import spray.http.{HttpMethod, HttpRequest, Uri}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by Nikolay.Smelik on 7/12/2016.
  */
class SlackSource(config: Config) extends common.Source with ActorLogging {
  override val sourceType: String = getClass.getSimpleName
  override val id: String = Try(config.getString("id")).toOption getOrElse (throw new IllegalArgumentException("Slack id is not defined in config"))
  private[this] val counter: AtomicInteger = new AtomicInteger(0)
  private[this] val client: SlackApiClient = SlackApiClient(id)(context.system)
  private[this] val webSocket = context.actorOf(Props(classOf[WebSocket], self), s"${sourceType}Websocket")
  private[this] val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], 1.msgsPerSecond))
  private[this] var integrationInfo: StartResponse = _
  openConnection()
  throttler ! SetTarget(Some(webSocket))

  override def sendReply(message: outcoming.OutgoingMessage, to: common.chat.Chat): Unit = {
    val id = counter.incrementAndGet()
    message match {
      case textMessage: outcoming.TextMessage =>
        val responseToSend = pretty(render(Extraction.decompose(Response(id, "message", to.id, textMessage.value))))
        throttler ! Send(responseToSend)
    }
  }

  override protected def handleUpdate[T <: SourceMessage](update: T): Unit = update match {
    case update@TextMessage(channelId, userId, text, _, None) =>
      val channelOpt = findChannelById(channelId)
      findUserById(userId) match {
        case Some(user) =>
          val chat = channelOpt match {
            case Some(channel) =>
              val members = getChannelMembers(channel)
              common.chat.GroupChat(channel.id, sourceType, transformSlackUserToCommon(user), Some(channel.name), members)
            case None =>
              val im = findOrCacheImById(channelId, Im(channelId, userId, Option(update)))
              common.chat.UserChat(im.id, sourceType, transformSlackUserToCommon(user))
          }
          val commonMessage = incoming.TextMessage(chat, text)
          botRef ! commonMessage
        case None => log.info(s"Unable to find user with id: $userId")
      }
    case TeamJoin(user: User) => integrationInfo = integrationInfo.copy(users = integrationInfo.users :+ user)
    case UnexpectedMessage(subtype, _) => log.info(s"unexpected type of message: $subtype")
    case UnexpectedEvent(messageType, json) => log.debug(s"unexpected event: $messageType, with json: $json")
    case Hello => log.info("Slack websoket start.")
    case Goodbye => log.info("Slack websocket is going to close. ")
    case ResponseMessage(false, _, _, Some(error)) =>
      log.info(s"Sending message to Slack finish with error. Error code: ${error.code}. Error message: ${error.msg}")
    case _ =>
  }

  private def getChannelMembers(channel: Channel): Seq[common.chat.User] = channel.members.map(membersList => {
      membersList.foldLeft(Seq.empty[common.chat.User]) {
        case (result, userId) => findUserById(userId) match {
          case Some(user) => result :+ transformSlackUserToCommon(user)
          case None => result
        }
      }
    }).getOrElse(Seq.empty)

  private def transformSlackUserToCommon(slackUser: slack.User): common.chat.User = {
    val firstName = slackUser.profile match {
      case Some(profile) => profile.first_name
      case None => slackUser.name
    }
    val lastName = slackUser.profile.map(profile => profile.last_name)
    common.chat.User(firstName + " " + lastName.getOrElse(""), Some(slackUser.name))
  }

  private def findChannelById(channelId: String): Option[Channel] =
    integrationInfo.channels.find(storeChannel => storeChannel.id == channelId)

  private def findOrCacheImById(imId: String, newIm: => Im): Im = {
    integrationInfo.ims.find(storeIm => storeIm.id == imId).getOrElse({
      integrationInfo = integrationInfo.copy(ims = integrationInfo.ims :+ newIm)
      newIm
    })
  }

  private def findUserById(userId: String): Option[slack.User] = {
    integrationInfo.users.find(storeUser => storeUser.id == userId)
  }

  private def getHostAndPath(url: String): (String, String) = {
    val withoutProtocol = url drop 6
    val host = (withoutProtocol split '/') (0)
    (host, withoutProtocol.drop(host.length))
  }

  private def openConnection(): Unit = client.post[StartResponse](id, "rtm.start", Map("token" -> id)).foreach(response => {
      integrationInfo = response
      val (host, path) = getHostAndPath(response.url)
      webSocket ! Connect(host, 443, path, withSsl = true)
    }
  )

  private final case class SlackApiClient(private val id: String)(override implicit val actorSystem: ActorSystem) extends ApiClient {
    type TIn = Map[String, String]

    override protected def apiUrl(endpoint: String): String =
      (Try(scalabot.common.BotConfig.get("api.slack.url")).toOption getOrElse "https://slack.com/api/") + endpoint

    override protected def request[TOut](method: HttpMethod, id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut] =
      pipeline(HttpRequest(method, Uri(apiUrl(endpoint)).withQuery(params)))
        .map(_.entity.asString)
        .map(value => parse(value).extract[TOut])
  }

}
