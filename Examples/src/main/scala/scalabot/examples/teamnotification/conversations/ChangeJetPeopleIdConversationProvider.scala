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
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}
import scalabot.examples.teamnotification.TeamNotificationBot

/**
  * Created by Nikolay.Smelik on 8/31/2016.
  */
trait ChangeJetPeopleIdConversationProvider {
  this: TeamNotificationBot =>

  case class ChangeJetPeopleIdConversation() extends Conversation {
    val setJetPeopleIdState: BotState = BotState {
      case TextIntent(sender, login) =>
        val userInfo = data.users(sender)
        val userInfoWithNewJetPeopleId = userInfo.copy(jetPeopleLogin = login)
        data.updateUsers(sender, userInfoWithNewJetPeopleId)
        Reply(Exit).withIntent(ReplyMessageIntent(sender, "JetBrains login was successfully changed!"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent => Reply(setJetPeopleIdState).withIntent(ReplyMessageIntent(intent.sender, "Enter your JetPeople login"))
    }
  }
}
