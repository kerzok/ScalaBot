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

package scalabot.examples.bash

import scalabot.common.bot._
import scalabot.common.extensions.{RequestParams, ResultIntent, SocketExtension, SocketIntent}
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}

import scala.util.Random

/**
  * Created by Nikolay.Smelik on 8/8/2016.
  */
final class BashBot() extends AbstractBot[EmptyData] with SocketExtension {
  override protected var data: EmptyData = EmptyData()

  override protected def id: String = "BashBot"

  override def helpMessage: String = "This is bash bot.\nPrint \"bash\" to see new article"

  override def unknownMessage: String = "Unknown command"

  override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, "bash") => new GetNewStoryConversation()(intent)
  }

  class GetNewStoryConversation() extends Conversation {
    val responseFromBashHandler: BotState = BotState {
      case ResultIntent(sender, result: BashResponse) =>
        val itemNumber = Random.nextInt(result.items.size)
        val article = result.items(itemNumber)
        Reply(Exit)
          .withIntent(ReplyMessageIntent(sender, s"<b>${article.title.trim}</b>\n${article.summary.content.trim.replaceAll("<br>", "\n")}"))
    }

    override def initialState: BotState = BotState {
      case intent: Intent =>
        makeRequest[BashResponse](SocketIntent(intent.sender, "http://cloud.feedly.com/v3/streams/contents", RequestParams(canCache = true).putParam("streamId", "feed/https://bash.org.ru/rss/")))
        Reply(responseFromBashHandler)
    }
  }
}

case class BashResponse(items: Seq[Article])
case class Article(title: String, summary: Summary)
case class Summary(content: String)
