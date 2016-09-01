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

package scalabot.common.bot

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.Route
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.reflections.Reflections
import scalabot.common.chat.Chat
import scalabot.common.message._
import scalabot.common.message.incoming.IncomingMessage
import scalabot.common.web.{StartWebhook, Webhook}
import scalabot.common.{BotConfig, Source}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Try

/**
  * Created by Nikolay.Smelik on 7/22/2016.
  */
trait AbstractBot[TData <: Data] extends PersistentActor {
  protected var data: TData
  protected def id: String
  implicit val system: ActorSystem = context.system
  private[this] val states: mutable.Map[Chat, Conversation] = mutable.Map.empty
  installSources()

  def helpMessage: String

  def unknownMessage: String

  def startConversation: PartialFunction[Intent, Conversation]

  def getChat(source: String, id: String): Option[Chat] = {
    data.chats.find(chat => chat.source.toLowerCase.contains(source.toLowerCase) && (chat.id == id))
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, offeredSnapshot: TData) =>
      data = offeredSnapshot
      states ++= data.chats.map(chat => chat -> Idle()).toMap
      recoverState(data)
  }

  def recoverState(data: TData): Unit = {}

  override def persistenceId: String = id

  override def receiveCommand: Receive = handleSystemMessage orElse handleOutgoingMessage orElse handleIncomingMessage orElse handleCustomMessage

  def handleCustomIntent: PartialFunction[Any, Intent] = Map.empty

  def handleCustomMessage: Receive = Map.empty

  private[this] def handleSystemMessage: Receive = {
    case intent: ChangeStateIntent =>
      val recipientState = states(intent.recipient)
      val senderState = states(intent.sender)
      val (replyIntent, newState) = recipientState.changeState(intent)
      updateState(intent.sender, senderState(replyIntent))
      updateState(intent.recipient, newState(intent.innerIntent))
    case route: Route =>
      BotHelper.webhook ! route
      BotHelper.webhook ! StartWebhook
    case SaveSnapshot => saveSnapshot(data)
  }

  private[this] def handleOutgoingMessage: Receive = {
    case ReplyMessageIntent(sender: Chat, message) =>
      sender sendMessage message
  }

  private[this] def selectBaseConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, text) if text.matches("(?:H|h)elp") =>
      new HelpConversation(helpMessage)(self)(intent)
  }

  private[this] def selectUnknownConversation: PartialFunction[Intent, Conversation] = {
    case intent: Intent => new UnknownConversation(unknownMessage)(self)(intent)
  }

  private[this] def selectConversationByIntent = selectBaseConversation orElse startConversation orElse selectUnknownConversation

  private[this] def transformMessageToIntent: PartialFunction[IncomingMessage, Intent] = handleBasicIntent orElse handleCustomIntent orElse handleTextIntent

  private[this] def handleIncomingMessage: Receive = {
    case message: incoming.IncomingMessage =>
      val intent = transformMessageToIntent(message)
      if (!states.contains(message.sender)) {
        data.updateChats(message.sender)
        updateState(message.sender, Idle())
      }
      val state = states(message.sender)
      state match {
        case Idle() => updateState(message.sender, selectConversationByIntent(intent))
        case otherState => updateState(message.sender, otherState(intent))
      }
    case intent: Intent =>
      val state = states.getOrElse(intent.sender, Idle())
      state match {
        case Idle() => updateState(intent.sender, selectConversationByIntent(intent))
        case otherState: Conversation => updateState(intent.sender, otherState(intent))
      }
  }

  private[this] def handleBasicIntent: PartialFunction[Any, Intent] = {
    case incoming.TextMessage(sender, text) if text.matches("""yes|yup|yeah|yep""") =>
      PositiveIntent(sender)
    case incoming.TextMessage(sender, text) if text.matches("""no|nope""") =>
      NegativeIntent(sender)
    case incoming.TextMessage(sender, text) if text.matches("""\d+""") =>
      NumberIntent(sender, text.toInt)
  }

  private[this] def handleTextIntent: PartialFunction[Any, Intent] = {
    case incoming.TextMessage(sender, text) =>
      new TextIntent(sender, text)
    case _ => throw new IllegalArgumentException("Unknown type of incoming message")
  }

  private[this] def updateState(chat: Chat, newState: Conversation) = {
    states += (chat -> newState)
  }

  private[this] def installSources() = {
    val reflection = new Reflections()
    val classes = reflection.getSubTypesOf(classOf[Source])
    classes.foreach(sourceClass => {
      val valuesOpt = Try(BotConfig.getConfig(id + "." + sourceClass.getSimpleName)).toOption
      valuesOpt match {
        case Some(values) => context.actorOf(Props(sourceClass, values, self), sourceClass.getSimpleName)
        case None => //ignore
      }
    })
  }

}
case object SaveSnapshot

case object BotHelper {
  val system = ActorSystem("BotSystem")
  private[this] val webhookHost: String = Try(BotConfig.get("bot.webhook.host")).toOption.getOrElse("localhost")
  private[this] val webhookPort: Int = Try(BotConfig.get("bot.webhook.port").toInt).toOption.getOrElse(8080)
  private[bot] val webhook = system.actorOf(Props(classOf[Webhook], webhookHost, webhookPort))

  def registerBot[T](botClass: Class[T]): ActorRef = {
    system.actorOf(Props(botClass))
  }

  def registerBot[T](botClass: Class[T], args: Any*): ActorRef = {
    system.actorOf(Props(botClass, args))
  }
}

