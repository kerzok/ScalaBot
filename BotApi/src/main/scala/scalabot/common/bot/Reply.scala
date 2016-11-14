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
import scalabot.common.message.outcoming.OutgoingMessage
import scalabot.common.message.{Intent, ReplyMessageIntent}


/**
  * Created by Nikolay.Smelik on 8/3/2016.
  */
case class Reply(state: BotState) {
  var intents: Seq[Intent] = Seq.empty

  def withIntent(intent: Intent): Reply = {
    intents +:= intent
    this
  }

  def withIntent(intents: Seq[Intent]): Reply = {
    this.intents ++= intents
    this
  }

  def withTextReply(sender: Chat, text: String): Reply = {
    this.intents +:= ReplyMessageIntent(sender, text)
    this
  }

  def withMessageReply(sender: Chat, message: OutgoingMessage): Reply = {
    this.intents +:= ReplyMessageIntent(sender, message)
    this
  }
}
