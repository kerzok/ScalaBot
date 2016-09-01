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

import scalabot.common.message.incoming.SourceMessage

/**
  * Created by Nikolay.Smelik on 7/8/2016.
  */

trait BaseChannel {val id: String}
case class Channel(override val id: String,
                   creator: String,
                   name: String,
                   topic: Option[ChannelInfo],
                   unread_count: Option[Int],
                   members: Option[List[String]]) extends BaseChannel

case class Im(override val id: String,
              user: String,
              latest: Option[Update]) extends BaseChannel

case class ChannelInfo(value: String,
                       creator: String,
                       last_set: Long)

case class DirectChannel(id: String,
                         userId: String)

case class Team(id: String,
                name: String,
                email_domain: String,
                domain: String)

case class User(id: String,
                name: String,
                deleted: Boolean,
                is_admin: Option[Boolean],
                is_owner: Option[Boolean],
                is_primary_owner: Option[Boolean],
                is_restricted: Option[Boolean],
                is_ultra_restricted: Option[Boolean],
                has_files: Option[Boolean],
                is_bot: Option[Boolean],
                profile: Option[Profile])

case class Profile(first_name: String,
                   last_name: String,
                   real_name: String,
                   email: String,
                   skype: String,
                   phone: String)

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

case class Update(`type`: String,
                  url: Option[String] = None,
                  channel: Option[String] = None,
                  user: Option[String] = None,
                  text: Option[String] = None,
                  team: Option[String] = None) extends SourceMessage

case class Response(id: Int,
                    `type`: String,
                    channel: String,
                    text: String)
