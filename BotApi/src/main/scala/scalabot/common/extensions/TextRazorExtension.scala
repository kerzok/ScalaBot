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

package scalabot.common.extensions

import com.textrazor.TextRazor
import com.textrazor.annotations.{Response, Word}
import com.textrazor.classifier.ClassifierManager
import spray.caching.{Cache, LruCache}
import spray.util._

import scala.collection.JavaConversions._
import scala.util.Try
/**
  * Created by Nikolay.Smelik on 8/16/2016.
  */
trait TextRazorExtension extends BotExtension {
  import system.dispatcher
  protected val razorApiKey: String
  lazy protected val classifierManager = new ClassifierManager(razorApiKey)
  lazy protected val razorClient = new TextRazor(razorApiKey)
  private[this] val cache: Cache[Response] = LruCache()

  implicit class NLPStringExtension(s: String) {
      def nlpMatch(sequence: TokenSequence): Boolean = {
        val result = cache(s.toLowerCase()) {
          razorClient.addExtractor("dependency-trees")
          razorClient.analyze(s.toLowerCase()).getResponse
        }.await
        isMatch(result.getWords.toList, sequence)
      }

      def ->(word: String): TokenSequence = {
        TokenSequence(Seq(s, word))
      }
  }

  private def isMatch(wordsList:List[Word], sequence: TokenSequence): Boolean = {
    if (sequence.isEmpty) {
      true
    } else {
      val splittedWords = sequence.head.split('|')
      val matchedWords = if (splittedWords.contains("*")) wordsList else wordsList.filter(word => splittedWords.contains(word.getToken))
      val newWordsList = matchedWords.flatMap(word => word.getChildren)
      if (matchedWords.isEmpty) {
        false
      } else {
        isMatch(newWordsList, sequence.tail)
      }
    }
  }
}


case class TokenSequence(sequence: Seq[String]) {
  def ->(word: String): TokenSequence = {
    TokenSequence(sequence :+ word)
  }

  def head: String = sequence.head
  def tail: TokenSequence = TokenSequence(Try(sequence.tail).toOption.getOrElse(Nil))
  def isEmpty: Boolean = sequence == Nil
  def nonEmpty: Boolean = sequence.nonEmpty
}
