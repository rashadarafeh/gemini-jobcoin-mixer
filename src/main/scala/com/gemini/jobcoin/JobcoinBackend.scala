package com.gemini.jobcoin

import java.util.{Date, UUID}
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import com.typesafe.config.Config
import scala.concurrent.ExecutionContext
import play.api.Logger

class JobcoinBackend(client: JobcoinClient, config: Config)(implicit ec: ExecutionContext) {
  import JobcoinBackend._
  val logger: Logger = Logger(this.getClass())
  val initialDelay = config.getInt("jobcoin.timer.initialDelay")
  val interval = config.getInt("jobcoin.timer.interval")

  /** Instead of building out a database, and ive considered using some
  * simple json file dbs for this. but for sake of time, im going to
  * create a mutable Map thats alive throughout the lifetime of the application.
  * Not good functional programming practice to use a mutable, but doing so to simplify
  * this project with regards to wanting a DB to track the deposit addresses i care about, and their destination addresses
  * ## assuming this is not an application that persists this data. it'll be gone at the end of the apps lifecycle ##
  *
  * Also decided to use a Java ConcurrentHashMap, since the endpoint will be writing to this map, and the
  * timed transactions thread will reading from it separately. which means not using some pretty scala code in a few places :( */
  import java.util.concurrent.ConcurrentHashMap
  val depositAddressDB = new ConcurrentHashMap[String, Seq[String]]()
  var lastTimeStamp: Date = new Date(0)

  def createAccounts(addresses: Seq[String]) = {
    val depositAddress = UUID.randomUUID().toString
    depositAddressDB.put(depositAddress,addresses)
    Option(depositAddress)
  }

  def startPollingBalances() = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() {
        val accounts = depositAddressDB.keys()
        while(accounts.hasMoreElements) {
          val depositAccount = accounts.nextElement()
          for {
            balance <- client.getBalanceForAddress(depositAccount)
            addresses = depositAddressDB.getOrDefault(depositAccount, Seq.empty)
            amounts = getBalancesForAddressesRandomized(addresses, BigDecimal(balance.balance))
          } yield {
            sendAmountsToAddresses(
              addresses,
              amounts,
              balance.balance,
              depositAccount
            )
          }
        }
      }
    }
    ex.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.SECONDS)
  }

  def startPollingTransactions() = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() {
        for {
          transactions <- client.getTransactions()
        } yield {
          transactions.map ( transaction =>
            if(transaction.getTimestamp().after(lastTimeStamp) && depositAddressDB.containsKey(transaction.toAddress)) {
              lastTimeStamp = transaction.getTimestamp()
              val addresses = depositAddressDB.getOrDefault(transaction.toAddress, Seq.empty)
              val amounts = getBalancesForAddressesRandomized(addresses, BigDecimal(transaction.amount))
              sendAmountsToAddresses(
                addresses,
                amounts,
                transaction.amount,
                transaction.toAddress
              )
            }
          )
        }
      }
    }
    ex.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.SECONDS)
  }

  private def sendAmountsToAddresses(addresses: Seq[String], amounts: Seq[BigDecimal], totalAmount: String, despositAccount: String) = {
    try{
      if(addresses.size > 0 && totalAmount.toFloat > 0.0f) {
        (addresses zip amounts).map { case (address, amount) => {
          client.sendTransfer(despositAccount, address, amount)
        }}
      }
    } catch {
      case e: Exception => logger.logger.error("Error! Balance could not be transferred from: " + despositAccount + "!! " + e.getMessage)
    }
  }
}

object JobcoinBackend {
  /** These are some methods we created in the interview with Jimmy.
  * I decided to maintain the ability to make the business logic unit
  * testable here, and so i am keeping the methods, and using them in
  * the polling tasks. */
  def getBalancesForAdresses(accounts: Seq[String], balance: BigDecimal): Seq[BigDecimal] = {
    val numOfAddresses = accounts.size
    val amountGoingToAddress = balance/numOfAddresses
    val results = accounts.map { _ =>
      amountGoingToAddress
    }
    results
  }

  def getBalancesForAddressesRandomized(accounts: Seq[String], balance: BigDecimal): Seq[BigDecimal] = {
    var bl = balance
    val results = accounts.map { address =>
      if(address == accounts.last) {
        bl
      } else {
        val amountToAddress = bl * BigDecimal(scala.util.Random.nextFloat().toString)
        bl = bl - amountToAddress
        amountToAddress
      }
    }
    /** Uncomment for running in unit tests to see output
    * cant exactly have an expectation for randomized output */
    //println("amounts: " + results)
    results
  }
}
