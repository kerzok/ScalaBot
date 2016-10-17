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

package scalabot.common.chat

import java.beans.Transient
import java.util.UUID

import akka.actor.ActorContext

/**
  * Created by Nikolay.Smelik on 7/11/2016.
  */
trait Chat extends Serializable {
  def id: String
  def source: String
  def from: User

  def sendMessage(message: Any)(implicit system: ActorContext): Unit = {
    system.actorSelection(source) ! (message, this)
  }
}

case class System(id: String = "system" + UUID.randomUUID().toString) extends Chat {
  override def sendMessage(message: Any)(implicit system: ActorContext): Unit = {}

  override def source: String = "system"

  override def from: User = User("user", None)
}
