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

package scalabot.examples.teamnotification.states

import scalabot.common.bot._
import scalabot.common.message.{Intent, NegativeIntent, PositiveIntent}
import scalabot.examples.teamnotification.conversations.AddNotificationConversationProvider
import scalabot.examples.teamnotification.{TeamNotificationBot, TeamNotificationData}

/**
  * Created by Nikolay.Smelik on 8/17/2016.
  */
trait AddNotificationStateProvider {
  this: TeamNotificationBot with AddNotificationConversationProvider =>

  case class AddNotificationAgree(bundle: Bundle, data: TeamNotificationData) extends BotState {
    override def handleIntent = {
      case PositiveIntent(sender, _) =>
        Reply(MoveToConversation(new AddNotificationConversation(data).appendBundle(bundle)))
      case NegativeIntent(sender, _) =>
        Reply(Exit)
      case _ => Reply(this)
    }
  }
}
