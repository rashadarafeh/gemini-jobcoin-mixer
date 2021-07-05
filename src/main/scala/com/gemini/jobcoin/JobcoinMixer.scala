package com.gemini.jobcoin

import com.gemini.jobcoin.controller._
import com.typesafe.config.{Config, ConfigFactory}
import play.api.http.HttpErrorHandler
import play.api._
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import jobcoin.Routes

class MixerLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    new JobCoinMixerComponents(context).application
  }
}

class JobCoinMixerComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {
  val logger: Logger = Logger(this.getClass())
  // Load Config
  val config = ConfigFactory.load()

  val client = new JobcoinClient(config)
  val backend = new JobcoinBackend(client, config)
  val jobcoinController = new JobCoinController(backend, controllerComponents)
  val errorHandler = play.api.http.DefaultHttpErrorHandler

  logger.logger.info("Started Polling Transactions")
  //val scheduledFuture = backend.startPollingBalances()
  val scheduledFuture = backend.startPollingTransactions()
  sys.addShutdownHook {
    client.stopClient()
    scheduledFuture.cancel(true)
    materializer.shutdown()
    actorSystem.terminate()
  }

  lazy val router = new jobcoin.Routes(errorHandler, jobcoinController)
}