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

package scalabot.telegram

import org.json4s.JsonAST.JString
import org.json4s.ext.EnumNameSerializer
import org.json4s.{CustomSerializer, DefaultFormats, Extraction, JValue}
import org.json4s._
import scalabot.common.message.incoming.SourceMessage
import scalabot.telegram
import scalabot.telegram.MessageEntityType.MessageEntityType

/**
  * Created by Nikolay.Smelik on 7/5/2016.
  */

case class TelegramObject[T](ok: Boolean,
                             result: T)

object MessageEntityType extends Enumeration {
  type MessageEntityType = Value
  val mention, hashTag, botCommand, url, email, bold, italic, code, pre, textLink = Value
}


case class User(id: Long,
                firstName: String,
                lastName: Option[String] = None,
                username: Option[String] = None)

sealed trait Chat {
  val id: Long
}

case class PrivateChat(id: Long,
                       firstName: String,
                       lastName: String,
                       username: Option[String] = None) extends Chat

case class GroupChat(id: Long,
                     title: String) extends Chat

case class SuperGroupChat(id: Long,
                          title: String) extends Chat

case class ChannelChat(id: Long,
                       title: String,
                       username: Option[String] = None) extends Chat

case object TelegramUpdate {
  implicit val formats = DefaultFormats +
    MessageEntitySerializer +
    ChatSerializer +
    new EnumNameSerializer(telegram.MessageEntityType)

  object MessageEntitySerializer extends CustomSerializer[MessageEntity](format => ( {
    case json: JValue => parseMessageEntity(json)
  }, {
    case messageEntity: MessageEntity => Extraction.decompose(messageEntity)
  }))

  object ChatSerializer extends CustomSerializer[Chat](format => ( {
    case json: JValue => parseChat(json)
  }, {
    case chat: Chat => Extraction.decompose(chat)
  }))

  def parseChat(json: JValue): Chat = {
    val jsonType = json \ "type"
    jsonType match {
      case JString("private") => json.camelizeKeys.extract[PrivateChat]
      case JString("group") => json.camelizeKeys.extract[GroupChat]
      case JString("supergroup") => json.camelizeKeys.extract[SuperGroupChat]
      case JString("channel") => json.camelizeKeys.extract[ChannelChat]
    }
  }

  def parseMessageEntity(json: JValue): MessageEntity = {
    val jsonType = json \ "type"
    jsonType match {
      case JString("mention") => MessageEntity(MessageEntityType.mention, json)
      case JString("hashtag") => MessageEntity(MessageEntityType.hashTag, json)
      case JString("bot_command") => MessageEntity(MessageEntityType.botCommand, json)
      case JString("url") => MessageEntity(MessageEntityType.url, json)
      case JString("email") => MessageEntity(MessageEntityType.email, json)
      case JString("bold") => MessageEntity(MessageEntityType.bold, json)
      case JString("italic") => MessageEntity(MessageEntityType.italic, json)
      case JString("code") => MessageEntity(MessageEntityType.code, json)
      case JString("pre") => MessageEntity(MessageEntityType.pre, json)
      case JString("text_link") => MessageEntity(MessageEntityType.textLink, json)
    }
  }
}

case class Message(messageId: Long,
                   from: User,
                   date: Long,
                   chat: Chat,
                   forwardFrom: Option[User] = None,
                   forwardDate: Option[Long] = None,
                   replyToMessage: Option[Message] = None,
                   text: Option[String] = None,
                   entities: Option[Seq[MessageEntity]] = None,
                   audio: Option[Audio] = None,
                   document: Option[Document] = None,
                   photo: Option[Array[PhotoSize]] = None,
                   sticker: Option[Sticker] = None,
                   video: Option[Video] = None,
                   voice: Option[Voice] = None,
                   caption: Option[String] = None,
                   contact: Option[Contact] = None,
                   location: Option[Location] = None,
                   venue: Option[Venue] = None,
                   newChatMember: Option[User] = None,
                   leftChatMember: Option[User] = None) {
  def sender = chat.id
}

case class MessageEntity(entityType: MessageEntityType,
                         offset: Long,
                         length: Long,
                         url: Option[String] = None)

case object MessageEntity {
  private implicit val formats = DefaultFormats
  def apply(messageEntityType: MessageEntityType, json: JValue) = {
    new MessageEntity(
      messageEntityType, (json \ "offset").extract[Long],
      (json \ "length").extract[Long],
      (json \ "url").extractOpt[String]
    )
  }
}

case class PhotoSize(fileId: String,
                     width: Long,
                     height: Long,
                     fileSize: Option[Long] = None)

case class Audio(fileId: String,
                 duration: Long,
                 performer: Option[String] = None,
                 title: Option[String] = None,
                 mimeType: Option[String] = None,
                 fileSize: Option[Long] = None)

case class Document(fileId: String,
                    thumb: Option[PhotoSize] = None,
                    fileName: Option[String] = None,
                    mimeType: Option[String] = None,
                    fileSize: Option[Long] = None)

case class Sticker(fileId: String,
                   width: Long,
                   height: Long,
                   thumb: Option[PhotoSize] = None,
                   fileSize: Option[Long] = None)

case class Video(fileId: String,
                 width: Long,
                 height: Long,
                 duration: Long,
                 thumb: Option[PhotoSize] = None,
                 mimeType: Option[String] = None,
                 fileSize: Option[Long] = None)

case class Voice(fileId: String,
                 duration: Long,
                 mimeType: Option[String] = None,
                 fileSize: Option[Long] = None)

case class Contact(phoneNumber: String,
                   firstName: String,
                   lastName: Option[String] = None,
                   userId: Option[Long] = None)

case class Location(longitude: Float,
                    latitude: Float)

case class Venue(location: Location,
                 title: String,
                 address: String,
                 foursquareId: Option[String] = None)

case class UserProfilePhotos(totalCount: Long,
                             photos: Seq[Seq[PhotoSize]])

case class File(fileId: String,
                fileSize: Option[Long] = None,
                filePath: Option[String] = None)

case class ReplyKeyboardMarkup(keyboard: Seq[Seq[KeyboardButton]],
                               resizeKeyboard: Option[Boolean] = None,
                               oneTimeKeyboard: Option[Boolean] = None,
                               selective: Option[Boolean] = None)

case class KeyboardButton(text: String,
                          requestContact: Option[Boolean] = None,
                          requestLocation: Option[Boolean] = None)

case class ReplyKeyboardHide(hideKeyboard: Boolean,
                             selective: Option[Boolean] = None)

case class InlineKeyboardMarkup(inlineKeyboard: Seq[Seq[InlineKeyboardMarkup]])

case class InlineKeyboardButton(text: String,
                                url: Option[String],
                                callbackData: Option[String],
                                switchInlineQuery: Option[String])

case class CallbackQuery(id: String,
                         from: User,
                         message: Option[Message] = None,
                         inlineMessageId: Option[String] = None,
                         data: Option[String] = None)

case class InlineQuery(id: String,
                       from: User,
                       location: Option[Location] = None,
                       query: String,
                       offset: String)

case class ChosenInlineResult(resultId: String,
                              from: User,
                              location: Option[Location] = None,
                              inlineMessageId: Option[String] = None,
                              query: String)

trait InlineQueryResult

case class InputTextMessageContent(message_text: String,
                                   parseMode: Option[String] = None,
                                   disableWebPagePreview: Option[Boolean] = None) extends InlineQueryResult

case class Update(updateId: Long,
                  message: Option[Message] = None,
                  inlineQuery: Option[InlineQuery] = None,
                  chosenInlineResult: Option[ChosenInlineResult] = None,
                  callbackQuery: Option[CallbackQuery] = None) extends SourceMessage