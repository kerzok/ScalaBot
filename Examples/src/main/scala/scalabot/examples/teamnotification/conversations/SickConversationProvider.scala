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

import java.text.SimpleDateFormat

import scalabot.common.bot.{BotState, Conversation, Exit, Reply}
import scalabot.common.message._
import scalabot.examples.teamnotification.{Flag, TeamNotificationBot, TeamNotificationData}

import scala.util.Try

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait SickConversationProvider {
  this: TeamNotificationBot =>

  case class SickConversation(data: TeamNotificationData) extends Conversation {
    val sickUntilState: BotState = BotState {
      case TextIntent(sender, text) =>
        val sdf = new SimpleDateFormat("dd MM yyyy")
        val sickDateOpt = Try(sdf.parse(text)).toOption
        sickDateOpt match {
          case Some(date) =>
            val user = data.users(sender)
            data.updateUsers(sender, user.copy(isSick = true))
            Reply(Exit).withIntent(ReplyMessageIntent(sender, s"I will not disturb you until ${sdf.format(date)}.\nGet well soon!"))
          case None =>
            Reply(sickUntilState).withIntent(ReplyMessageIntent(sender, "Invalid date format"))
        }
    }

    val sickUntilChoose: BotState = BotState {
      case PositiveIntent(sender) =>
        Reply(sickUntilState).withIntent(ReplyMessageIntent(sender, "Please enter the end date of your sick leave in format dd mm yyyy"))
      case NegativeIntent(sender) =>
        Reply(Exit).withIntent(ReplyMessageIntent(sender, "Okay, I will ask you about it when your turn of flag holder will come.\nGet well soon!"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        val flags = data.teams.foldLeft(Seq[Flag]()){case (list, team) => list ++ team.flags}
        val user = data.users(intent.sender)
        data.updateUsers(intent.sender, user.copy(isSick = true))
        val intents = flags.filter(flag => flag.holder == intent.sender).map(flag => {
          val oldHolder = flag.holder
          val newHolder = flag.participants.head
          flag.participants = flag.participants.tail :+ oldHolder
          flag.holder = newHolder
          ReplyMessageIntent(newHolder, s"You are new flag holder for flag ${flag.name} because of current flag holder has ill")
        })

        Reply(sickUntilChoose).withIntent(ReplyMessageIntent(intent.sender, "Do you know the end date of your sick leave?"))
          .withIntent(intents)
    }
  }
}
