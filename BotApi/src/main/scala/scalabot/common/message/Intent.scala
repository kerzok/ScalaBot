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

package scalabot.common.message

import scalabot.common.bot.Conversation
import scalabot.common.chat.Chat
import scalabot.common.message.outcoming.OutgoingMessage

/**
  * Created by Nikolay.Smelik on 7/21/2016.
  */
trait Intent extends Serializable {
  val sender: Chat
}

case class SystemPositiveIntent(sender: Chat) extends Intent
case class SystemNegativeIntent(sender: Chat) extends Intent

//region TextIntents

class TextIntent(val sender: Chat,
                 val text: String) extends Intent

object TextIntent {
  def apply(sender: Chat, text: String) = new TextIntent(sender, text)
  def unapply(arg: TextIntent): Option[(Chat, String)] = Some(arg.sender, arg.text)
}

case class PositiveIntent(override val sender: Chat, override val text: String) extends TextIntent(sender, text)


case class NegativeIntent(override val sender: Chat, override val text: String) extends TextIntent(sender, text)

case class NumberIntent(override val sender: Chat,
                        value: Int) extends TextIntent(sender, value.toString)
//endregion

//region Conversation changers

abstract class ChangeStateIntent extends Intent {
  val recipient: Chat
  val newState: Conversation
  val innerIntent: Intent
}

case class AskChangeStateIntent(override val recipient: Chat,
                                override val sender: Chat,
                                override val newState: Conversation,
                                override val innerIntent: Intent) extends ChangeStateIntent

object AskChangeStateIntent {
  def apply(recipient: Chat, sender: Chat, newState: Conversation) = new AskChangeStateIntent(recipient, sender, newState, EmptyIntent(sender))
}

case class RequireChangeStateIntent(override val recipient: Chat,
                                    override val sender: Chat,
                                    override val newState: Conversation,
                                    override val innerIntent: Intent) extends ChangeStateIntent

object RequireChangeStateIntent {
  def apply(recipient: Chat, sender: Chat, newState: Conversation) = new RequireChangeStateIntent(recipient, sender, newState, EmptyIntent(sender))
}

class ReplyMessageIntent(override val sender: Chat,
                         val message: OutgoingMessage) extends Intent
//endregion

object ReplyMessageIntent {
  def apply(recipient: Chat, message: OutgoingMessage): ReplyMessageIntent = new ReplyMessageIntent(recipient, message)
  def apply(recipient: Chat, message: String): ReplyMessageIntent = new ReplyMessageIntent(recipient, outcoming.TextMessage(message))

  def unapply(arg: ReplyMessageIntent): Option[(Chat, OutgoingMessage)] = Some(arg.sender, arg.message)
}

case class EmptyIntent(sender: Chat) extends Intent

