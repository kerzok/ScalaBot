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

package scalabot.examples.notification

import java.util.concurrent.atomic.AtomicInteger

import scalabot.common.bot._
import scalabot.common.chat.{Chat, System, UserChat}
import scalabot.common.extensions.{ScheduleIntent, SchedulerExtension}
import scalabot.common.message.{ReplyMessageIntent, _}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * Created by Nikolay.Smelik on 7/25/2016.
  */
final class NotificationBot extends AbstractBot[NotificationData] with SchedulerExtension {
  override protected var data: NotificationData = NotificationData()

  override protected def id: String = "NotificationBot"

  override def helpMessage: String = "There is notification bot"

  override def unknownMessage: String = "Something goes wrong"

  override def startConversation: PartialFunction[Intent, Conversation] = {
    case textIntent@TextIntent(_, text) if text == "create new team" =>
      new CreateTeamConversation(data)(textIntent)
    case textIntent@TextIntent(_, text) if text == "manage teams" =>
      new ManageTeamsConversation(data)(textIntent)
    case textIntent@TextIntent(_, text) if text.startsWith("join team") =>
      new RequestToJoinConversation(data)(textIntent)
    case notificationIntent: ScheduleIntent =>
      new SendNotification(data)(notificationIntent)
  }

  class SendNotification(data: NotificationData) extends Conversation {
    override def initialState: BotState = BotState {
      case ScheduleIntent(_, notification: Notification) =>
        val outputIntents = (notification.team.teammates :+ notification.team.admin).map(chat => ReplyMessageIntent(chat, outcoming.TextMessage(notification.text)))
        Reply(Exit).withIntent(outputIntents)
    }
  }

  class CreateTeamConversation(data: NotificationData) extends Conversation {
    val enterTeamState = BotState {
      case textIntent: TextIntent =>
        val team = Team(data.index.incrementAndGet(), textIntent.text, textIntent.sender)
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

  class AddNotificationConversation(data: NotificationData) extends Conversation {
    val createNotificationState: BotState = BotState {
      case TextIntent(sender, cronExpression) if isValidCronExpression(cronExpression) =>
        val text = bundle.getString("notificationText")
        val team = bundle.getObject[Team]("team")
        val notification = Notification(team.name + data.notificationId.incrementAndGet(), text, cronExpression, team)
        repeat(cronExpression, ScheduleIntent(notification.id, notification))
        team.notifications = team.notifications :+ notification
        bundle.put("team", team)
        data.updateTeams(team)
        Reply(AddNotificationAgree(bundle, data))
          .withIntent(ReplyMessageIntent(sender, "Notification successfully created!\nWould you like to add one more notification?"))
      case TextIntent(sender, duration) if Try(Duration.apply(duration)).isSuccess =>
        val text = bundle.getString("notificationText")
        val team = bundle.getObject[Team]("team")
        val notification = Notification(team.name + data.notificationId.incrementAndGet(), text, duration, team)
        repeatEvery(Duration(duration), ScheduleIntent(notification.id, notification))
        team.notifications = team.notifications :+ notification
        bundle.put("team", team)
        data.updateTeams(team)
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

  class ManageTeamsConversation(data: NotificationData) extends Conversation {
    val changeNameState: BotState = BotState {
      case textIntent: TextIntent =>
        val team = bundle.getObject[Team]("team")
        val oldName = team.name
        team.name = textIntent.text
        data.updateTeams(team)
        val replies = team.teammates.map(chat => ReplyMessageIntent(chat, s"Name of the team $oldName was changed to ${team.name}"))
        Reply(Exit)
          .withIntent(replies)
          .withIntent(ReplyMessageIntent(textIntent.sender, "Name of the team was changed"))
    }

    val deleteNotificationState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= bundle.getObject[Team]("team").notifications.size =>
        val team = bundle.getObject[Team]("team")
        val notification = team.notifications(value - 1)
        delete(notification.id)
        team.notifications = team.notifications.filter(existNotification => existNotification != notification)
        data.updateTeams(team)
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, "Notification was deleted"))
      case intent: Intent =>
        Reply(deleteNotificationState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    val selectOperationState: BotState = BotState {
      case NumberIntent(sender, value) if value > 0 && value <= 4 =>
        value match {
          case 1 => Reply(changeNameState).withIntent(ReplyMessageIntent(sender, "Enter new name"))
          case 2 => Reply(MoveToConversation(new AddNotificationConversation(data).appendBundle(bundle)))
          case 3 if bundle.getObject[Team]("team").notifications.nonEmpty =>
            val team = bundle.getObject[Team]("team")
            val notifications = team.notifications
            val notificationsNumeratedList = notifications.foldLeft(("Notifications, available for remove:\n", 1)) {
              case ((result, number), notification) => (result + number + ") " + notification.text + "\n", number + 1)
            }._1
            Reply(deleteNotificationState)
              .withIntent(ReplyMessageIntent(sender, notificationsNumeratedList))
          case 3 => Reply(Exit).withIntent(ReplyMessageIntent(sender, "You have no any notifications"))
          case 4 => Reply(Exit)
          case _ => Reply(selectOperationState).withIntent(ReplyMessageIntent(sender, "Invalid number"))
        }
      case intent: Intent =>
        Reply(selectOperationState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
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
            s"4) Cancel"))
      case intent: Intent =>
        Reply(selectTeamsState)
          .withIntent(ReplyMessageIntent(intent.sender, "Invalid number"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        val teams = data.teams.filter(team => team.admin == intent.sender).toList
        if (teams.nonEmpty) {
          val teamsNumeratedList = printListWithHeader(teams, (team: Team) => { team.name }, Some("Teams, available for manage:\n"))
          bundle.put("teams", teams)
          Reply(selectTeamsState).withIntent(ReplyMessageIntent(intent.sender, teamsNumeratedList))
        } else {
          Reply(Exit).withIntent(ReplyMessageIntent(intent.sender, "There is no teams available manage for"))
        }
    }
  }

  class RequestToJoinConversation(data: NotificationData) extends Conversation {
    val requestState: BotState = BotState {
      case SystemPositiveIntent(sender: UserChat) =>
        val team = bundle.getObject[Team]("team")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, s"You joined to the team ${team.name}"))
      case SystemNegativeIntent(sender: UserChat) =>
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, "Your application was refused by administrator"))
    }

    val requestForChannelState: BotState = BotState {
      case SystemPositiveIntent(_) =>
        val team = bundle.getObject[Team]("team")
        val chat = bundle.getObject[Chat]("destChat")
        Reply(requestState)
          .withIntent(ReplyMessageIntent(chat, s"Wait for agreement from user ${team.admin.from.displayName}"))
      case SystemNegativeIntent(_) =>
        val team = bundle.getObject[Team]("team")
        val chat = bundle.getObject[Chat]("destChat")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(chat, s"User ${team.admin.from.displayName} is busy. Please try again later"))
    }

    override def initialState: BotState = BotState {
      case textIntent: TextIntent =>
        textIntent.text split " " match {
          case Array(_, _, teamName) =>
            val teamOpt = data.teams.find(team => team.name == teamName)
            teamOpt match {
              case Some(team) =>
                if (team.admin == textIntent.sender || team.teammates.contains(textIntent.sender)) {
                  Reply(Exit)
                    .withIntent(ReplyMessageIntent(textIntent.sender, "You already are a member of this team"))
                } else {
                  bundle.put("destChat", textIntent.sender)
                  bundle.put("team", team)
                  Reply(requestForChannelState)
                    .withIntent(AskChangeStateIntent(team.admin, textIntent.sender,
                      new AgreementConversation().appendBundle(bundle)))
                }
              case None => Reply(Exit)
                  .withIntent(ReplyMessageIntent(textIntent.sender, s"There is no team with such name: $teamName"))
            }
        }
    }
  }

  class AgreementConversation extends Conversation {
    val agreementState: BotState = BotState {
      case PositiveIntent(sender, _) =>
        val team = bundle.getObject[Team]("team")
        val destChat = bundle.getObject[Chat]("destChat")
        val replies = (team.teammates :+ team.admin).map(user => ReplyMessageIntent(user, s"User ${destChat.from.displayName} joined to team ${team.name}"))
        team.teammates = team.teammates :+ destChat
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

  case class AddNotificationAgree(bundle: Bundle, data: NotificationData) extends BotState {
    override def handleIntent = {
      case PositiveIntent(sender, _) =>
        Reply(MoveToConversation(new AddNotificationConversation(data).appendBundle(bundle)))
      case NegativeIntent(sender, _) =>
        Reply(Exit)
      case _ => Reply(this)
    }
  }

  private[this] def printListWithHeader[T](list: Traversable[T], toString: Function[T, String], header: Option[String] = None): String = {
    list.foldLeft((header.getOrElse(""), 1)) {
      case ((result, number), item) => (result + number + ") " + toString(item) + "\n", number + 1)
    }._1
  }
}

case class NotificationData(teams: mutable.Set[Team] = mutable.Set.empty,
                            notificationId: AtomicInteger = new AtomicInteger(0)) extends Data {
  val index = new AtomicInteger()

  def updateTeams(team: Team): Unit = {
    teams += team
  }
}

case class Notification(id: String, text: String, time: String, team: Team) {
  override def toString: String = s"Notification($id, $text, $time, ${team.name})"
}

case class Team(id: Int, var name: String, admin: Chat, var teammates: Seq[Chat] = Seq.empty,
                var notifications: Seq[Notification] = Seq.empty) {

  override def equals(obj: scala.Any): Boolean = obj match {
    case team: Team => team.id == id
    case _ => false
  }

  override def hashCode(): Int = id
}