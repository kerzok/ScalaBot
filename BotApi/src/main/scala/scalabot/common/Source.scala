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

import akka.actor.{Actor, ActorLogging, ActorRef}
import scalabot.common
import scalabot.common.chat.Chat
import scalabot.common.message.incoming.SourceMessage
import scalabot.common.message.outcoming.OutgoingMessage

import scala.util.{Failure, Try}

/**
  * Created by Nikolay.Smelik on 7/11/2016.
  */
trait Source extends Actor with ActorLogging {
  val sourceType: String
  val id: String
  protected val botRef: ActorRef = context.parent

  override def receive: Receive = {
    case update: SourceMessage => Try(handleUpdate(update)) match {
      case Failure(ex) => log.error(ex, s"Some error occur in $sourceType. error: $ex")
      case _ => //ignore
    }
    case (message: OutgoingMessage, to: common.chat.Chat) => sendReply(message, to)
    case _ => //ignore
  }

  protected def sendReply(message: OutgoingMessage, to: Chat): Unit
  protected def handleUpdate[T <: SourceMessage](update: T): Unit
}


