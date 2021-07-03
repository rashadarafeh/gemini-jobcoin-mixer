version := "0.1"
scalaVersion := "2.12.10"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)
name := "gemini-jobcoin-scala"
mainClass in (Compile, run) := Some("com.gemini.jobcoin.JobcoinMixer")
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe" % "config" % "1.3.2",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M1",
  "com.typesafe.play" %% "play-ws-standalone-json" % "2.0.0-M1",
  "com.typesafe.play" %% "play" % "2.6.19",
  "com.typesafe.play" %% "play-netty-server" % "2.6.19",
  "org.scala-lang.modules" %% "scala-async" % "0.9.7"
)
routesImport ++= Seq(
  "com.gemini.jobcoin.controller.Binders._"
)

