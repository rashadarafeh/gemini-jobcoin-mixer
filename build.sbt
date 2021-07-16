version := "0.1"
scalaVersion := "2.12.10"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)
name := "gemini-jobcoin-scala"
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe" % "config" % "1.3.2",
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",
  "com.typesafe.play" %% "play-ahc-ws" % "2.8.8"
)
routesImport ++= Seq(
  "com.gemini.jobcoin.controller.Binders._"
)

