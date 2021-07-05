package com.gemini.jobcoin

import java.util.{Date, UUID}
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

class JobcoinBackend(client: JobcoinClient, config: Config)(implicit ec: ExecutionContext) {
  val initialDelay = config.getInt("jobcoin.timer.initialDelay")
  val interval = config.getInt("jobcoin.timer.interval")

  // instead of building out a database, and ive considered using some
  // simple json file dbs for this. but for sake of time, im going to
  // create a var Map thats alive through the lifetime of the application.
  // Not good functional programming practice to use the var. but doing so to simplify
  // this project with regards to wanting a DB to track the deposit addresses i care about, and their destination addresses
  // ** assuming this is not an application that persists this data. it'll be gone at the end of the apps lifecycle **
  //
  // Also decided to use a Java ConcurrentHashMap, since the endpoint will be writing to this map, and the
  // timed transactions thread will reading from it separately. which means not using some pretty scala code in a few places :(
  import java.util.concurrent.ConcurrentHashMap
  val depositAddressDB = new ConcurrentHashMap[String, Seq[String]]()
  var lastTimeStamp: Date = new Date(0)

  def createAccounts(addresses: Seq[String]) = {
    val depositAddress = UUID.randomUUID().toString
    //println(depositAddress)
    depositAddressDB.put(depositAddress,addresses)
    //println(depositAddressDB.getOrDefault(depositAddress, Seq.empty).head)
    Option(depositAddress)
  }

  def startPollingBalances() = {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() {
        //println("running timed task")
        val accounts = depositAddressDB.keys()
        while(accounts.hasMoreElements) {
          val depositAccount = accounts.nextElement()

          for {
            balance <- client.getBalanceForAddress(depositAccount)
            addresses = depositAddressDB.getOrDefault(depositAccount, Seq.empty)
          } yield {
            try {
              val numOfAddresses = addresses.size
              if(numOfAddresses > 0 && balance.balance.toFloat > 0.0f) {
                //println("balance is: " + balance.balance)
                val amountGoingToAddress = balance.balance.toFloat/numOfAddresses
                addresses.map { address =>
                  client.sendTransfer(depositAccount, address, amountGoingToAddress)
                }
              }

            } catch {
              case e: Exception => println("Error! Balance could not be transferred from: " + depositAccount + "!! " + e.getMessage)
            }
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
            try {
              if(transaction.getTimestamp().after(lastTimeStamp) && depositAddressDB.containsKey(transaction.toAddress)) {
                lastTimeStamp = transaction.getTimestamp()
                val addresses = depositAddressDB.getOrDefault(transaction.toAddress, Seq.empty)
                val amountGoingToAddress = transaction.amount.toFloat/addresses.size.toFloat
                addresses.map { address =>
                  client.sendTransfer(transaction.toAddress, address, amountGoingToAddress)
                }
              }
            } catch {
              case e: Exception => println("Error! Balance could not be transferred from: " + transaction.toAddress + "!! " + e.getMessage)
            }
          )
        }
      }
    }
    ex.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.SECONDS)
  }
}
