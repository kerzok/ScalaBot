name := "ScalaBotApi"

version := "0.2.9"
scalaVersion := "2.11.8"
organization := "com.github.kerzok"

lazy val akkaVersion = "2.4.12"
lazy val akkaHttpVersion = "3.0.0-RC1"
lazy val sprayVersion = "1.3.1"
lazy val json4sVersion = "3.4.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-actor"               % akkaVersion,
  "com.typesafe.akka"          %% "akka-slf4j"               % akkaVersion,
  "com.typesafe.akka"          %% "akka-testkit"             % akkaVersion % "test",
  "com.typesafe.akka"          %% "akka-http-core"           % akkaHttpVersion,
  "com.typesafe.akka"          %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"           % "akka-contrib_2.11"        % akkaVersion,
  "com.enragedginger"          %% "akka-quartz-scheduler"    % "1.5.0-akka-2.4.x",
  "io.spray"                   %% "spray-client"             % sprayVersion,
  "io.spray"                    % "spray-caching_2.11"       % sprayVersion,
  "io.spray"                    % "spray-util_2.11"          % sprayVersion,
  "org.json4s"                 %% "json4s-ext"               % json4sVersion,
  "org.json4s"                 %% "json4s-native"            % json4sVersion,
  "com.wandoulabs.akka"        %% "spray-websocket"          % "0.1.4",
  "org.reflections"             % "reflections"              % "0.9.10",
  "com.textrazor"               % "textrazor"                % "1.0.9",
  "org.scalatest"              %% "scalatest"                % "2.2.1" % "test")

publishMavenStyle := true

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false
pomIncludeRepository := { _ => false }
pomExtra := <url>https://github.com/kerzok/ScalaBot</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/kerzok/ScalaBot</connection>
    <developerConnection>scm:git:git@github.com:kerzok/ScalaBot</developerConnection>
    <url>github.com/kerzok/ScalaBot</url>
  </scm>
  <developers>
    <developer>
      <id>kerzok11</id>
      <name>Nikolay Smelik</name>
      <email>kerzok11@gmail.com</email>
    </developer>
  </developers>