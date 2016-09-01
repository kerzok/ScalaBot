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

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by Nikolay.Smelik on 7/6/2016.
  */

class Webhook(host: String, port: Int) extends Actor with ActorLogging {
  implicit val system: ActorSystem = context.system
  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val materializer = ActorMaterializer()

  private[this] var binder: Future[ServerBinding] = _
  private[this] var routes: Route = _
  private[this] var isWorking: Boolean = false

  def startListening(): Unit = {
    binder = Http().bindAndHandle(routes, host, port)
    binder.onFailure {
      case ex: Exception => ex.printStackTrace()
    }
    if (!isWorking) log.info(s"Webhook start on $host:$port")
    isWorking = true
  }

  def restartListening() = {
    val futureResult = binder flatMap(_.unbind())
    Await.result(futureResult, 5 seconds)
    startListening()
  }

  override def receive: Receive = {
    case route: Route => if (routes == null) routes = route else routes = routes ~ route
    case StartWebhook => if (isWorking) restartListening() else startListening()
    case PoisonPill => binder flatMap(_.unbind())
  }
}

case object StartWebhook

