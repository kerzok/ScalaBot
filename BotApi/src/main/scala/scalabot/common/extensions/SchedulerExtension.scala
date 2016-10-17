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

package scalabot.common.extensions

import java.util.concurrent.TimeUnit._
import java.util.{Calendar, Date}

import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import org.quartz.CronExpression
import scalabot.common.chat.Chat
import scalabot.common.message.Intent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Created by Nikolay.Smelik on 7/25/2016.
  */
trait SchedulerExtension extends BotExtension {
  private[this] var scheduler = QuartzSchedulerExtension(system)

  final def repeatEvery(duration: Duration, intent: ScheduleIntent, startDate: Date = new Date()) = {
    duration match {
      case Duration(time, DAYS) =>
        val (hour, minute, second) = getTimeFromDate(startDate)
        scheduler.createSchedule(intent.name, cronExpression = s"$second $minute $hour */$time * ?")
        scheduler.schedule(intent.name, self, intent, Some(startDate))
      case Duration(time, HOURS) =>
        val (_, minute, second) = getTimeFromDate(startDate)
        scheduler.createSchedule(intent.name, cronExpression = s"$second $minute */$time ? * *")
        scheduler.schedule(intent.name, self, intent, Some(startDate))
      case Duration(time, MINUTES) =>
        val (_, _, second) = getTimeFromDate(startDate)
        scheduler.createSchedule(intent.name, cronExpression = s"$second */$time * ? * *")
        scheduler.schedule(intent.name, self, intent, Some(startDate))
      case Duration(time, SECONDS) =>
        scheduler.createSchedule(intent.name, cronExpression = s"*/$time * * ? * *")
        scheduler.schedule(intent.name, self, intent, Some(startDate))
      case _ => throw new UnsupportedOperationException("Duration less than minutes is unsupported")
    }
  }

  final def recreateScheduler(): Unit = {
    scheduler.shutdown()
    scheduler = QuartzSchedulerExtension(system)
  }

  final def repeat(cronExpression: String, intent: ScheduleIntent) = {
    if (CronExpression.isValidExpression(cronExpression)) {
      scheduler.createSchedule(intent.name, cronExpression = cronExpression)
      scheduler.schedule(intent.name, self, intent)
    } else {
      throw new IllegalArgumentException(s"invalid cron expression $cronExpression")
    }
  }

  final def wait(duration: FiniteDuration, intent: ScheduleIntent) = {
    system.scheduler.scheduleOnce(duration, self, intent)
  }

  final def delete(name: String): Boolean = {
    scheduler.cancelJob(name)
  }

  final def isValidCronExpression(expression: String): Boolean = {
    CronExpression.isValidExpression(expression)
  }

  final private def getTimeFromDate(date: Date): (Int, Int, Int) = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    (hour, minute, second)
  }
}

case class ScheduleIntent(name: String, data: Any) extends Intent {
  override val sender: Chat = scalabot.common.chat.System()
}


