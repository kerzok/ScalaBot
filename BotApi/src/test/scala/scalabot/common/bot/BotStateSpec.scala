package scalabot.common.bot

import akka.actor.{ActorSystem, Props}
import org.scalatest.{FlatSpec, Matchers}

import scalabot.common.Stubs
import scalabot.common.message.{ReplyMessageIntent, TextIntent}

/**
  * Created by kerzo on 10.11.2016.
  */
class BotStateSpec extends FlatSpec with Matchers with Stubs {

  it should "return other state if incoming message is verified criteria" in {
    val state = botStateStub()

    val reply = state(defaultIntent)

    reply.state shouldEqual OtherState
  }

  it should "exit from conversation if Exit conversation message is coming" in {
    val state = botStateStub()

    val reply = state(TextIntent(senderStub, "exit conversation"))

    reply.state shouldEqual Exit
  }

  it should "respond exit conversation notification if exit conversation message is coming" in {
    val state = botStateStub()

    val reply = state(TextIntent(senderStub, "exit conversation"))

    reply.intents.length shouldEqual 1
    reply.intents.foreach(_ shouldEqual ReplyMessageIntent(senderStub, "Conversation was interrupted"))
  }

  it should "react for exit conversation message if first letter is upper case" in {
    val state =  botStateStub()

    val reply = state(TextIntent(senderStub, "Exit conversation"))

    reply.state shouldEqual Exit
  }

  it should "do not react for exit conversation message if exit is disabled" in {
    val state = botStateStub(hasExit = false)

    val reply = state(TextIntent(senderStub, "exit conversation"))

    reply.state shouldEqual state
  }

  it should "do nothing if unknown intent is coming" in {
    val state = botStateStub()

    val reply = state(TextIntent(senderStub, "unknown intent"))

    reply.state shouldEqual state
  }

  "MoveToConversation" should "throw IllegalStateException in case of incoming message" in {
    val system = ActorSystem("test")
    implicit val actor = system.actorOf(Props.empty)
    val state = MoveToConversation(conversation())

    intercept[IllegalStateException] {
      state(defaultIntent)
    }
    system.terminate()
  }

  "StateWithDefaultReply" should "return default message" in {
    val helpMessage = "this is a default message"
    val helpState = StateWithDefaultReply(helpMessage)

    val reply = helpState(TextIntent(senderStub, "some message"))

    reply.state shouldEqual Exit
    reply.intents.length shouldEqual 1
    reply.intents.foreach(_ shouldEqual ReplyMessageIntent(senderStub, helpMessage))
  }
}
