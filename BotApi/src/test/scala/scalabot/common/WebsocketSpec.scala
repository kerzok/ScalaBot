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

package scalabot.common

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{FlatSpecLike, Matchers}
import scala.concurrent.duration._
import scalabot.common.web.{WebSocket, WebSocketHelper}
/**
  * Created by Nikolay.Smelik on 8/30/2016.
  */
class WebsocketSpec extends TestKit(ActorSystem("websocketTest")) with ImplicitSender with Matchers with FlatSpecLike {

  it should "receive message" in {
    val wse = system.actorOf(Props(classOf[WebSocket], self))
    wse ! WebSocketHelper.Connect("echo.websocket.org", 443, "/echo", withSsl = true)
    Thread.sleep(3500L)
    val rock = "Rock it with WebSocket"
    wse ! WebSocketHelper.Send(rock)
    expectMsg(10 seconds, rock)
    wse ! WebSocketHelper.Release
  }
}
