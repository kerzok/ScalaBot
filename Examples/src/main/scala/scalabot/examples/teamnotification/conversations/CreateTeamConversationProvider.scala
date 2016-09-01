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

import java.util.UUID

import scalabot.common.bot.{BotState, Conversation, Reply}
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}
import scalabot.examples.teamnotification.states.AddNotificationStateProvider
import scalabot.examples.teamnotification.{Team, TeamNotificationBot, TeamNotificationData}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait CreateTeamConversationProvider {
  this: TeamNotificationBot with AddNotificationStateProvider =>

  case class CreateTeamConversation(data: TeamNotificationData) extends Conversation {
    val enterTeamState = BotState {
      case textIntent: TextIntent =>
        val team = Team(UUID.randomUUID().toString, textIntent.text, textIntent.sender)
        data.updateTeams(team)
        bundle.put("team", team)
        Reply(AddNotificationAgree(bundle, data))
          .withIntent(ReplyMessageIntent(textIntent.sender, "New team created!\nWould you like to add notifications?\nyes/no"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent => Reply(enterTeamState)
        .withIntent(ReplyMessageIntent(intent.sender, "Enter name of new team"))
    }
  }
}
