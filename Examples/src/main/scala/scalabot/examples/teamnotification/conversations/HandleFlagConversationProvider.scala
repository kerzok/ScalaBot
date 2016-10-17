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
import scalabot.common.chat.{Chat, System}
import scalabot.common.extensions.{ResultIntent, ScheduleIntent}
import scalabot.common.message.ReplyMessageIntent
import scalabot.examples.teamnotification._

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait HandleFlagConversationProvider {
  this: TeamNotificationBot =>

  case class FlagNotificationConversation(data: TeamNotificationData) extends Conversation {
    def sendNotification(): Reply = {
      val flag = bundle.getObject[Flag]("flag")
      val isRussianHoliday = bundle.getBoolean("isRussianHoliday")
      val isGermanHoliday = bundle.getBoolean("isGermanHoliday")
      val isHolderHoliday = if (data.users(flag.holder).country == RUSSIA) isRussianHoliday else isGermanHoliday
      if (isHolderHoliday) {
        flag.duration match {
          case ONE_DAY =>
            scrollParticipantsSeq(flag, isRussianHoliday, isGermanHoliday)
            Reply(Exit)
              .withIntent(ReplyMessageIntent(flag.holder,
                s"You are new flag holder for flag ${flag.name}, because of there is a holiday in current flag holder country"))
              .withIntent(ReplyMessageIntent(flag.holder, flag.text))
          case _ => Reply(Exit)
        }
      } else if (flag.isGoingToExpired) {
        flag.resetDays()
        val oldHolder = flag.holder
        scrollParticipantsSeq(flag, isRussianHoliday, isGermanHoliday)
        Reply(Exit)
          .withIntent(ReplyMessageIntent(oldHolder, flag.text))
          .withIntent(ReplyMessageIntent(flag.holder, s"You are new flag holder for flag ${flag.name}"))
      } else {
        flag.incDays()
        Reply(Exit).withIntent(ReplyMessageIntent(flag.holder, flag.text))
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
        checkGermanHoliday(sender)
        Reply(todayHolidayHandler)
      case ResultIntent(sender, holidays: GermanHoliday) =>
        bundle.put("isGermanHoliday", holidays.holidays.arr.nonEmpty)
        sendNotification()
    }

    override def initialState: BotState = BotState {
      case intent@ScheduleIntent(_, flag: Flag) =>
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
          Reply(Exit)
        } else {
          bundle.put("flag", flag)
          checkRussianHoliday(intent.sender)
          Reply(todayHolidayHandler)
        }
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
  }
}
