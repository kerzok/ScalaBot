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

package scalabot.common.web

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill}
import akka.io.IO
import org.json4s.native.JsonMethods._

import scalabot.Implicits._
import scalabot.{common, slack}
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket.WebSocketClientWorker
import spray.can.websocket.frame.{CloseFrame, StatusCode, TextFrame}
import spray.http.{HttpHeaders, HttpMethods, HttpRequest}

import scala.util.Try

/**
  * Created by Nikolay.Smelik on 7/12/2016.
  */
case class WebSocket(sourceRef: ActorRef) extends Actor with WebSocketClientWorker {
  private[this] val defaultWebSocketKey = "MTAxMTAwMDEwMDExMTAwMTEwMTEwMDExMDAwMDAwMTAxMTEwMDAxMTEwMDEwMDAwMDAxMDAwMTAxMDEwMDAwMTAxMDAwMDEwMDExMTAwMTExMDAxMDAxMTAxMDEwMDEwMTAwMDEwMDAwMDExMDAxMTExMDAwMTExMTEwMDAwMTENCg=="
  override def receive = connect orElse handshaking orElse closeLogic

  private def connect(): Receive = {
    case WebSocketHelper.Connect(host, port, resource, ssl) =>
      val headers = List(
        HttpHeaders.Host(host, port),
        HttpHeaders.Connection("Upgrade"),
        HttpHeaders.RawHeader("Upgrade", "websocket"),
        HttpHeaders.RawHeader("Sec-WebSocket-Version", "13"),
        HttpHeaders.RawHeader("Sec-WebSocket-Key", Try(common.BotConfig.get("api.slack.webSocketKey")).toOption getOrElse defaultWebSocketKey))
      request = HttpRequest(HttpMethods.GET, resource, headers)
      IO(UHttp)(ActorSystem("websocket")) ! Http.Connect(host, port, ssl)
  }

  override def businessLogic = {
    case WebSocketHelper.Release => close()
    case TextFrame(msg) =>
      val stringMessage = msg.utf8String
      if (stringMessage.contains("reply_to")) {
        sourceRef ! None
      } else if (stringMessage.contains("type")) {
        val update = parse(stringMessage).extract[slack.Update]
        sourceRef ! update
      } else {
        sourceRef ! stringMessage
      }
    case WebSocketHelper.Send(message) =>
      send(message)
    case ignoreThis => // ignore
  }

  def send(message: String) = connection ! TextFrame(message)

  def close() = if (connection != null) connection ! CloseFrame(StatusCode.NormalClose)

  private var request: HttpRequest = _

  override def upgradeRequest = request

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    close()
  }

}

object WebSocketHelper {

  sealed trait WebSocketMessage

  case class Connect(
                      host: String,
                      port: Int,
                      resource: String,
                      withSsl: Boolean = false) extends WebSocketMessage

  case class Send(msg: String) extends WebSocketMessage

  case object Release extends WebSocketMessage

}