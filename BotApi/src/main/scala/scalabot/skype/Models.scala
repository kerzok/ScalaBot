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

package scalabot.skype

import scalabot.common.message.incoming.SourceMessage
import scalabot.skype.ActivityType.ActivityType

/**
  * Created by Nikolay.Smelik on 7/8/2016.
  */
case class Update(id: String,
                  activity: ActivityType,
                  from: String,
                  to: String,
                  time: String,
                  content: Option[String],
                  views: Option[Seq[View]],
                  name: Option[String],
                  action: Option[String],
                  fromDisplayName: Option[String],
                  membersAdded: Option[Seq[String]],
                  membersRemoved: Option[Seq[String]],
                  topicName: Option[String],
                  historyDisclosed: Option[Seq[Boolean]]) extends SourceMessage

case class View(viewId: String,
                size: Long)

case class Message(content: String)

case class Activities(message: Message)

case class Attachments(originalBase64: String,
                       thumbnailBase64: Option[String] = None,
                       `type`: String,
                       name: String)

case class AttachmentResponse(attachmentId: String,
                              activityId: Option[String] = None)

abstract class ErrorObject (error: Option[String],
                            error_description: Option[String])

case class AccessTokenObject(token_type: Option[String],
                             expires_in: Option[Int],
                             ext_expires_in: Option[Int],
                             access_token: Option[String],
                             error: Option[String],
                             error_description: Option[String]) extends ErrorObject(error, error_description)

class SkypeException(name:String,
                     description: String) extends Exception {
  override def toString: String = "Some error has occurred in Skype Api client:\nerror name:\n" + name + "\ndescription:\n" + description
}

class Empty

object ActivityType extends Enumeration {
  type ActivityType = Value
  val message, attachment, contactRelationUpdate, conversationUpdate = Value
}


