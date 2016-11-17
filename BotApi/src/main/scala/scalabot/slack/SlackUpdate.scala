package scalabot.slack

import akka.actor.ActorRef
import org.json4s.JsonAST.{JField, JString}
import org.json4s._
import org.json4s.native.Serialization.write
import org.json4s.JsonDSL._

import scalabot.common.message.incoming.SourceMessage
import org.json4s.Extraction
import org.json4s.native.JsonMethods._

/**
  * Created by kerzo on 15.11.2016.
  */

sealed trait SlackUpdate extends SourceMessage {
  def toStringJson: String = {
    implicit val formats = SlackUpdate.formats
    pretty(render(Extraction.decompose(this)))
  }
}


case object Goodbye extends SlackUpdate

case object Hello extends SlackUpdate

case class UnexpectedEvent(messageType: String, json: Option[String] = None) extends SlackUpdate

class Message(user: String,
              text: String,
              ts: String) extends SlackUpdate

case object EmptyMessage extends SlackUpdate

case class TextMessage(channel: String,
                       user: String,
                       text: String,
                       ts: String,
                       replyTo: Option[String] = None) extends Message(user, text, ts)

case class BotMessage(botId: String,
                      username: Option[String] = None,
                      text: String,
                      ts: String) extends Message(botId, text, ts)

case class ChannelArchiveMessage(members: Seq[String],
                                 user: String,
                                 text: String,
                                 ts: String) extends Message(user, text, ts)

case class ChannelJoinMessage(user: String,
                              text: String,
                              ts: String) extends Message(user, text, ts)

case class ChannelLeaveMessage(user: String,
                               text: String,
                               ts: String) extends Message(user, text, ts)

case class ChannelNameMessage(oldName: String,
                              name: String,
                              user: String,
                              text: String,
                              ts: String) extends Message(user, text, ts)

case class ChannelPurposeMessage(purpose: String,
                                 user: String,
                                 text: String,
                                 ts: String) extends Message(user, text, ts)

case class ChannelTopicMessage(topic: String,
                               user: String,
                               text: String,
                               ts: String) extends Message(user, text, ts)

case class ChannelUnarchiveMessage(user: String,
                                   text: String,
                                   ts: String) extends Message(user, text, ts)

case class UnexpectedMessage(subtype: String, json: Option[String] = None) extends Message("", "", "")

case class GroupArchiveMessage(members: Seq[String],
                               user: String,
                               text: String,
                               ts: String) extends Message(user, text, ts)

case class GroupJoinMessage(user: String,
                            text: String,
                            ts: String) extends Message(user, text, ts)

case class GroupLeaveMessage(user: String,
                             text: String,
                             ts: String) extends Message(user, text, ts)

case class GroupNameMessage(oldName: String,
                            name: String,
                            user: String,
                            text: String,
                            ts: String) extends Message(user, text, ts)

case class GroupPurposeMessage(purpose: String,
                               user: String,
                               text: String,
                               ts: String) extends Message(user, text, ts)

case class GroupTopicMessage(topic: String,
                             user: String,
                             text: String,
                             ts: String) extends Message(user, text, ts)

case class GroupUnarchiveMessage(user: String,
                                 text: String,
                                 ts: String) extends Message(user, text, ts)

case class TeamJoin(user: User) extends SlackUpdate

case class ResponseMessage(ok: Boolean,
                           replyTo: Int,
                           ts: Option[String] = None,
                           error: Option[Error] = None) extends SlackUpdate

case class Ping(id: Int) extends SlackUpdate
case class Pong(replyTo: Int) extends SlackUpdate
case class TextMessageResponse(id: Int, channel: String, text: String) extends SlackUpdate

case object Disconnect extends SlackUpdate
case class ConnectionEstablished(ref: ActorRef) extends SlackUpdate
case class ReconnectUrl(url: String) extends SlackUpdate
case class ErrorMessage(error: Error) extends SlackUpdate

case object SlackUpdate {
  implicit lazy val formats = DefaultFormats + SlackUpdateSerializer + MessageSerializer

  object SlackUpdateSerializer extends CustomSerializer[SlackUpdate](format => ({
    case json: JValue => SlackUpdate.parseUpdate(json)
  }, {
    case update: SlackUpdate => Extraction.decompose(update)
  }))

  object MessageSerializer extends CustomSerializer[Message](format => ({
    case json: JValue => SlackUpdate.parseMessage(json)
  }, {
    case Ping(id) => ("id" -> id) ~ ("type" -> "ping")
    case TextMessageResponse(id, channel, text) => ("type" -> "message") ~ ("id" -> id) ~ ("channel" -> channel) ~ ("text" -> text)
    case message: Message => Extraction.decompose(message)
  }))

  def parseUpdate(json: JValue): SlackUpdate = {
    val jsonType = json \ "type"
    jsonType match {
      case jValue@JString("goodbye") => Goodbye
      case jValue@JString("hello") => Hello
      case jValue@JString("message") => json.camelizeKeys.extract[Message]
      case jValue@JString("team_join") => json.camelizeKeys.extract[TeamJoin]
      case jValue@JString("pong") => json.camelizeKeys.extract[Pong]
      case jValue@JString("reconnect_url") => json.camelizeKeys.extract[ReconnectUrl]
      case jValue@JString("error") => json.camelizeKeys.extract[ErrorMessage]
      case jValue@JString(messageType) => UnexpectedEvent(messageType)
      case JNothing => json \ "ok" match {
        case JBool(value) => json.camelizeKeys.extract[ResponseMessage]
        case _ => UnexpectedEvent("undefined", Some(write(json)))
      }
      case _ => UnexpectedEvent("undefined", Some(write(json)))
    }
  }

  def parseMessage(json: JValue): Message = {
    val jsonSubtype = json \ "subtype"
    jsonSubtype match {
      case jValue@JString("bot_message") => json.camelizeKeys.extract[BotMessage]
      case jValue@JString("channel_archive") => json.camelizeKeys.extract[ChannelArchiveMessage]
      case jValue@JString("channel_join") => json.camelizeKeys.extract[ChannelJoinMessage]
      case jValue@JString("channel_leave") => json.camelizeKeys.extract[ChannelLeaveMessage]
      case jValue@JString("channel_name") => json.camelizeKeys.extract[ChannelNameMessage]
      case jValue@JString("channel_purpose") => json.camelizeKeys.extract[ChannelPurposeMessage]
      case jValue@JString("channel_topic") => json.camelizeKeys.extract[ChannelTopicMessage]
      case jValue@JString("channel_unarchive") => json.camelizeKeys.extract[ChannelUnarchiveMessage]
      case jValue@JString("group_archive") => json.camelizeKeys.extract[GroupArchiveMessage]
      case jValue@JString("group_join") => json.camelizeKeys.extract[GroupJoinMessage]
      case jValue@JString("group_leave") => json.camelizeKeys.extract[GroupLeaveMessage]
      case jValue@JString("group_name") => json.camelizeKeys.extract[GroupNameMessage]
      case jValue@JString("group_purpose") => json.camelizeKeys.extract[GroupPurposeMessage]
      case jValue@JString("group_topic") => json.camelizeKeys.extract[GroupTopicMessage]
      case jValue@JString("group_unarchive") => json.camelizeKeys.extract[GroupUnarchiveMessage]
      case jValue@JString(subtype) => UnexpectedMessage(subtype)
      case JNothing => json.camelizeKeys.extract[TextMessage]
      case _ => UnexpectedMessage("undefined", Some(write(json)))
    }
  }
}
