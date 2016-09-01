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

import scalabot.common.bot.{AbstractBot, BotState, Conversation, EmptyData}
import scalabot.common.message.Intent

/**
  * Created by Nikolay.Smelik on 9/1/2016.
  */
class AbstractBotTest {
  /*case class TestBot() extends AbstractBot[EmptyData] {
    override protected var data: EmptyData = _

    override protected def id: String = "TestBot"

    override def helpMessage: String = "Test bot"

    override def unknownMessage: String = "unknown message"

    override def startConversation: PartialFunction[Intent, Conversation] = {
      case _ =>
    }

    case class BaseConversation() extends Conversation {
      override def initialState: BotState = BotState {
        _ => Reply(Exit)
      }
    }
  }*/
}
