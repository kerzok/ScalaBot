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
import scalabot.common.chat.Chat
import scalabot.common.message._
import scalabot.examples.teamnotification.{Team, TeamNotificationBot}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait AgreementConversationProvider {
  this: TeamNotificationBot =>

  class AgreementConversation extends Conversation {
    val agreementState: BotState = BotState {
      case PositiveIntent(sender, _) =>
        val team = bundle.getObject[Team]("team")
        val destChat = bundle.getObject[Chat]("destChat")
        val replies = (team.teammates :+ team.admin).map(user => ReplyMessageIntent(user, s"User ${destChat.from.displayName} joined to team ${team.name}"))
        data.updateTeams(team.withTeammate(destChat))
        Reply(Exit)
          .withIntent(SystemPositiveIntent(destChat))
          .withIntent(replies)
      case NegativeIntent(sender, _) =>
        val team = bundle.getObject[Team]("team")
        val destChat = bundle.getObject[Chat]("destChat")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, s"You refused application for join to team ${team.name} from user ${destChat.from.displayName}"))
          .withIntent(SystemNegativeIntent(destChat))
      case intent: Intent =>
        Reply(agreementState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid operation, please type yes or no"))
    }

    override def initialState: BotState = BotState {
      case _ =>
        val team = bundle.getObject[Team]("team")
        val destChat = bundle.getObject[Chat]("destChat")
        Reply(agreementState)
          .withIntent(ReplyMessageIntent(team.admin,
            s"User from ${destChat.source} with nick ${destChat.from.displayName} want to join to your team ${team.name}.\n" +
              s"Do you approve to join for your team?\n" +
              s"yes/no"))
    }
  }
}
