package com.gemini.jobcoin

import akka.stream.Materializer
import com.gemini.jobcoin.JobcoinClient.{AccountBalanceResponse, TransactionResponse}
import com.typesafe.config.Config
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.ahc._

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class JobcoinClient(config: Config)(implicit materializer: Materializer) {
  private val wsClient = StandaloneAhcWSClient()
  private val apiTransactionUrl = config.getString("jobcoin.apiTransactionsUrl")
  private val apiAddressUrl = config.getString("jobcoin.apiAddressesUrl")

  def getBalanceForAddress(address: String): Future[AccountBalanceResponse] = async {
    val response = await {
      wsClient
        .url(apiAddressUrl + "/" + address)
        .get()
    }
    response
      .body[JsValue]
      .validate[AccountBalanceResponse]
      .get
  }

  def getTransactions(): Future[Seq[TransactionResponse]] = async {
    val response = await {
      wsClient
        .url(apiTransactionUrl)
        .get()
    }
    response
      .body[JsValue]
      .validate[Seq[TransactionResponse]]
      .get
  }

  def sendTransfer(fromAddress: String, toAddress: String, amount: Float): Unit = async {
    val body = Json.obj(
      "fromAddress" -> fromAddress,
      "toAddress" -> toAddress,
      "amount" -> amount.toString
    )
    await {
      wsClient
        .url(apiTransactionUrl).post(body)
    }
  }

}

object JobcoinClient {
  case class TransactionResponse(timestamp: String, fromAddress: Option[String], toAddress: String, amount: String)
  object TransactionResponse {
    implicit val jsonReads: Reads[TransactionResponse] = Json.reads[TransactionResponse]
  }
  case class AccountBalanceResponse(balance: String)
  object AccountBalanceResponse {
    implicit val jsonReads: Reads[AccountBalanceResponse] = Json.reads[AccountBalanceResponse]
  }

}
