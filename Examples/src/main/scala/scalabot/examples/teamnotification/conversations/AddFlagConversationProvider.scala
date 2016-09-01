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

import java.util.{Calendar, Date, UUID}

import scalabot.common.bot.{BotState, Conversation, Exit, Reply}
import scalabot.common.chat.Chat
import scalabot.common.extensions.ScheduleIntent
import scalabot.common.message.{ReplyMessageIntent, _}
import scalabot.examples.teamnotification._

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait AddFlagConversationProvider {
  this: TeamNotificationBot =>

  case class AddFlagConversation(data: TeamNotificationData) extends Conversation {
    def saveNewFlag(sender: Chat): Reply = {
      val builder: FlagBuilder = bundle.getObject[FlagBuilder]("builder")
      val team = bundle.getObject[Team]("team")
      val flag = builder.build()
      repeatEvery(ONE_DAY, ScheduleIntent(flag.id, flag), flag.startTime)
      data.updateTeams(team.withFlag(flag))
      Reply(Exit).withIntent(ReplyMessageIntent(sender, "Flag successfully added!"))
    }

    val addMoreTeammates: BotState = BotState {
      case PositiveIntent(sender) =>
        val team = bundle.getObject[Team]("team")
        val builder = bundle.getObject[FlagBuilder]("builder")
        val candidates = (team.teammates :+ team.admin).filter(!builder.participants.contains(_))
        bundle.put("candidates", candidates)
        val listOfUser = printListWithHeader[Chat](candidates, (chat) => {chat.source + " " + chat.from.displayName}, "Choose teammates can receive flag:\n")
        Reply(addTeammates).withIntent(ReplyMessageIntent(sender, listOfUser))
      case NegativeIntent(sender) =>
        saveNewFlag(sender)
      case intent: Intent =>
        Reply(addMoreTeammates).withIntent(ReplyMessageIntent(intent.sender, "Unknown keyword, please type yes or no"))
    }

    val addTeammates: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 =>
        val candidates = bundle.getObject[Seq[Chat]]("candidates")
        if (value <= candidates.size) {
          val candidate = candidates(value - 1)
          var builder = bundle.getObject[FlagBuilder]("builder")
          builder = builder.withParticipant(candidate)
          bundle.put("builder", builder)
          if (candidates.size == 1) {
            saveNewFlag(sender)
          } else {
            Reply(addMoreTeammates).withIntent(ReplyMessageIntent(sender, "Would you like to add more participants?"))
          }
        } else {
          Reply(addTeammates).withIntent(ReplyMessageIntent(sender, "Invalid arg"))
        }
      case TextIntent(sender, text) =>
        val candidatesId = text.replaceAll(" ", "").split(",").flatMap(maybeInt =>
          scala.util.Try(maybeInt.toInt).toOption).distinct
        if (candidatesId.nonEmpty) {
          val candidates = bundle.getObject[Seq[Chat]]("candidates")
          val newParticipants = candidatesId.flatMap(id => scala.util.Try(candidates(id - 1)).toOption).distinct
          var builder = bundle.getObject[FlagBuilder]("builder")
          builder = builder.withParticipants(newParticipants)
          bundle.put("builder", builder)
          if (newParticipants.length == candidates.size) {
            saveNewFlag(sender)
          } else {
            Reply(addMoreTeammates).withIntent(ReplyMessageIntent(sender, "Would you like to add more participants?"))
          }
        } else {
          Reply(addTeammates).withIntent(ReplyMessageIntent(sender, "Invalid args"))
        }
    }

    val addNotification: BotState = BotState {
      case TextIntent(sender, text) =>
        var builder: FlagBuilder = bundle.getObject[FlagBuilder]("builder")
        val team = bundle.getObject[Team]("team")
        builder = builder.withNotification(text)
        val candidates = (team.teammates :+ team.admin).filterNot(builder.participants.contains(_))
        bundle.put("builder", builder)
        bundle.put("candidates", candidates)
        val listOfUser = printListWithHeader[Chat](candidates, (chat) => {chat.source + " " + chat.from.displayName}, "Choose teammates can receive flag:\n")
        Reply(addTeammates).withIntent(ReplyMessageIntent(sender, listOfUser))
    }

    val addNotificationTime: BotState = BotState {
      case TextIntent(sender, text) =>
        try {
          val splittedTime = text.split(":")
          val hoursStr = splittedTime(0)
          val minutesStr = splittedTime(1)
          val hours = hoursStr.toInt
          val minutes = minutesStr.toInt
          if ((hours < 0 || hours > 24) && (minutes < 0 || minutes > 59))
            throw new IllegalArgumentException()
          val calendar = Calendar.getInstance()
          calendar.set(Calendar.HOUR_OF_DAY, hours)
          calendar.set(Calendar.MINUTE, minutes)
          val date = calendar.getTime
          val builder = bundle.getObject[FlagBuilder]("builder")
          bundle.put("builder", builder.withStartTime(date))
          Reply(addNotification).withIntent(ReplyMessageIntent(sender, "Enter notification text"))
        } catch {
          case ex: Exception => Reply(addNotificationTime).withIntent(ReplyMessageIntent(sender, "Invalid time format, please enter notification time in format hh:mm"))
        }
    }

    val addDuration: BotState = BotState {
      case NumberIntent(sender, value) if value == 1 || value == 2 =>
        var builder: FlagBuilder = bundle.getObject[FlagBuilder]("builder")
        value match {
          case 1 => builder = builder.withDuration(ONE_DAY)
          case 2 => builder = builder.withDuration(ONE_WEEK)
        }
        bundle.put("builder", builder)
        Reply(addNotificationTime).withIntent(ReplyMessageIntent(sender, "Enter time of notification in format hh:mm"))
      case intent: Intent =>
        Reply(addDuration).withIntent(ReplyMessageIntent(intent.sender, "Invalid value, please try again"))
    }

    val addName: BotState = BotState {
      case TextIntent(sender, text) if text.nonEmpty =>
        val team = bundle.getObject[Team]("team")
        val builder = FlagBuilder(UUID.randomUUID().toString, text, team.id)
        bundle.put("builder", builder)
        Reply(addDuration)
          .withIntent(ReplyMessageIntent(sender, """
              |Choose the duration of the flag retention:
              |1) 1 day
              |2) 1 week""".stripMargin))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        Reply(addName)
          .withIntent(ReplyMessageIntent(intent.sender, "Enter name of new flag"))
    }
  }
}
