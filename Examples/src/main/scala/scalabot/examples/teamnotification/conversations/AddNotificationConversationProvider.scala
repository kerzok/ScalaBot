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
import scalabot.common.extensions.ScheduleIntent
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}
import scalabot.examples.teamnotification.states.AddNotificationStateProvider
import scalabot.examples.teamnotification.{Notification, Team, TeamNotificationBot, TeamNotificationData}

import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait AddNotificationConversationProvider {
  this: TeamNotificationBot with AddNotificationStateProvider =>

  class AddNotificationConversation(data: TeamNotificationData) extends Conversation {
    val createNotificationState: BotState = BotState {
      case TextIntent(sender, cronExpression) if isValidCronExpression(cronExpression) =>
        val text = bundle.getString("notificationText")
        var team = bundle.getObject[Team]("team")
        val notification = Notification(UUID.randomUUID().toString, text, cronExpression, team.id)
        repeat(cronExpression, ScheduleIntent(notification.id, notification))
        team = team.withNotification(notification)
        data.updateTeams(team)
        bundle.put("team", team)
        Reply(AddNotificationAgree(bundle, data))
          .withIntent(ReplyMessageIntent(sender, "Notification successfully created!\nWould you like to add one more notification?"))
      case TextIntent(sender, duration) if Try(Duration.apply(duration)).isSuccess =>
        val text = bundle.getString("notificationText")
        var team = bundle.getObject[Team]("team")
        val notification = Notification(UUID.randomUUID().toString, text, duration, team.id)
        repeatEvery(Duration(duration), ScheduleIntent(notification.id, notification))
        team = team.withNotification(notification)
        data.updateTeams(team)
        bundle.put("team", team)
        Reply(AddNotificationAgree(bundle, data))
          .withIntent(ReplyMessageIntent(sender, "Notification successfully created!\nWould you like to add one more notification?"))
      case intent: Intent =>
        Reply(createNotificationState)
          .withIntent(ReplyMessageIntent(intent.sender, "Incorrect time, please enter valid time in cron format"))
    }

    val createNotificationText: BotState = BotState {
      case textIntent: TextIntent =>
        bundle.put("notificationText", textIntent.text)
        Reply(createNotificationState)
          .withIntent(ReplyMessageIntent(textIntent.sender, "Enter time of notification in cron format"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        Reply(createNotificationText)
          .withIntent(ReplyMessageIntent(intent.sender, "Enter text of notification"))
    }
  }
}
