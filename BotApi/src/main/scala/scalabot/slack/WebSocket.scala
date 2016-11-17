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

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import org.json4s.native.JsonMethods._


/**
  * Created by Nikolay.Smelik on 7/12/2016.
  */
case class WebSocket(url: String, sourceRef: ActorRef)(implicit val actorSystem: ActorSystem) {
  implicit val materializer = ActorMaterializer()
  implicit val formats = SlackUpdate.formats

  val webSocketFlow = Http().singleWebSocketRequest(ws.WebSocketRequest(url), flow())

  def flow(): Flow[ws.Message, ws.Message, _] = {
    Flow.fromGraph(GraphDSL.create(Source.actorRef[SlackUpdate](10000, OverflowStrategy.fail)) {
      implicit builder => slackSource =>
        import GraphDSL.Implicits._

        val materialization = builder.materializedValue.map(slackServerRef => ConnectionEstablished(slackServerRef))

        val merge = builder.add(Merge[SlackUpdate](2))
        val messageToSlackUpdateFlow = builder.add(Flow[ws.Message].collect {
          case ws.TextMessage.Strict(text) =>
            parse(text).extract[SlackUpdate]
        })
        val slackUpdateToMessageFlow = builder.add(Flow[SlackUpdate].collect {
          case update: SlackUpdate => ws.TextMessage.Strict(update.toStringJson)
        })
        val closeStage = builder.add(getCloseStage)

        val sink = Sink.actorRef[SlackUpdate](sourceRef, Disconnect)
        materialization ~> merge ~> sink
        messageToSlackUpdateFlow ~> merge
        slackSource ~> closeStage ~> slackUpdateToMessageFlow
        FlowShape(messageToSlackUpdateFlow.in, slackUpdateToMessageFlow.out)
    })
  }

  def getCloseStage = new GraphStage[FlowShape[SlackUpdate, SlackUpdate]] {
    val in = Inlet[SlackUpdate]("closeStage.in")
    val out = Outlet[SlackUpdate]("closeStage.out")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = grab(in) match {
          case Disconnect => completeStage()
          case msg => push(out, msg)
        }
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }

    override def shape: Shape = FlowShape.of(in, out)
  }
}

