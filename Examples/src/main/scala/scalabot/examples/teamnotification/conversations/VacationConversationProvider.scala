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
trait VacationConversationProvider {
  this: TeamNotificationBot =>

  case class VacationConversation(data: TeamNotificationData) extends Conversation {
    val vacationUntilState: BotState = BotState {
      case TextIntent(sender, text) =>
        val sdf = new SimpleDateFormat("dd MM yyyy")
        val sickDateOpt = Try(sdf.parse(text)).toOption
        sickDateOpt match {
          case Some(date) =>
            val user = data.users(sender)
            data.updateUsers(sender, user.copy(isVocation = true))
            Reply(Exit).withIntent(ReplyMessageIntent(sender, s"I will not disturb you until ${sdf.format(date)}."))
          case None =>
            Reply(vacationUntilState).withIntent(ReplyMessageIntent(sender, "Invalid date format"))
        }
    }

    val vacationUntilChoose: BotState = BotState {
      case PositiveIntent(sender, _) =>
        Reply(vacationUntilState).withIntent(ReplyMessageIntent(sender, "Please enter the end date of your vacation in format dd mm yyyy"))
      case NegativeIntent(sender, _) =>
        Reply(Exit).withIntent(ReplyMessageIntent(sender, "Okay, I will ask you about it when your turn of flag holder will come."))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        val flags = data.teams.foldLeft(Seq[Flag]()){case (list, team) => list ++ team.flags}
        val user = data.users(intent.sender)
        data.updateUsers(intent.sender, user.copy(isVocation = true))
        val intents = flags.filter(flag => flag.holder == intent.sender).map(flag => {
          val oldHolder = flag.holder
          val newHolder = flag.participants.head
          flag.participants = flag.participants.tail :+ oldHolder
          flag.holder = newHolder
          ReplyMessageIntent(newHolder, s"You are the new flag holder for flag ${flag.name} because of current flag holder has vacation")
        })

        Reply(vacationUntilChoose).withIntent(ReplyMessageIntent(intent.sender, "Do you know the end date of your vacation?"))
          .withIntent(intents)
    }
  }
}
