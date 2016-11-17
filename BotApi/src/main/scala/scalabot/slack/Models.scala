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

package scalabot.slack

/**
  * Created by Nikolay.Smelik on 7/8/2016.
  */

trait BaseChannel {
  val id: String
}

case class Channel(override val id: String,
                   creator: String,
                   name: String,
                   topic: Option[ChannelInfo],
                   unreadCount: Option[Int],
                   members: Option[List[String]]) extends BaseChannel

case class Im(override val id: String,
              user: String,
              latest: Option[SlackUpdate]) extends BaseChannel

case class ChannelInfo(value: String,
                       creator: String,
                       lastSet: Long)

case class DirectChannel(id: String,
                         userId: String)

case class Team(id: String,
                name: String,
                emailDomain: String,
                domain: String)

case class User(id: String,
                name: String,
                deleted: Boolean,
                isAdmin: Option[Boolean],
                isOwner: Option[Boolean],
                isPrimaryOwner: Option[Boolean],
                isRestricted: Option[Boolean],
                isUltraRestricted: Option[Boolean],
                hasFiles: Option[Boolean],
                isBot: Option[Boolean],
                profile: Option[Profile])

case class Profile(firstName: String,
                   lastName: String,
                   realName: String,
                   email: String,
                   skype: String,
                   phone: String)

case class Bot(id: String,
               name: String)

case class Self(id: String,
                name: String)

case class StartResponse(ok: Boolean,
                         url: String,
                         self: Self,
                         team: Team,
                         users: Seq[User],
                         channels: Seq[Channel],
                         ims: Seq[Im],
                         bots: Seq[User])

case class ChannelRenameInfo(id: String,
                             name: String,
                             created: Long)

case class DndStatus(dndEnabled: Boolean,
                     nextDndStartTs: Long,
                     nextDndEndTs: Long,
                     snoozeEnabled: Boolean,
                     snoozeEndtime: Long)

case class Error(code: Int,
                 msg: String)
