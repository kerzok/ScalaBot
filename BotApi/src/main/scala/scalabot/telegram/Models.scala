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

import scalabot.common.message.incoming.SourceMessage
import scalabot.telegram.ChatType.ChatType
import scalabot.telegram.MessageEntityType.MessageEntityType

/**
  * Created by Nikolay.Smelik on 7/5/2016.
  */


object ChatType extends Enumeration {
  type ChatType = Value
  val `private`, group, supergroup, channel = Value
}

object MessageEntityType extends Enumeration {
  type MessageEntityType = Value
  val mention, hashtag, bot_command, url, email, bold, italic, code, pre, text_link = Value
}

case class TelegramObject[T](ok: Boolean,
                             result: T)


case class User(id: Long,
                first_name: String,
                last_name: Option[String] = None,
                username: Option[String] = None)

case class Chat(id: Long,
                `type`: ChatType,
                title: Option[String] = None,
                username: Option[String] = None,
                first_name: Option[String] = None,
                last_name: Option[String] = None)

class Message(messageId: Long,
              from: User,
              date: Long,
              chat: Chat,
              forwardFrom: Option[User] = None,
              forwardDate: Option[Long] = None,
              replyToMessage: Option[Message] = None)

case class TextMessage(messageId: Long,
                       from: User,
                       date: Long,
                       chat: Chat,
                       forwardFrom: Option[User] = None,
                       forwardDate: Option[Long] = None,
                       replyToMessage: Option[Message] = None,
                       text: String) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class EntitiesMessage(messageId: Long,
                           from: User,
                           date: Long,
                           chat: Chat,
                           forwardFrom: Option[User] = None,
                           forwardDate: Option[Long] = None,
                           replyToMessage: Option[Message] = None,
                           entities: Seq[MessageEntity]) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class AudioMessage(messageId: Long,
                        from: User,
                        date: Long,
                        chat: Chat,
                        forwardFrom: Option[User] = None,
                        forwardDate: Option[Long] = None,
                        replyToMessage: Option[Message] = None,
                        audio: Audio) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class DocumentMessage(messageId: Long,
                           from: User,
                           date: Long,
                           chat: Chat,
                           forwardFrom: Option[User] = None,
                           forwardDate: Option[Long] = None,
                           replyToMessage: Option[Message] = None,
                           document: Document) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class StickerMessage(messageId: Long,
                          from: User,
                          date: Long,
                          chat: Chat,
                          forwardFrom: Option[User] = None,
                          forwardDate: Option[Long] = None,
                          replyToMessage: Option[Message] = None,
                          sticker: Sticker) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class VideoMessage(messageId: Long,
                        from: User,
                        date: Long,
                        chat: Chat,
                        forwardFrom: Option[User] = None,
                        forwardDate: Option[Long] = None,
                        replyToMessage: Option[Message] = None,
                        video: Video) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class VoiceMessage(messageId: Long,
                        from: User,
                        date: Long,
                        chat: Chat,
                        forwardFrom: Option[User] = None,
                        forwardDate: Option[Long] = None,
                        replyToMessage: Option[Message] = None,
                        voice: Voice) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class CaptionMessage(messageId: Long,
                          from: User,
                          date: Long,
                          chat: Chat,
                          forwardFrom: Option[User] = None,
                          forwardDate: Option[Long] = None,
                          replyToMessage: Option[Message] = None,
                          caption: String) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class ContactMessage(messageId: Long,
                          from: User,
                          date: Long,
                          chat: Chat,
                          forwardFrom: Option[User] = None,
                          forwardDate: Option[Long] = None,
                          replyToMessage: Option[Message] = None,
                          contact: Contact) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class LocationMessage(messageId: Long,
                           from: User,
                           date: Long,
                           chat: Chat,
                           forwardFrom: Option[User] = None,
                           forwardDate: Option[Long] = None,
                           replyToMessage: Option[Message] = None,
                           location: Location) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class VenueMessage(messageId: Long,
                        from: User,
                        date: Long,
                        chat: Chat,
                        forwardFrom: Option[User] = None,
                        forwardDate: Option[Long] = None,
                        replyToMessage: Option[Message] = None,
                        venue: Venue) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class NewChatMemberMessage(messageId: Long,
                                from: User,
                                date: Long,
                                chat: Chat,
                                forwardFrom: Option[User] = None,
                                forwardDate: Option[Long] = None,
                                replyToMessage: Option[Message] = None,
                                user: User) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class LeftChatMemberMessage(messageId: Long,
                                 from: User,
                                 date: Long,
                                 chat: Chat,
                                 forwardFrom: Option[User] = None,
                                 forwardDate: Option[Long] = None,
                                 replyToMessage: Option[Message] = None,
                                 user: User) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class UndefinedMessage(messageId: Long,
                            from: User,
                            date: Long,
                            chat: Chat,
                            forwardFrom: Option[User] = None,
                            forwardDate: Option[Long] = None,
                            replyToMessage: Option[Message] = None) extends Message(messageId, from, date, chat, forwardFrom, forwardDate, replyToMessage)

case class OldMessage(message_id: Long,
                   from: User,
                   date: Long,
                   chat: Chat,
                   forward_from: Option[User] = None,
                   forward_date: Option[Long] = None,
                   reply_to_message: Option[Message] = None,
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
                   new_chat_member: Option[User] = None,
                   left_chat_member: Option[User] = None,
                   new_chat_title: Option[String] = None,
                   new_chat_photo: Option[Array[PhotoSize]] = None,
                   delete_chat_photo: Option[Boolean] = None,
                   group_chat_created: Option[Boolean] = None,
                   supergroup_chat_created: Option[Boolean] = None,
                   channel_chat_created: Option[Boolean] = None,
                   migrate_to_chat_id: Option[Long] = None,
                   migrate_from_chat_id: Option[Long] = None,
                   pinned_message: Option[Message] = None) {
  def sender = chat.id
}

case class MessageEntity(`type`: MessageEntityType,
                         offset: Long,
                         length: Long,
                         url: Option[String] = None)

case class PhotoSize(file_id: String,
                     width: Long,
                     height: Long,
                     file_size: Option[Long] = None)

case class Audio(file_id: String,
                 duration: Long,
                 performer: Option[String] = None,
                 title: Option[String] = None,
                 mime_type: Option[String] = None,
                 file_size: Option[Long] = None)

case class Document(file_id: String,
                    thumb: Option[PhotoSize] = None,
                    file_name: Option[String] = None,
                    mime_type: Option[String] = None,
                    file_size: Option[Long] = None)

case class Sticker(file_id: String,
                   width: Long,
                   height: Long,
                   thumb: Option[PhotoSize] = None,
                   file_size: Option[Long] = None)

case class Video(file_id: String,
                 width: Long,
                 height: Long,
                 duration: Long,
                 thumb: Option[PhotoSize] = None,
                 mime_type: Option[String] = None,
                 file_size: Option[Long] = None)

case class Voice(file_id: String,
                 duration: Long,
                 mime_type: Option[String] = None,
                 file_size: Option[Long] = None)

case class Contact(phone_number: String,
                   first_name: String,
                   last_name: Option[String] = None,
                   user_id: Option[Long] = None)

case class Location(longitude: Float,
                    latitude: Float)

case class Venue(location: Location,
                 title: String,
                 address: String,
                 foursquare_id: Option[String] = None)

case class UserProfilePhotos(total_count: Long,
                             photos: Seq[Seq[PhotoSize]])

case class File(file_id: String,
                file_size: Option[Long] = None,
                file_path: Option[String] = None)

case class ReplyKeyboardMarkup(keyboard: Seq[Seq[KeyboardButton]],
                               resize_keyboard: Option[Boolean] = None,
                               one_time_keyboard: Option[Boolean] = None,
                               selective: Option[Boolean] = None)

case class KeyboardButton(text: String,
                          request_contact: Option[Boolean] = None,
                          request_location: Option[Boolean] = None)

case class ReplyKeyboardHide(hide_keyboard: Boolean,
                             selective: Option[Boolean] = None)

case class InlineKeyboardMarkup(inline_keyboard: Seq[Seq[InlineKeyboardMarkup]])

case class InlineKeyboardButton(text: String,
                                url: Option[String],
                                callback_data: Option[String],
                                switch_inline_query: Option[String])

case class CallbackQuery(id: String,
                         from: User,
                         message: Option[Message] = None,
                         inline_message_id: Option[String] = None,
                         data: Option[String] = None)

case class InlineQuery(id: String,
                       from: User,
                       location: Option[Location] = None,
                       query: String,
                       offset: String)

case class ChosenInlineResult(result_id: String,
                              from: User,
                              location: Option[Location] = None,
                              inline_message_id: Option[String] = None,
                              query: String)

trait InlineQueryResult

case class InputTextMessageContent(message_text: String,
                                   parse_mode: Option[String] = None,
                                   disable_web_page_preview: Option[Boolean] = None) extends InlineQueryResult

case class Update(update_id: Long,
                  message: Option[OldMessage] = None,
                  inline_query: Option[InlineQuery] = None,
                  chosen_inline_result: Option[ChosenInlineResult] = None,
                  callback_query: Option[CallbackQuery] = None) extends SourceMessage