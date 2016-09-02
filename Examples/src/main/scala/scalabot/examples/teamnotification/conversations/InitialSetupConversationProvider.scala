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

package scalabot.examples.teamnotification.conversations

import scalabot.common.bot.{BotState, Conversation, MoveToConversation, Reply}
import scalabot.common.message.{ReplyMessageIntent, _}
import scalabot.examples.teamnotification.{TeamNotificationBot, TeamNotificationData, User}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait InitialSetupConversationProvider {
  this: TeamNotificationBot =>

  class InitialSetupConversation(data: TeamNotificationData) extends Conversation {
    val setNationality: BotState = BotState {
      case NumberIntent(sender, value) if value == 1 || value == 2 =>
        val conversation = bundle.getObject[Conversation]("storedConversation")
        val intent = bundle.getObject[Intent]("storedIntent")
        value match {
          case 1 => data.users = data.users + (sender -> User(RUSSIA))
          case 2 => data.users = data.users + (sender -> User(GERMANY))
        }
        Reply(MoveToConversation(conversation, intent)).withIntent(ReplyMessageIntent(sender, "Thank you, let's begin"))
      case intent: Intent =>
        Reply(setNationality).withIntent(ReplyMessageIntent(intent.sender, "Invalid arg"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        Reply(setNationality).withIntent(ReplyMessageIntent(intent.sender,
          """
            |Before we begin please answer for a few questions.
            |Select your country:
            |1) Russia
            |2) Germany
          """.stripMargin))
    }
  }
}
