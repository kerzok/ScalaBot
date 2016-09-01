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

import java.util.Calendar

import org.json4s.JsonAST.JInt
import scalabot.common.bot._
import scalabot.common.chat.Chat
import scalabot.common.extensions.ResultIntent
import scalabot.common.message.{Intent, NumberIntent, ReplyMessageIntent, TextIntent}
import scalabot.examples.teamnotification._

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait ManageTeamConversationProvider {
  this: TeamNotificationBot
    with AddFlagConversationProvider
    with AddNotificationConversationProvider =>

  case class ManageTeamsConversation(data: TeamNotificationData) extends Conversation {
    val changeNameState: BotState = BotState {
      case TextIntent(sender, text) =>
        val team = bundle.getObject[Team]("team")
        val oldName = team.name
        data.updateTeams(team.withName(text))
        val replies = team.teammates.map(chat => ReplyMessageIntent(chat, s"Name of the team $oldName was changed to $text"))
        Reply(Exit)
          .withIntent(replies)
          .withIntent(ReplyMessageIntent(sender, "Name of the team was changed"))
    }

    val deleteNotificationState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= bundle.getObject[Team]("team").notifications.size =>
        val team = bundle.getObject[Team]("team")
        val notification = team.notifications(value - 1)
        delete(notification.id)
        data.updateTeams(team.withoutNotification(notification))
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, "Notification was deleted"))
      case NumberIntent(sender, value) if value == bundle.getObject[Team]("team").notifications.size + 1 =>
        Reply(Exit)
      case intent: Intent =>
        Reply(deleteNotificationState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    val deleteFlagState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= bundle.getObject[Team]("team").flags.size =>
        val team = bundle.getObject[Team]("team")
        val flag = team.flags(value - 1)
        delete(flag.id)
        data.updateTeams(team.withoutFlag(flag))
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, "Flag was deleted"))
      case NumberIntent(sender, value) if value == bundle.getObject[Team]("team").flags.size + 1 =>
        Reply(Exit)
      case intent: Intent =>
        Reply(deleteFlagState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    def sendNotification(): Reply = {
      val team = bundle.getObject[Team]("team")
      val isRussianHoliday = bundle.getBoolean("isRussianHoliday")
      val isGermanHoliday = bundle.getBoolean("isGermanHoliday")
      val excluded = bundle.getObject[Chat]("excluded")
      val flagsToSetNewHolder = team.flags.filter(flag => flag.holder == excluded)
      flagsToSetNewHolder.foreach(flag => {
        scrollParticipantsSeq(flag, isRussianHoliday, isGermanHoliday)
        flag.participants = flag.participants.filterNot(chat => chat == excluded)
      })
      val messagesToNewHolders = flagsToSetNewHolder.map(flag => ReplyMessageIntent(flag.holder, s"You are new flag holder for flag ${flag.name}, " +
        s"because of previous flag holder ${excluded.from.displayName} was excluded from your team"))
      val newTeam = team.withoutTeammate(excluded)
      val messagesToTeammates = newTeam.teammates.map(chat => ReplyMessageIntent(chat, s"User ${excluded.from.displayName} was excluded from team ${team.name}"))
      data.updateTeams(newTeam)
      Reply(Exit).withIntent(ReplyMessageIntent(team.admin, "User was excluded from your team"))
        .withIntent(ReplyMessageIntent(excluded, s"You was excluded from team ${team.name}"))
        .withIntent(messagesToTeammates)
        .withIntent(messagesToNewHolders)
    }

    def scrollParticipantsSeq(flag: Flag, isRussianHoliday: Boolean, isGermanHoliday: Boolean) = {
      val nextHolder = getNextHolder(flag.participants, isRussianHoliday, isGermanHoliday).getOrElse(flag.holder)
      var participants = flag.participants :+ flag.holder
      while (participants.head != nextHolder) {
        participants = participants.tail :+ participants.head
      }
      flag.holder = participants.head
      flag.participants = participants.tail
    }

    def getNextHolder(participants: Seq[Chat], isRussianHoliday: Boolean, isGermanHoliday: Boolean): Option[Chat] = {
      if (participants.isEmpty) {
        None
      } else if (data.users(participants.head).canDisturb) {
        val isHolderHoliday = if (data.users(participants.head).country == RUSSIA) isRussianHoliday else isGermanHoliday
        if (!isHolderHoliday)
          Some(participants.head)
        else
          getNextHolder(participants.tail, isRussianHoliday, isGermanHoliday)
      } else if (participants.tail.nonEmpty) {
        getNextHolder(participants.tail, isRussianHoliday, isGermanHoliday)
      } else {
        None
      }
    }

    val todayHolidayHandler: BotState = BotState {
      case ResultIntent(sender, holidayCalendar: RussianHoliday) =>
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        holidayCalendar.data \ year.toString \ month.toString \ day.toString \ "isWorking" match {
          case JInt(value) if value == 2 => bundle.put("isRussianHoliday", true)
          case _ => bundle.put("isRussianHoliday", false)
        }
        val chatSender = bundle.getObject[Chat]("sender")
        checkGermanHoliday(chatSender)
        Reply(todayHolidayHandler)
      case ResultIntent(sender, holidays: GermanHoliday) =>
        bundle.put("isGermanHoliday", holidays.holidays.arr.nonEmpty)
        sendNotification()
    }

    val deleteParticipantsState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= bundle.getObject[Team]("team").teammates.size =>
        val team = bundle.getObject[Team]("team")
        val excluded = team.teammates(value - 1)
        team.flags.foreach(flag => flag.participants = flag.participants.filterNot(chat => chat == excluded))
        if (team.flags.exists(flag => flag.holder == excluded)) {
          bundle.put("sender", sender)
          bundle.put("excluded", excluded)
          checkRussianHoliday(sender)
          Reply(todayHolidayHandler)
        } else {
          val newTeam = team.withoutTeammate(excluded)
          val messagesToTeammates = newTeam.teammates.map(chat => ReplyMessageIntent(chat, s"User ${excluded.from.displayName} was excluded from team ${team.name}"))
          data.updateTeams(newTeam)
          Reply(Exit).withIntent(ReplyMessageIntent(sender, "User was excluded from your team"))
            .withIntent(ReplyMessageIntent(excluded, s"You was excluded from team ${team.name}"))
            .withIntent(messagesToTeammates)
        }
      case NumberIntent(sender, value) if value == bundle.getObject[Team]("team").teammates.size + 1 =>
        Reply(Exit)
      case intent: Intent =>
        Reply(deleteFlagState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    val selectOperationState: BotState = BotState {
      case NumberIntent(sender, 1) => Reply(changeNameState).withIntent(ReplyMessageIntent(sender, "Enter new name"))
      case NumberIntent(sender, 2) => Reply(MoveToConversation(new AddNotificationConversation(data).appendBundle(bundle)))
      case NumberIntent(sender, 3) if bundle.getObject[Team]("team").notifications.nonEmpty =>
        val team = bundle.getObject[Team]("team")
        val notificationsNumeratedList = printListWithHeader[Notification](team.notifications, {_.text}, "Notifications, available for remove:\n") + s"${team.notifications.size + 1}) Cancel"
        Reply(deleteNotificationState)
          .withIntent(ReplyMessageIntent(sender, notificationsNumeratedList))
      case NumberIntent(sender, 3) => Reply(Exit).withIntent(ReplyMessageIntent(sender, "You have no any notifications"))
      case NumberIntent(sender, 4) => Reply(MoveToConversation(AddFlagConversation(data).appendBundle(bundle)))
      case NumberIntent(sender, 5) if bundle.getObject[Team]("team").flags.nonEmpty =>
        val team = bundle.getObject[Team]("team")
        val flagNumeratedList = printListWithHeader[Flag](team.flags, {_.name}, "Flags, available for remove:\n") + s"${team.flags.size + 1}) Cancel"
        Reply(deleteFlagState)
          .withIntent(ReplyMessageIntent(sender, flagNumeratedList))
      case NumberIntent(sender, 5) => Reply(Exit).withIntent(ReplyMessageIntent(sender, "You have no any flags"))
      case NumberIntent(sender, 6) if bundle.getObject[Team]("team").teammates.nonEmpty =>
        val team = bundle.getObject[Team]("team")
        val teammatesNumeratedList = printListWithHeader[Chat](team.teammates, {chat => chat.from.displayName + " (" + chat.source + ")"}, "Users, available for remove\n") + s"${team.teammates.size + 1}) Cancel"
        Reply(deleteParticipantsState)
          .withIntent(ReplyMessageIntent(sender, teammatesNumeratedList))
      case NumberIntent(sender, 6) => Reply(Exit).withIntent(ReplyMessageIntent(sender, "There is no teammates in this team"))
      case NumberIntent(sender, 7) => Reply(Exit)
      case intent: Intent => Reply(selectOperationState).withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    val selectTeamsState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= bundle.getObject[List[Team]]("teams").size =>
        val teams = bundle.getObject[List[Team]]("teams")
        val team = teams(value - 1)
        bundle.put("team", team)
        Reply(selectOperationState)
          .withIntent(ReplyMessageIntent(sender, s"What do you want to do with ${team.name}?\n" +
            s"1) Change name\n" +
            s"2) Add notification\n" +
            s"3) Delete notification\n" +
            s"4) Add flag\n" +
            s"5) Delete flag\n" +
            s"6) Delete teammate\n" +
            s"7) Cancel"))
      case intent: Intent =>
        Reply(selectTeamsState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        val teams = data.teams.filter(team => team.admin == intent.sender).toList
        if (teams.nonEmpty) {
          val teamsNumeratedList = printListWithHeader(teams, (team: Team) => { team.name }, "Teams, available for manage:\n")
          bundle.put("teams", teams)
          Reply(selectTeamsState).withIntent(ReplyMessageIntent(intent.sender, teamsNumeratedList))
        } else {
          Reply(Exit).withIntent(ReplyMessageIntent(intent.sender, "There is no teams available manage for"))
        }
    }
  }
}
