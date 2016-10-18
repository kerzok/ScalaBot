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
import scalabot.common.bot.{BotState, Conversation, Exit, Reply}
import scalabot.common.chat.Chat
import scalabot.common.extensions._
import scalabot.common.message.{Intent, ReplyMessageIntent}
import scalabot.examples.teamnotification._

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait SendNotificationConversationProvider {
  this: TeamNotificationBot with SocketExtension =>

  case class NotificationConversation(data: TeamNotificationData) extends Conversation {
    val germanUsersSender: BotState = BotState {
      case ResultIntent(sender, holidays: GermanHoliday) =>
        val isRussianHoliday = bundle.getBoolean("isRussianHoliday")
        var outputIntents = Seq[Intent]()
        /*val germanUsers = bundle.getObjectOpt[Seq[Chat]]("german")
        val russianUsers = bundle.getObjectOpt[Seq[Chat]]("russian")
        if (holidays.holidays.arr.isEmpty) {
          val notification = bundle.getObject[Notification]("notification")
          outputIntents = germanUsers.map(ReplyMessageIntent(_, notification.text))
          if (isRussianHoliday) {
            outputIntents = outputIntents ++ germanUsers.map(chat => ReplyMessageIntent(chat, "By the way there is holiday in Russia"))
          }
        } else {
          if (germanUsers.nonEmpty) {
            outputIntents = russianUsers.map(chat => ReplyMessageIntent(chat, "By the way there is holiday in Germany"))
          }
        }*/
        Reply(Exit).withIntent(outputIntents)
    }

    val russianUsersSender: BotState = BotState {
      case ResultIntent(sender, holidayCalendar: RussianHoliday) =>
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        var outputIntents = Seq[Intent]()
        holidayCalendar.data \ year.toString \ month.toString \ day.toString \ "isWorking" match {
          case JInt(value) if value == 2 =>
            bundle.put("isRussianHoliday", true)
          case _ =>
            bundle.put("isRussianHoliday", false)
            val russianUsers = bundle.getObjectOpt[Seq[Chat]]("russian")
            val notification = bundle.getObject[Notification]("notification")
            outputIntents = russianUsers.map(_.map(ReplyMessageIntent(_, notification.text))).getOrElse(Seq())
        }
        checkGermanHoliday(sender)
        Reply(germanUsersSender).withIntent(outputIntents)
    }

    override def initialState: BotState = BotState {
      case intent@ScheduleIntent(_, notification: Notification) =>
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
          Reply(Exit)
        } else {
          data.getTeamById(notification.teamId) match {
            case Some(team) =>
              bundle.put("notification", notification)
              bundle.put("team", team)
              checkRussianHoliday(intent.sender)
              Reply(russianUsersSender)
            case _ =>
              Reply(Exit)
          }
        }
    }
  }
}
