# Bot API for Scala

This is simple framework for easy development bots on Scala language based on [Akka](http://akka.io/).
For now it's support **Slack**, **Skype** and **Telegram** messengers.

* [Getting started](#getting-started)
* [Writing your first bot](#writing-your-first-bot)
    * [A simple echo bot](#a-simple-echo-bot)
    * [Configuration EchoBot](#configuration-echobot)
* [Extensions](#extensions)
    * [Scheduler Extension](#scheduler-extension)
    * [Socket Extension](#socket-extension)
    * [TextRazor Extension](#textrazor-extension)
* [Intents](#intents)
* [Data store](#data-store)

## Getting started.

## Writing your first bot.
 
### A simple echo bot
The AbstractBot trait handle all messages from users and send it to conversations by different criteria

First of all we need to create new file called `EchoBot.scala` and inherits from `AbstractBot`
`AbstractBot` apply `Data` class as generic argument. As we need no any additional data we use `EmptyData` class.  
```scala
class EchoBot extends from AbstractBot[EmptyData] {
  override protected var data: EmptyData = EmptyData()
    
  override protected def id: String = ???

  override def printHelp: String = ???

  override def printUnknown: String = ???

  override def startConversation: PartialFunction[Intent, Conversation] = ???
}
```
After that we need specify `id` which need for configuration and also add help and unknown text
```scala
  override protected def id: String = "EchoBot"

  override def printHelp: String = "This is example echo bot"

  override def printUnknown: String = "I don't understand you"
```
`startConversation` is a function which handle all incoming messages and select suitable conversation to handle it
In our case we need to handle all text messages.
```scala
  override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent: TextIntent => EchoConversation()(intent)
  }
```
`EchoConversation` is the class inherited from `Conversation` which contains states like [FSM](https://en.wikipedia.org/wiki/Finite-state_machine)
It applies `Intent` and return new `BotState` with optionally reply to user
Our bot will have only one `BotState` 
```scala
case class EchoConversation() extends Conversation {
    override def initialState: BotState = BotState {
      case intent: TextIntent => Reply(Exit).withIntent(ReplyMessageIntent(intent.sender, intent.text))
    }
} 
```
`Exit` is the system `BotState` which finish conversation and turn user chat to `Idle`

### Configuration EchoBot
In configuration file we need to define API keys and tokens for messengers to connect
```
EchoBot {
  TelegramSource {
    id = "Your Telegram API token here"
  }
  SlackSource {
    id = "Your Slack API token here"
  }
  SkypeSource {
    id = "Your Skype API token here"
    secret = "Your Skype secret here"
  }
}
```
Also we need to add to specify some [Akka Persistence](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html) parameters needs for internal use
```
akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"

akka.persistence.journal.leveldb.dir = "target/example/journal"
akka.persistence.snapshot-store.local.dir = "target/example/snapshots"
```

As the result we can run our EchoBot

```scala
object EchoBotMain {
  BotHelper.registerBot(classOf[EchoBot])
}
```


## Extensions
Extensions is a special mixin for `AbstractBot` which contains additional functionality
There is several extensions available for use:

###Scheduler Extension
Scheduler extension provides job scheduler based on [Quartz Job Scheduler](http://www.quartz-scheduler.org/)
Job can be set by cron like expression or duration expressions
Scheduler extension provides special type of intent — `SchedulerIntent` — which is send to the bot system as usual intent every time the scheduler triggered

Example:
```scala
val schedulerState = BotState {
    ...
    repeat(s"0 */2 * ? * *", SchedulerIntent("job1", "Hello")) //Will send SchedulerIntent("job1", "Hello") to the bot system every 2 minutes
    repeatEvery(3 minutes, SchedulerIntent("job2", "World")) //Will send SchedulerIntent("job2", "World") to the bot system every 3 minutes 
    ... 
    Reply(Exit)    
}
```

You can catch this `Intent` in `startConversation` method
```scala
override def startConversation: PartialFunction[Intent, Conversation] = {
    ...
    case intent@SchedulerIntent(_, "Hello") => SchedulerHandlerConversation()(intent)
    ...
}
```

To cancel job use `delete("jobId")` method
```scala
delete("job1")
```
###Socket Extension
Socket extension provides requests to the remote servers 
To use this extension you need to call `makeRequest` method with SocketIntent which contains params:
- `sender` — Chat which will be send the result
- `url` — Url to call
- `requestParams` — Parameters of the request

Example:
```scala
val params = RequestParams(canCache = true).put("streamId", "feed/https://bash.org.ru/rss/")
makeRequest[BashResponse](SocketIntent(intent.sender, "http://cloud.feedly.com/v3/streams/contents", params))
```

The answer will be sent to the system and can be processed in `BotState` handler 
```scala
val responseFromBashHandler: BotState = BotState {
  case ResultIntent(sender, result: BashResponse) =>
    val itemNumber = Random.nextInt(result.items.size)
    val article = result.items(itemNumber)
    Reply(Exit)
      .withIntent(ReplyMessageIntent(sender, s"<b>${article.title.trim}</b>\n${article.summary.content.trim.replaceAll("<br>", "\n")}"))
}
```
###TextRazor Extension
TextRazor extension is [Natural Language Processing](https://en.wikipedia.org/wiki/Natural_language_processing) Extension (NLP Extension) for improve parsing commands based on [TextRazor API](https://www.textrazor.com/)
This extension build dependency tree of user command and can be matched with pattern.

To configure TextRazor you need to specify `razorApiKey` value in code of your bot

Example:
```scala
override def startConversation: PartialFunction[Intent, Conversation] = {
    case intent@TextIntent(_, text) if text.nlpMatch("add|create" -> "team" -> "new") => 
        CreateTeamConversation(data)(intent)  // Applies expressions like "Create new team", "Please, add new team", "Can you create new team", etc.
    case intent@TextIntent(_, text) if text.nlpMatch("teams" -> "manage") => 
        ManageTeamsConversation(data)(intent) // Applies expressions like "Menage teams", "Can you manage teams", etc
    case intent@TextIntent(_, text) if text.nlpMatch("join" -> * -> "team") => 
        JoinTeamConversation(data)(intent)    // Applies expressions like "Join SomeTeam team" or something like this
    ...
}
```

To write your own extension you need to inherit from Extension trait.

##Intents
Intents is a part of data which is used for communicate between part of the bot system and users
There are several types of default intent, but you can create your own intent:

`TextIntent` — Simple wrap of user's text sent to bot.

`PositiveIntent` — User's positive answer of a question (e.g. "yes").

`NegativeIntent` — User's negative answer of a question (e.g. "no").

`NumberIntent` — User's number answer. Use when user need to select one of the few answers.

`AskChangeStateIntent` — Request to change state of other user (Can be declined if `canChange` parameter of the `BotState` is `true`).

`RequireChangeStateIntent` — Requires to change state of other user (Can't be declined).

`SystemPositiveIntent` — System positive answer of some request (e.g. ask for change state of other user).

`SystemNegativeIntent` — System negative answer of some request (e.g. ask for change state of other user).

`ReplyMessageIntent` — Send some text to user (usually use when bot state is changed).

`ScheduleIntent` — Message sent by SchedulerExtension when scheduler triggered.

`SocketIntent` — Intent for SocketExtension which specify parameters for HTTP request.

`SocketReply` — Message sent by SocketExtension which contains the result of HTTP request.
 
You can create your own Intent by inherited Intent trait and parse it by overriding `handleCustomIntent` method

##Data store
`AbstractBot` is [PersistentActor](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html) which can persist it's `data` and recover it after restart your bot
To add more complex recover behavior you need to override `recoverState` method

To store data you need to send object `SaveSnapshot` to the `self` Actor

Example
```scala
self ! SaveSnapshot
```