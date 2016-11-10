package scalabot.common

import akka.actor.ActorRef

import scalabot.common.bot.{BotState, Conversation, Exit, Reply}
import scalabot.common.chat.{User, UserChat}
import scalabot.common.message.{Intent, ReplyMessageIntent, TextIntent}

/**
  * Created by kerzo on 10.11.2016.
  */
trait Stubs {
  val senderStub = UserChat("someId", "someSource", User("someName", None))
  val defaultIntent = TextIntent(senderStub, "needToReact")
  case object OtherState extends BotState {
    override val canChange: Boolean = false

    override def handleIntent: (Intent) => Reply = {intent => Reply(Exit)}
  }

  def botStateStub(intentToReact: Intent = defaultIntent, canChange: Boolean = true, hasExit: Boolean = true) = BotState({
    case intent: Intent if intent == intentToReact => Reply(OtherState).withIntent(ReplyMessageIntent(senderStub, "default response"))
  }, canChange, hasExit)

  def conversation(botState: BotState = botStateStub())(implicit actorRef: ActorRef) = new Conversation() {
    override def initialState: BotState = botState
  }
}
