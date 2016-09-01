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

import scalabot.common.bot.{BotState, Conversation, Exit, Reply}
import scalabot.common.message.{Intent, ReplyMessageIntent}
import scalabot.examples.teamnotification.{TeamNotificationBot, TeamNotificationData}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait ComeBackConversationProvider {
  this: TeamNotificationBot =>

  case class ReturnConversation(data: TeamNotificationData) extends Conversation {
    override def initialState: BotState =  BotState {
      case intent: Intent =>
        val user = data.users(intent.sender)
        if (user.isSick || user.isVocation) {
          data.updateUsers(intent.sender, user.copy(isSick = false, isVocation = false))
          Reply(Exit).withIntent(ReplyMessageIntent(intent.sender, "Welcome back. Now you will receive all notifications and will be able to receive a flag"))
        } else {
          Reply(Exit).withIntent(ReplyMessageIntent(intent.sender, "I'm glad to hear it, but i haven't known that you were absent"))
        }
    }
  }
}
