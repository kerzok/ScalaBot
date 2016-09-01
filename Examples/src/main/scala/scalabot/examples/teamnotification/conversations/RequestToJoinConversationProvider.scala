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
import scalabot.common.chat.{Chat, System}
import scalabot.common.message._
import scalabot.examples.teamnotification.{Team, TeamNotificationBot, TeamNotificationData}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait RequestToJoinConversationProvider {
  this: TeamNotificationBot with AgreementConversationProvider =>

  case class RequestToJoinConversation(data: TeamNotificationData) extends Conversation {
    val requestState: BotState = BotState {
      case SystemPositiveIntent(sender: Chat) =>
        val team = bundle.getObject[Team]("team")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, s"You joined to the team ${team.name}"))
      case SystemNegativeIntent(sender: Chat) =>
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, "Your application was refused by administrator"))
    }

    val requestForChannelState: BotState = BotState {
      case SystemPositiveIntent(System) =>
        val team = bundle.getObject[Team]("team")
        val chat = bundle.getObject[Chat]("destChat")
        Reply(requestState)
          .withIntent(ReplyMessageIntent(chat, s"Wait for agreement from user ${team.admin.from.displayName}"))
      case SystemNegativeIntent(System) =>
        val team = bundle.getObject[Team]("team")
        val chat = bundle.getObject[Chat]("destChat")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(chat, s"User ${team.admin.from.displayName} is busy. Please try again later"))
    }

    override def initialState: BotState = BotState {
      case TextIntent(sender, text) =>
        text split " " match {
          case Array("join", "team", teamName) =>
            data.teams.find(team => team.name == teamName) match {
              case Some(team) =>
                if (team.admin == sender || team.teammates.contains(sender)) {
                  Reply(Exit)
                    .withIntent(ReplyMessageIntent(sender, "You already are a member of this team"))
                } else {
                  bundle.put("destChat", sender)
                  bundle.put("team", team)
                  Reply(requestForChannelState)
                    .withIntent(AskChangeStateIntent(team.admin, sender, new AgreementConversation().appendBundle(bundle)))
                }
              case None => Reply(Exit)
                .withIntent(ReplyMessageIntent(sender, s"There is no team with such name: $teamName"))
            }
        }
    }
  }
}
