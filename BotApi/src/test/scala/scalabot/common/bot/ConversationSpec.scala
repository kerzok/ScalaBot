package scalabot.common.bot

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{FlatSpec, FlatSpecLike, Matchers}

import scalabot.common.Stubs
import scalabot.common.chat.Chat
import scalabot.common.message._

/**
  * Created by kerzo on 15.11.2016.
  */
class ConversationSpec extends TestKit(ActorSystem("test")) with FlatSpecLike with ImplicitSender with Matchers with Stubs {
  it should "change state if current state finished" in {
    val conversation = conversationStub()

    conversation(defaultIntent)

    conversation.currentState shouldEqual OtherState
  }

  it should "send intent to bot if state finished with intents" in {
    val actor = TestProbe()
    val conversation = conversationStub()(actor.ref)

    conversation(defaultIntent)

    actor.expectMsg(ReplyMessageIntent(senderStub, "default response"))
  }

  it should "finish if current bot state reply with exit" in {
    val conversation = new Conversation() {
      override def initialState: BotState = BotState {
        case intent => Reply(Exit)
      }
    }

    val actualConversation = conversation(defaultIntent)

    actualConversation shouldEqual Idle()
  }

  it should "send correct message instantly if current state reply with StateWithDefaultReply" in {
    val actor = TestProbe()
    val conversation = new Conversation()(actor.ref) {
      override def initialState: BotState = BotState {
        case intent => Reply(StateWithDefaultReply("some message"))
      }
    }

    conversation(defaultIntent)

    actor.expectMsg(ReplyMessageIntent(senderStub, "some message"))
  }

  it should "return new conversation if current state reply with MoveToConversationIntent" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState {
        case intent => Reply(MoveToConversation(expectedConversation))
      }
    }

    val actualConversation = conversation(defaultIntent)

    actualConversation shouldEqual expectedConversation
  }

  it should "return new conversation and take new Intent if current state reply with MoveToConversationIntent with Intent" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState {
        case intent => Reply(MoveToConversation(expectedConversation, defaultIntent))
      }
    }

    val actualConversation = conversation(PositiveIntent(senderStub, ""))

    actualConversation shouldEqual expectedConversation
    actualConversation.currentState shouldEqual OtherState
  }

  it should "return new conversation for AskChangeStateIntent if current state can be change" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState({
        case _ => Reply(Exit)
      }, isCanChange = true)
    }

    val (intent, newConversation) = conversation.changeState(AskChangeStateIntent(senderStub, senderStub, expectedConversation))

    intent shouldEqual SystemPositiveIntent(senderStub)
    newConversation shouldEqual expectedConversation
  }

  it should "return same conversation for AskChangeState if current state cannot be change" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState({
        case _ => Reply(Exit)
      }, isCanChange = false)
    }

    val (intent, newConversation) = conversation.changeState(AskChangeStateIntent(senderStub, senderStub, expectedConversation))

    intent shouldEqual SystemNegativeIntent(senderStub)
    newConversation shouldEqual conversation
  }

  it should "return new conversation for RequireChangeStateIntent if current state can be change" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState({
        case _ => Reply(Exit)
      }, isCanChange = true)
    }

    val (intent, newConversation) = conversation.changeState(RequireChangeStateIntent(senderStub, senderStub, expectedConversation))

    intent shouldEqual SystemPositiveIntent(senderStub)
    newConversation shouldEqual expectedConversation
  }

  it should "return new conversation for RequireChangeStateIntent if current state cannot be change" in {
    val expectedConversation = conversationStub()
    val conversation = new Conversation() {
      override def initialState: BotState = BotState({
        case _ => Reply(Exit)
      }, isCanChange = false)
    }

    val (intent, newConversation) = conversation.changeState(RequireChangeStateIntent(senderStub, senderStub, expectedConversation))

    intent shouldEqual SystemPositiveIntent(senderStub)
    newConversation shouldEqual expectedConversation
  }

  "ConversationWithDefaultReply" should "reply with default message" in {
    val actor = TestProbe()
    val conversation = ConversationWithDefaultReply("some message")(actor.ref)

    conversation(defaultIntent)

    actor.expectMsg(ReplyMessageIntent(senderStub, "some message"))
  }

  "ConversationWithDefaultReply" should "exit after reply message" in {
    val conversation = ConversationWithDefaultReply("some message")

    val newConversation = conversation(defaultIntent)

    newConversation shouldEqual Idle()
  }

  it should "append bundle" in {
    val conversation = conversationStub()
    val bundle = Bundle()
    bundle.put("someKey", "someValue")

    conversation.bundle.containsKey("someKey") shouldEqual false

    conversation.appendBundle(bundle)

    conversation.bundle.containsKey("someKey") shouldEqual true
  }

  "Bundle" should "correctly get boolean" in {
    val bundle = Bundle()

    bundle.put("someKey", true)

    bundle.getBoolean("someKey") shouldEqual true
  }

  "Bundle" should "correctly get string" in {
    val bundle = Bundle()

    bundle.put("someKey", "someValue")

    bundle.getString("someKey") shouldEqual "someValue"
  }

  "Bundle" should "correctly get int" in {
    val bundle = Bundle()

    bundle.put("someKey", 1)

    bundle.getInt("someKey") shouldEqual 1
  }

  "Bundle" should "correctly get object" in {
    val bundle = Bundle()
    val expectedObject = senderStub

    bundle.put("someKey", senderStub)

    bundle.getObject[Chat]("someKey") shouldEqual expectedObject
  }

  "Bundle" should "return default value if key does not exist" in {
    val bundle = Bundle()
    val expectedObject = senderStub

    bundle.getObjectOrElse("someKey", expectedObject) shouldEqual expectedObject
  }

  "Bundle" should "return None if key does not exist" in {
    val bundle = Bundle()

    bundle.getObjectOpt("someKey") shouldEqual None
  }

}
