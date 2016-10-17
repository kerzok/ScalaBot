/*
package scalabot.common.extensions

import akka.actor.ActorSystem
import akka.persistence.{AbstractPersistentActor, PersistentActor, SnapshotOffer}

import scala.collection.mutable
import scalabot.common.bot.{AbstractBot, Conversation, Data, Idle}
import scalabot.common.chat.Chat
import scalabot.common.message.Intent

/**
  * Created by kerzo on 17.10.2016.
  */
trait PersistenceExtension[TData <: Data] extends BotExtension with PersistentActor {
  this: AbstractBot[TData] =>

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, offeredSnapshot: TData) =>
      data = offeredSnapshot
      states ++= data.chats.map(chat => chat -> Idle()).toMap
      recoverState(data)
    case intent: Intent =>
  }

  override def receiveCommand: Receive = receive orElse {
    case SaveSnapshot => saveSnapshot(data)
  }

  def recoverState(data: TData): Unit = {}

  override def persistenceId: String = id
}

case object SaveSnapshot
*/
