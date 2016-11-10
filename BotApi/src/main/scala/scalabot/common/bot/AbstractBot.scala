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

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}
import org.reflections.Reflections

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Try
import scalabot.common.chat.Chat
import scalabot.common.message._
import scalabot.common.message.incoming.IncomingMessage
import scalabot.common.web.{AddRoute, StopWebhook, Webhook}
import scalabot.common.{BotConfig, Source}

/**
  * Created by Nikolay.Smelik on 7/22/2016.
  */
trait AbstractBot[TData <: Data] extends Actor with ActorLogging {
  protected var data: TData
  protected def id: String
  val selfSelection: ActorSelection = context.actorSelection(s"akka://BotSystem/user/${id}Supervisor/$id")
  implicit val system: ActorSystem = context.system
  private[this] val states: mutable.Map[Chat, Conversation] = mutable.Map.empty
  installSources()
  log.info(s"Bot $id successfully started!")


  def helpMessage: String

  def unknownMessage: String

  def startConversation: PartialFunction[Intent, Conversation]

  def getChat(source: String, id: String): Option[Chat] = {
    data.chats.find(chat => chat.source.toLowerCase.contains(source.toLowerCase) && (chat.id == id))
  }

  def recoverState(data: TData): Unit = {}

  override def receive: Receive = handleSystemMessage orElse handleOutgoingMessage orElse handleIncomingMessage orElse handleCustomMessage

  def handleCustomIntent: PartialFunction[Any, Intent] = PartialFunction.empty

  def handleCustomMessage: Receive = PartialFunction.empty

  override def postStop(): Unit = {
    BotHelper.webhook ! StopWebhook
  }

  protected def positiveIntentMatcher(text: String): Boolean = text.matches("""(y|Y)es|(y|Y)up|(y|Y)eah|(y|Y)ep""")
  protected def negativeIntentMatcher(text: String): Boolean = text.matches("""(N|n)o|(N|n)ope""")

  private[this] def handleSystemMessage: Receive = {
    case intent: ChangeStateIntent =>
      val recipientState = states(intent.recipient)
      val senderState = states(intent.sender)
      val (replyIntent, newState) = recipientState.changeState(intent)
      updateState(intent.sender, senderState(replyIntent))
      updateState(intent.recipient, newState(intent.innerIntent))
    case AddRoute(sourceId, route) =>
      BotHelper.webhook ! AddRoute(id + sourceId, route)
  }

  private[this] def handleOutgoingMessage: Receive = {
    case ReplyMessageIntent(sender: Chat, message) =>
      sender sendMessage message
  }

  private[this] def selectBaseConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, text) if text.matches("(?:H|h)elp") =>
      new HelpConversation(helpMessage).apply(intent)
  }

  private[this] def selectUnknownConversation: PartialFunction[Intent, Conversation] = {
    case intent: Intent => new UnknownConversation(unknownMessage).apply(intent)
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
    case incoming.TextMessage(sender, text) if positiveIntentMatcher(text) =>
      PositiveIntent(sender, text)
    case incoming.TextMessage(sender, text) if negativeIntentMatcher(text) =>
      NegativeIntent(sender, text)
    case incoming.TextMessage(sender, text) if text.matches("""\d+""") =>
      NumberIntent(sender, text.toInt)
  }

  private[this] def handleTextIntent: PartialFunction[Any, Intent] = {
    case incoming.TextMessage(sender, text) =>
      new TextIntent(sender, text)
    case _ => throw new IllegalArgumentException("Unknown type of incoming message")
  }

  private[this] def updateState(chat: Chat, newState: Conversation): Unit = chat match {
    case systemChat: scalabot.common.chat.System if newState.isInstanceOf[Idle] =>
      states -= chat
    case systemChat: scalabot.common.chat.System =>
      states += (chat -> newState)
    case chat: Chat =>
      states += (chat -> newState)
  }

  private[this] def installSources() = {
    val reflection = new Reflections()
    val classes = reflection.getSubTypesOf(classOf[Source])
    classes.foreach(sourceClass => {
      val valuesOpt = Try(BotConfig.getConfig(id + "." + sourceClass.getSimpleName)).toOption
      valuesOpt match {
        case Some(values) => context.actorOf(Props(sourceClass, values), sourceClass.getSimpleName)
        case None => //ignore
      }
    })
  }

}

case object BotHelper {
  val system = ActorSystem("BotSystem")
  private[this] val webhookHost: String = Try(BotConfig.get("bot.webhook.host")).toOption.getOrElse("localhost")
  private[this] val webhookPort: Int = Try(BotConfig.get("bot.webhook.port").toInt).toOption.getOrElse(8080)
  private[bot] val webhook = system.actorOf(Props(classOf[Webhook], webhookHost, webhookPort))

  def registerBot[T](botClass: Class[T]): ActorRef = registerBot(Props(botClass), botClass)

  def registerBot[T](botClass: Class[T], args: Any*): ActorRef = registerBot(Props(botClass, args), botClass)

  private def registerBot[T](botProps: Props, botClass: Class[T]) = {
    val supervisorProps = withSupervisor(botProps, botClass.getSimpleName)
    system.actorOf(supervisorProps, botClass.getSimpleName + "Supervisor")
  }

  private def withSupervisor(props: Props, name: String) = {
    BackoffSupervisor.props(
      Backoff.onFailure(
        props,
        childName = name,
        minBackoff = 2 seconds,
        maxBackoff = 20 seconds,
        randomFactor = 0.2
      ).withAutoReset(3 seconds)
        .withSupervisorStrategy(OneForOneStrategy() {
          case _ => SupervisorStrategy.Restart
      }))
  }
}

