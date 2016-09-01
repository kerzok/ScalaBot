import Dependencies._
name := "ScalaBotApi"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8"
)


lazy val BotApi = project in file("BotApi") settings(commonSettings: _*) settings(libraryDependencies ++= coreApiDeps)

lazy val Examples = project in file("Examples") settings (commonSettings: _*) dependsOn BotApi

resolvers += "Spray" at "http://repo.spray.io"
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))