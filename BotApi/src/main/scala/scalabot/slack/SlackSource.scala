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

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.contrib.throttle.TimerBasedThrottler
import akka.contrib.throttle.Throttler.{RateInt, SetRate, SetTarget}
import com.typesafe.config.Config
import org.json4s.native.JsonMethods._

import scalabot.common.ApiClient
import scalabot.common.message.incoming.SourceMessage
import scalabot.common.message.{incoming, outcoming}
import scalabot.{common, slack}
import spray.http.{HttpMethod, HttpRequest, Uri}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by Nikolay.Smelik on 7/12/2016.
  */
class SlackSource(config: Config) extends common.Source with ActorLogging {
  override val sourceType: String = getClass.getSimpleName
  override val id: String = Try(config.getString("id")).toOption getOrElse (throw new IllegalArgumentException("Slack id is not defined in config"))
  private[this] val defaultThrottlingRate = 20.msgsPerSecond
  private[this] val slowThrottlingRate = 1.msgsPerSecond
  private[this] val counter: AtomicInteger = new AtomicInteger(0)
  private[this] val client: SlackApiClient = SlackApiClient(id)(context.system)
  private[this] val throttler: ActorRef = context.actorOf(Props(classOf[TimerBasedThrottler], defaultThrottlingRate))
  implicit val formats = scalabot.slack.SlackUpdate.formats

  private[this] var webSocket: Option[ActorRef] = None
  private[this] var integrationInfo: StartResponse = _
  private[this] var lastPing: Ping = Ping(counter.get())
  private[this] var lastPong: Pong = Pong(counter.get())
  private[this] var pingPongScheduler: Option[Cancellable] = None

  private[this] var messageStorage: Map[Int, SlackUpdate] = Map.empty
  openConnection()


  override def sendReply(message: outcoming.OutgoingMessage, to: common.chat.Chat): Unit = {
    val id = counter.incrementAndGet()
    message match {
      case outcoming.TextMessage(text) =>
        val message = TextMessageResponse(id, to.id, text)
        messageStorage += (id -> message)
        throttler ! message
    }
  }

  private def retrySendMessage(id: Int) = {
    messageStorage.get(id).foreach(throttler ! _)
  }

  private def sendPing(): Unit = {
    val id = counter.incrementAndGet()
    lastPing = Ping(id)
    webSocket.foreach(_ ! lastPing)
  }

  private def runPingPong(): Unit = {
    lastPing = Ping(counter.get())
    lastPong = Pong(counter.get())
    pingPongScheduler = Some(context.system.scheduler.schedule(5 seconds, 10 seconds, new Runnable {
      override def run(): Unit = {
        if (lastPing.id == lastPong.replyTo) {
          sendPing()
        } else {
          log.warning("Slack server not responding within 5 seconds. Trying to reconnect...")
          restartWebSocket()
        }
      }
    }))
  }

  def initialHandler[T <: SourceMessage](update: T): Unit = update match {
    case ConnectionEstablished(ref) =>
      log.info("WebSocket connection established.")
      webSocket = Some(ref)
      throttler ! SetTarget(webSocket)
    case Hello =>
      log.info("The client has successfully connected to the Slack server.")
      messageStorage.values.foreach(throttler ! _)
      runPingPong()
      context.unbecome()
    case _ =>
  }

  override protected def handleUpdate[T <: SourceMessage](update: T): Unit = update match {
    case pong: Pong => lastPong = pong
    case TeamJoin(user: User) => integrationInfo = integrationInfo.copy(users = integrationInfo.users :+ user)
    case update@TextMessage(channelId, userId, text, _, None) =>
      val channelOpt = findChannelById(channelId)
      findUserById(userId) match {
        case Some(user) if !user.isBot.getOrElse(false) =>
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
        case Some(user) =>
      }
    case ResponseMessage(true, replyTo, _, _) =>
      messageStorage -= replyTo
    case ResponseMessage(false, replyTo, _, Some(Error(-1, _))) =>
      throttler ! SetRate(slowThrottlingRate)
      retrySendMessage(replyTo)
      context.system.scheduler.scheduleOnce(1 minute, new Runnable {
        override def run(): Unit = {
          throttler ! SetRate(defaultThrottlingRate)
        }
      })
    case ResponseMessage(false, _, _, Some(error)) =>
      log.info(s"Sending message to Slack finish with error. Error code: ${error.code}. Error message: ${error.msg}")
    case ErrorMessage(error) =>
      log.warning(s"Slack server respond with error: Error code: ${error.code}. Error message: ${error.msg}")
      webSocket.foreach(_ ! Disconnect)
      restartWebSocket()
    case UnexpectedMessage(subtype, _) => log.info(s"unexpected type of message: $subtype")
    case UnexpectedEvent(messageType, json) => log.debug(s"unexpected event: $messageType, with json: $json")
    case Goodbye =>
      log.info("Slack websocket is going to close. ")
      webSocket.foreach(_ ! Disconnect)
      restartWebSocket()
    case Disconnect =>
      log.info("Slack WebSocket closed. restarting")
      restartWebSocket()
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
      case Some(profile) => profile.firstName
      case None => slackUser.name
    }
    val lastName = slackUser.profile.map(profile => profile.lastName)
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

  private def restartWebSocket(): Unit = {
    pingPongScheduler.map(_.cancel())
    context.system.scheduler.scheduleOnce(1 seconds, new Runnable {
      override def run(): Unit = {
        openConnection()
      }
    })
  }

  private def openConnection(): Unit = client.post[StartResponse](id, "rtm.start", Map("token" -> id)).onComplete {
    case Success(response) =>
      integrationInfo = response
      become(initialHandler)
      WebSocket(response.url, self)(context.system)
    case Failure(ex) =>
      log.warning("Unable to get integration info from Slack. Finish SlackSource")
      context.stop(self)
  }

  private final case class SlackApiClient(private val id: String)(override implicit val actorSystem: ActorSystem) extends ApiClient {
    type TIn = Map[String, String]

    override protected def apiUrl(endpoint: String): String =
      (Try(scalabot.common.BotConfig.get("api.slack.url")).toOption getOrElse "https://slack.com/api/") + endpoint

    override protected def request[TOut](method: HttpMethod, id: String, endpoint: String, params: TIn)(implicit manifest: Manifest[TOut]): Future[TOut] =
      pipeline(HttpRequest(method, Uri(apiUrl(endpoint)).withQuery(params)))
        .map(_.entity.asString)
        .map(value => parse(value).camelizeKeys.extract[TOut])
  }

}
