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

import akka.actor.ActorRef
import akka.util.Timeout

import scalabot.common.message._
import scalabot.common.chat.{Chat, System}
import scala.collection.mutable
import scala.util.Try
import scala.concurrent.duration._
import scalabot.common.message.outcoming.{OutgoingMessage, TextMessage}

/**
  * Created by Nikolay.Smelik on 7/22/2016.
  */
abstract class Conversation()(implicit private val botRef: ActorRef) extends Serializable {
  val bundle: Bundle = Bundle()
  private[bot] var currentState: BotState = initialState

  def initialState: BotState
  final def apply(intent: Intent): Conversation = {
    val reply = currentState(intent)
    reply.intents.foreach(message => botRef ! message)
    reply.state match {
      case Exit => Idle()
      case state : StateWithDefaultReply =>
        currentState = state
        apply(intent)
      case MoveToConversation(conversation, newIntent: Intent) => conversation(newIntent)
      case MoveToConversation(conversation, _) => conversation(intent)
      case newState: BotState =>
        currentState = newState
        this
    }
  }

  final def changeState(intent: ChangeStateIntent): (Intent, Conversation) = {
    intent match {
      case AskChangeStateIntent(sender, recipient, newState, _) =>
        if (currentState.canChange) {
          (SystemPositiveIntent(recipient), newState)
        } else {
          (SystemNegativeIntent(recipient), this)
        }
      case RequireChangeStateIntent(sender, recipient, newState, _) =>
        (SystemPositiveIntent(recipient), newState)
    }
  }

  final def appendBundle(otherBundle: Bundle): Conversation = {
    bundle ++= otherBundle
    this
  }
}

trait AsyncConversation extends Conversation {
  def asyncResponse: PartialFunction[AsyncIntent, Reply]

  implicit val timeout: Timeout = Timeout(1 second)
  def errorMessage: String
  def asyncFailedMessage: String
  val asyncState: BotState = {
    BotState {
      case AsyncFailed(sender) =>
        Reply(Exit).withIntent(ReplyMessageIntent(sender, asyncFailedMessage))
      case async: AsyncIntent if asyncResponse.isDefinedAt(async) => asyncResponse(async)
      case textIntent: TextIntent =>
        Reply(asyncState).withIntent(ReplyMessageIntent(textIntent.sender, errorMessage))
    }
  }

  trait AsyncIntent extends Intent
  case class DataAsyncIntent[T](sender: Chat, data: T)
  case class AsyncFailed(sender: Chat) extends AsyncIntent
}

case class Idle()(implicit val actorRef: ActorRef) extends Conversation {
  override def initialState: BotState = Exit
}

case class ConversationWithDefaultReply(message: OutgoingMessage)(implicit val actorRef: ActorRef) extends Conversation {
  override def initialState: BotState = StateWithDefaultReply(message)
}

case object ConversationWithDefaultReply {
  def apply(message: String)(implicit actorRef: ActorRef): Conversation = ConversationWithDefaultReply(TextMessage(message))
}

case class Bundle() {
  private[Bundle] val bundleMap: mutable.Map[String, Any] = mutable.Map.empty

  def put(name: String, value: Any) = {
    bundleMap += (name -> value)
  }

  def getObjectOpt[T](name: String): Option[T] = {
    Try(bundleMap(name).asInstanceOf[T]).toOption
  }

  def getObject[T](name: String): T = {
    bundleMap(name).asInstanceOf[T]
  }

  def getObjectOrElse[T](name: String, default: T): T = {
    bundleMap.getOrElse(name, default).asInstanceOf[T]
  }

  def getString(name: String): String = {
    bundleMap(name).asInstanceOf[String]
  }

  def getInt(name: String): Int = {
    bundleMap(name).asInstanceOf[Int]
  }
  def getBoolean(name: String, default: Boolean = false): Boolean = {
    bundleMap.getOrElse(name, default).asInstanceOf[Boolean]
  }

  def ++=(bundle: Bundle): Unit = {
    bundleMap ++= bundle.bundleMap
  }

  def containsKey(name: String): Boolean = {
    bundleMap.contains(name)
  }
}
