package com.gemini.jobcoin

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.gemini.jobcoin.controller._
import com.typesafe.config.{Config, ConfigFactory}
import play.api.http.HttpErrorHandler
import play.api.routing.{Router => PlayRouter}
import play.api.{BuiltInComponents, Configuration, Mode}
import play.core.server.{NettyServerComponents, ServerConfig}

import scala.concurrent.ExecutionContext.Implicits.global

object JobcoinMixer extends App {
  // Create an actor system
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // Load Config
  val config = ConfigFactory.load()

  val client = new JobcoinClient(config)
  val backend = new JobcoinBackend(client, config)
  val jobcoinController = new JobCoinController(backend)
  val errorHandler = play.api.http.DefaultHttpErrorHandler

  val routes = new jobcoin.Routes(errorHandler, jobcoinController)
  val router: PlayRouter = new _root_.router.Routes(errorHandler, routes, "/")
  val server = new Server(config, errorHandler, router, 5432)

  //val scheduledFuture = backend.startPollingBalances()
  val scheduledFuture = backend.startPollingTransactions()

  sys.addShutdownHook {
    client.stopClient()
    scheduledFuture.cancel(true)
    server.stop()
    materializer.shutdown()
    actorSystem.terminate()
  }
  server.start()
}

class Server(
  config:       Config,
  errorHandler: HttpErrorHandler,
  routes:       PlayRouter,
  port:         Int
) extends NettyServerComponents with BuiltInComponents {
  def start(): Unit = server
  def stop(): Unit = server.stop()

  override lazy val configuration = Configuration(
    config.getConfig("play").atKey("play")
      .withFallback(ConfigFactory.load())
  )

  override lazy val serverConfig = ServerConfig(
    rootDir = new File("."),
    port = Some(port),
    sslPort = None,
    address = "0.0.0.0",
    mode = Mode.Dev,
    properties = System.getProperties,
    configuration = configuration
  )

  override lazy val router = routes
  override lazy val httpFilters = Seq.empty
  override lazy val httpErrorHandler = errorHandler
}