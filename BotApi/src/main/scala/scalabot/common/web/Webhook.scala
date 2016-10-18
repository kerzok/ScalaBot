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

  private[this] var routesMap: Map[String, Route] = Map.empty
  private[this] var binder: Future[ServerBinding] = _
  private[this] var isWorking: Boolean = false

  def startListening(routes: Route): Unit = {
    binder = Http().bindAndHandle(routes, host, port)
    binder.onFailure {
      case ex: Exception => ex.printStackTrace()
    }
    if (!isWorking) log.info(s"Webhook start on $host:$port")
    isWorking = true
  }

  def restartListening(routes: Route) = {
    val futureResult = binder flatMap(_.unbind())
    Await.result(futureResult, 5 seconds)
    startListening(routes)
  }

  override def receive: Receive = {
    case AddRoute(id, route) =>
      routesMap += (id -> route)
      val newRoutes = routesMap.values.foldLeft(reject.asInstanceOf[Route])({
        (accum, route) => accum ~ route
      })
      if (isWorking) restartListening(newRoutes) else startListening(newRoutes)
    case StartWebhook => if (isWorking) restartListening(reject) else startListening(reject)
    case StopWebhook =>
      binder.flatMap(_.unbind()).onComplete(_ => {
        log.info("Webhook stopped")
        routesMap = Map.empty
        isWorking = false
      })
    case PoisonPill => binder.flatMap(_.unbind())
  }
}

case class AddRoute(id: String, route: Route)
case object StartWebhook
case object StopWebhook

