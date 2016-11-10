name := "ScalaBotApi"

lazy val commonSettings = Seq(
  version := "0.2",
  scalaVersion := "2.11.8"
)


lazy val BotApi = project in file("BotApi")

lazy val Examples = project in file("Examples") settings (commonSettings: _*) dependsOn BotApi
