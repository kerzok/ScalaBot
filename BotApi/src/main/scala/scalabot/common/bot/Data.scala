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

import scalabot.common.chat.Chat

import scala.collection.mutable

/**
  * Created by Nikolay.Smelik on 7/25/2016.
  */
trait Data extends Serializable {
  var chats: mutable.Seq[Chat] = mutable.Seq()

  def updateChats(chat: Chat) = {
    chats = chats :+ chat
  }
}

case class EmptyData() extends Data
