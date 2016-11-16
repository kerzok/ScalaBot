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

package scalabot.examples.echo

import scalabot.common.bot._
import scalabot.common.chat.Chat
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}

/**
  * Created by Nikolay.Smelik on 8/29/2016.
  */
class EchoBot extends AbstractBot[EmptyData] {
  override protected var data: EmptyData = EmptyData()

  override protected def id: String = "EchoBot"

  override def helpMessage: String = "This is example echo bot"

  override def unknownMessage: String = "I don't understand you"

  override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent: TextIntent => EchoConversation()(intent)
  }

  case class EchoConversation() extends Conversation {
    override def initialState: BotState = BotState {
      case intent: TextIntent => Reply(Exit).withIntent(getIntents(intent.sender, intent.text)) //.withText(intent.sender, intent.text)
    }
  }

  def getIntents(sender: Chat, text: String): Seq[Intent] = {
    (1 to 21).map(_ => ReplyMessageIntent(sender, text))
  }
}