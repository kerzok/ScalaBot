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

package scalabot.examples.tunnel

import scalabot.common.bot._
import scalabot.common.chat.{System, UserChat}
import scalabot.common.message._

class TunnelBot extends AbstractBot[EmptyData] {
  override var data: EmptyData = EmptyData()

  override def id: String = "TunnelBot"
  override def helpMessage: String =
    """
      |This bot help you communicate with people from other messengers.
      |For now is available Telegram, Skype and Slack
      |Commands:
      |connect [source] [nickname] - create tunnel between you and a person with [nickname] from [source] messenger. [source] might be "skype", "slack", "telegram"
      |abort connection - close tunnel if exists
    """.stripMargin
  override def unknownMessage: String = "I don't understand you, please type help for more information"

  override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, text) if text startsWith "connect" =>
      TunnelConversation(data)(intent)
    case intent@TextIntent(_, "show list")  =>
      ShowListConversation(data)(intent)
  }

  case class TunnelConversation(data: EmptyData) extends Conversation {
    val requestState: BotState = BotState {
      case SystemPositiveIntent(chat) =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        val destChat: UserChat = bundle.getObject[UserChat]("destChat")
        Reply(ConnectionEstablished(destChat))
            .withIntent(ReplyMessageIntent(sourceChat, "Connection successfully established"))
      case SystemNegativeIntent(chat) =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sourceChat, "Connection refused"))
      case intent: Intent =>
        Reply(requestState)
          .withIntent(ReplyMessageIntent(intent.sender, "Wait agreement from user, please get patience"))
    }

    val requestForChannelState = BotState {
      case SystemPositiveIntent(_) =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        val destChat: UserChat = bundle.getObject[UserChat]("destChat")
        Reply(requestState)
          .withIntent(ReplyMessageIntent(sourceChat, s"Wait for agreement from user ${destChat.userFullName}"))
      case SystemNegativeIntent(_) =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        val destChat: UserChat = bundle.getObject[UserChat]("destChat")
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sourceChat, s"User ${destChat.userFullName} is busy. Please try again later"))
    }

    override def initialState:BotState = BotState {
      case TextIntent(sender: UserChat, text) =>
        val args = text split " "
        if (args.length == 3) {
          val destChatOpt = getChat(args(1), args(2))
          destChatOpt match {
            case Some(destChat: UserChat) =>
              bundle.put("sourceChat", sender)
              bundle.put("destChat", destChat)
              Reply(requestForChannelState)
                .withIntent(AskChangeStateIntent(destChat, sender,
                  AgreementConversation(data).appendBundle(bundle)))
            case None | Some(_) =>
              Reply(Exit)
                .withIntent(ReplyMessageIntent(sender, "There is no user with such combination of source and id"))
          }
        } else {
          Reply(Exit)
            .withIntent(ReplyMessageIntent(sender, "Invalid args"))
        }
      case intent: Intent =>
        Reply(Exit)
          .withIntent(ReplyMessageIntent(intent.sender, "Group chat is not supported"))
    }
  }

  case class AgreementConversation(data: EmptyData) extends Conversation {
    val agreementState: BotState = BotState ({
      case PositiveIntent(sender: UserChat, _) =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        Reply(ConnectionEstablished(sourceChat))
          .withIntent(SystemPositiveIntent(sourceChat))
          .withIntent(ReplyMessageIntent(sender, "Connection established"))
      case NegativeIntent(sender: UserChat, _) =>
        Reply(Exit)
          .withIntent(SystemNegativeIntent(sender))
          .withIntent(ReplyMessageIntent(sender, "Connection refused"))
      case intent: Intent =>
        Reply(agreementState)
          .withIntent(ReplyMessageIntent(intent.sender, outcoming.TextMessage("Invalid operation, please type yes or no")))
    }, false)

    override def initialState:BotState = BotState ({
      case intent: Intent =>
        val sourceChat: UserChat = bundle.getObject[UserChat]("sourceChat")
        val destChat: UserChat = bundle.getObject[UserChat]("destChat")
        Reply(agreementState)
          .withIntent(ReplyMessageIntent(destChat, s"User from ${sourceChat.source} with nick ${sourceChat.userFullName} want to talk with you.\n" +
            s"Would you agree to talk with him?\nyes/no"))
    }, false)
  }

  case class ShowListConversation(data: EmptyData) extends Conversation {
    override def initialState: BotState = BotState {
      case intent: Intent =>
        val reply: String = data.chats
          .filter(user => user.isInstanceOf[UserChat])
          .map(chat => chat.asInstanceOf[UserChat])
          .foldLeft("Available users:\n")((result: String, user: UserChat) => {
            result + user.id + " " + user.source + " " + user.userFullName + "\n"
          })
        Reply(Exit)
          .withIntent(ReplyMessageIntent(intent.sender, reply))
      }
    }

  case class ConnectionEstablished(destinationChat: UserChat) extends BotState {
    override val canChange: Boolean = false

    override def handleIntent = {
      case TextIntent(sender, text) if text equals "abort connection" =>
        Reply(Exit)
          .withIntent(RequireChangeStateIntent(destinationChat, sender, Idle()))
          .withIntent(ReplyMessageIntent(sender, "Connection aborted"))
          .withIntent(ReplyMessageIntent(destinationChat, "Connection aborted"))
      case textIntent: TextIntent =>
        Reply(this)
          .withIntent(ReplyMessageIntent(destinationChat, textIntent.text))
      case _ => Reply(this)
    }
  }
}