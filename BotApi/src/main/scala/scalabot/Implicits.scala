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

package scalabot

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.{Unmarshaller, _}
import akka.util.ByteString
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.JsonMethods._

import scalabot.skype.ActivityType

/**
  * Created by Nikolay.Smelik on 7/11/2016.
  */
object Implicits {
  implicit val formats = {
    DefaultFormats + new EnumNameSerializer(telegram.ChatType) +
      new EnumNameSerializer(telegram.MessageEntityType) +
      new EnumNameSerializer(ActivityType)
  }

  implicit val telegramUpdateFromEntityUnmarsheller: FromEntityUnmarshaller[telegram.Update] =
    Unmarshaller.withMaterializer {
      implicit ex => implicit mat => entity: HttpEntity =>
        entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
          .map(_.utf8String).map(parse(_).extract[telegram.Update])
    }

  implicit val skypeUpdateFromEntityUnmarshaller: FromEntityUnmarshaller[Seq[skype.Update]] =
    Unmarshaller.withMaterializer {
      implicit  ex => implicit mat => entity: HttpEntity =>
        entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
            .map(_.utf8String).map(parse(_).extract[Seq[skype.Update]])
    }
}
