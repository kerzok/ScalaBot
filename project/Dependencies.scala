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

/**
  * Created by Nikolay.Smelik on 8/16/2016.
  */
import sbt._

object Dependencies {
  lazy val akkaVersion = "2.4.12"
  lazy val sprayVersion = "1.3.1"
  lazy val json4sVersion = "3.4.0"
  val coreApiDeps = Seq("com.typesafe.akka"          %% "akka-actor"               % akkaVersion,
  "com.typesafe.akka"          %% "akka-slf4j"               % akkaVersion,
  "com.typesafe.akka"          %% "akka-testkit"             % akkaVersion % "test",
  "com.typesafe.akka"          %% "akka-http-core"           % "3.0.0-RC1",
  "com.enragedginger"          %% "akka-quartz-scheduler"    % "1.5.0-akka-2.4.x",
  "io.spray"                   %% "spray-client"             % sprayVersion,
  "io.spray"                   %% "spray-can"                % sprayVersion,
  "io.spray"                    % "spray-caching_2.11"            % sprayVersion,
  "io.spray"                    % "spray-util_2.11"               % sprayVersion,
  "org.json4s"                 %% "json4s-ext"               % json4sVersion,
  "org.json4s"                 %% "json4s-native"            % json4sVersion,
  "org.specs2"                 %% "specs2-core"              % "3.8.4",
  "com.wandoulabs.akka"        %% "spray-websocket"          % "0.1.4",
  "org.reflections"             % "reflections"              % "0.9.10",
  "org.iq80.leveldb"            % "leveldb"                  % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"           % "1.8",
  "com.textrazor"               % "textrazor"                % "1.0.9",
  "org.scalatest"              %% "scalatest"                % "2.2.1" % "test")
}

