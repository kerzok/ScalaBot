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

package scalabot.examples.teamnotification

import java.util.{Calendar, Date}

import akka.actor.ActorRef
import org.json4s.JArray
import org.json4s.JsonAST.JObject
import org.quartz.CronExpression
import scalabot.common.BotConfig
import scalabot.common.bot._
import scalabot.common.chat.{Chat, System}
import scalabot.common.extensions._
import scalabot.common.message._
import scalabot.examples.teamnotification.conversations._
import scalabot.examples.teamnotification.states.AddNotificationStateProvider

import scala.concurrent.duration._

/**
  * Created by Nikolay.Smelik on 7/25/2016.
  */
final class TeamNotificationBot extends AbstractBot[TeamNotificationData]
                              with SchedulerExtension
                              with SocketExtension
                              with TextRazorExtension
                              with HandleFlagConversationProvider
                              with InitialSetupConversationProvider
                              with SendNotificationConversationProvider
                              with AddNotificationConversationProvider
                              with AddFlagConversationProvider
                              with ManageTeamConversationProvider
                              with RequestToJoinConversationProvider
                              with AgreementConversationProvider
                              with ComeBackConversationProvider
                              with SickConversationProvider
                              with CreateTeamConversationProvider
                              with VacationConversationProvider
                              with AddNotificationStateProvider {
  protected val ONE_DAY: Duration = 1 day
  protected val ONE_WEEK: Duration = 7 days
  protected val GERMANY: String = "Germany"
  protected val RUSSIA: String = "Russia"
  override protected val razorApiKey: String = BotConfig.get(id + ".razorApiKey")
  override protected var data: TeamNotificationData = TeamNotificationData()
  override protected def id: String = "TeamNotificationBot"
  override def helpMessage: String = """|Available commands:
                                      |"Join team $teamName" add you to team $teamName
                                      |"Create new team" creates new team
                                      |"Manage teams" manage your teams
                                      |"Sick leave" inform bot that you are sick
                                      |"Vacation" inform bot that you are on vacation
                                      |"Return" inform bot that you have returned from sick leave or vacation
                                    """.stripMargin

  override def unknownMessage: String = """|Something goes wrong.
                                         |Please type help for more information about available commands"
                                      """.stripMargin

  override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, text) if text.nlpMatch("add|create" -> "team" -> "new") => handleSetup(CreateTeamConversation(data), intent)
    case intent@TextIntent(_, text) if text.nlpMatch("teams|manage" -> "manage|teams") => handleSetup(ManageTeamsConversation(data), intent)
    case intent@TextIntent(_, text) if text.matches("(?:J|j)oin team \\w*") => handleSetup(RequestToJoinConversation(data), intent)
    case intent@TextIntent(_, text) if text.matches("(?:S|s)ick leave") => handleSetup(SickConversation(data), intent)
    case intent@TextIntent(_, text) if text.matches("(?:V|v)acation") => handleSetup(VacationConversation(data), intent)
    case intent@TextIntent(_, text) if text.matches("(?:R|r)eturn") => handleSetup(ReturnConversation(data), intent)
    case intent@ScheduleIntent(_, notification: Notification) => NotificationConversation(data)(intent)
    case intent@ScheduleIntent(_, flag: Flag) => FlagNotificationConversation(data)(intent)
  }

  override def recoverState(data: TeamNotificationData) = {
    val notifications = data.teams.flatMap(team => team.notifications)
    val flags = data.teams.flatMap(team => team.flags)
    notifications.foreach(notification => {
      if (CronExpression.isValidExpression(notification.time))
        repeat(notification.time, ScheduleIntent(notification.id, notification))
      else repeatEvery(Duration(notification.time), ScheduleIntent(notification.id, notification))
    })
    flags.foreach(flag => repeatEvery(ONE_DAY, ScheduleIntent(flag.id, flag), flag.startTime))
  }

  protected def checkRussianHoliday(user: Chat = System()) = makeRequest[RussianHoliday](
    SocketIntent(user, "http://basicdata.ru/api/json/calend/", RequestParams(canCache = true)))

  protected def checkGermanHoliday(user: Chat = System()) = {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR).toString
    val month = (calendar.get(Calendar.MONTH) + 1).toString
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString
    val params = RequestParams(canCache = true)
      .putParam("key", BotConfig.get(id + ".holidayApiKey"))
      .putParam("country", "DE")
      .putParam("year", year)
      .putParam("month", month)
      .putParam("day", day)
    makeRequest[GermanHoliday](SocketIntent(user, s"https://holidayapi.com/v1/holidays", params))
  }

  protected def printListWithHeader[T](list: Seq[T], toString: Function[T, String], header: String = ""): String = {
    list.zipWithIndex.foldLeft(header) {
      case (result, (item, number)) => result + (number + 1) + ") " + toString(item) + "\n"
    }
  }

  private[this] def handleSetup(conversation: Conversation, intent: Intent): Conversation = {
    if (data.users.contains(intent.sender)) {
      conversation(intent)
    } else {
      val bundle = Bundle()
      bundle.put("storedConversation", conversation)
      bundle.put("storedIntent", intent)
      val initialSetupConversation = new InitialSetupConversation(data).appendBundle(bundle)
      initialSetupConversation(intent)
    }
  }
}

case class TeamNotificationData(var teams: Set[Team] = Set.empty,
                                var users: Map[Chat, User] = Map.empty) extends Data {

  def getTeamById(id: String): Option[Team] = teams.find(_.id == id)
  def updateTeams(team: Team)(implicit self: ActorRef): Unit = {
    teams = teams.filterNot(_ == team) + team
    self ! SaveSnapshot
  }
  def updateUsers(chat: Chat, user: User)(implicit self: ActorRef): Unit = {
    users = users + (chat -> user)
    self ! SaveSnapshot
  }
}

case class Notification(id: String, text: String, time: String, teamId: String)

case class Flag(id: String, name: String, teamId: String, var holder: Chat,
                var participants: Seq[Chat], text: String, duration: Duration, startTime: Date) {
  private[this] var daysCounter = 0

  def isExpired: Boolean = daysCounter.minutes == duration
  def isGoingToExpired: Boolean = (daysCounter + 1).days == duration
  def resetDays(): Unit = daysCounter = 0
  def incDays(): Unit = daysCounter += 1
}

case class FlagBuilder(id: String, name: String, teamId: String, holder: Chat = null,
                       participants: Seq[Chat] = Seq.empty, text: String = "", duration: Duration = 1 day, startTime: Date = new Date()) {
  def withParticipant(toAdd: Chat) = copy(participants = toAdd +: participants)
  def withParticipants(toAdd: Seq[Chat]) = copy(participants = toAdd ++ participants)
  def withNotification(toAdd: String) = copy(text = toAdd)
  def withDuration(toAdd: Duration) = copy(duration = toAdd)
  def withFlagHolder(toAdd: Chat) = copy(holder = toAdd)
  def withStartTime(toAdd: Date) = copy(startTime = toAdd)
  def build() = {
    val resultHolder = if (holder == null) participants.head else holder
    val resultParticipants = if (holder == null) participants.tail else participants.filterNot(chat => chat == holder)
    Flag(id, name, teamId, resultHolder, resultParticipants, text, duration, startTime)
  }
}

case class Team(id: String,
                name: String,
                admin: Chat,
                teammates: Seq[Chat] = Seq.empty,
                notifications: Seq[Notification] = Seq.empty,
                flags: Seq[Flag] = Seq.empty) {

  def withName(toAdd: String) = copy(name = toAdd)
  def withTeammates(toAdd: Seq[Chat]) = copy(teammates = toAdd)
  def withTeammate(toAdd: Chat) = copy(teammates = teammates :+ toAdd)
  def withNotification(toAdd: Notification) = copy(notifications = notifications :+ toAdd)
  def withFlag(toAdd: Flag) = copy(flags = flags :+ toAdd)
  def withoutFlag(toRemove: Flag) = copy(flags = flags.filterNot(_ == toRemove))
  def withoutNotification(toRemove: Notification) = copy(notifications = notifications.filterNot(_ == toRemove))
  def withoutTeammate(toRemove: Chat) = copy(teammates = teammates.filterNot(_ == toRemove))

  override def equals(obj: scala.Any): Boolean = obj match {
    case team: Team => team.id == id
    case _ => false
  }

  override def hashCode(): Int = id.hashCode
}

case class User(country: String,
                isVocation: Boolean = false,
                isSick: Boolean = false) {
  def canDisturb: Boolean = !(isSick || isVocation)
}

case class RussianHoliday(data: JObject)
case class GermanHoliday(holidays: JArray)