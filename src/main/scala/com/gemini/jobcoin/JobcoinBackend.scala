package com.gemini.jobcoin

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.Scheduler

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class JobcoinBackend(client: JobcoinClient, scheduler: Scheduler)(implicit ec: ExecutionContext) {

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

  def createAccounts(addresses: Seq[String]) = {
    val depositAddress = UUID.randomUUID().toString
    //println(depositAddress)
    depositAddressDB.put(depositAddress,addresses)
    //println(depositAddressDB.getOrDefault(depositAddress, Seq.empty).head)
    Option(depositAddress)
  }

  def startPollingTransactions(): Unit = {
    val task = new Runnable {
      def run() {
        println("running timed task")
        val accounts = depositAddressDB.keys()
        while(accounts.hasMoreElements) {
          val depositAccount = accounts.nextElement()

          for {
            balance <- client.getBalanceForAddress(depositAccount)
            addresses = depositAddressDB.getOrDefault(depositAccount, Seq.empty)
          } yield {
            println("we have some things to move!")
            try {
              val numOfAddresses = addresses.size
              val amountGoingToAddress = if(numOfAddresses != 0) balance.balance.toFloat/numOfAddresses else 0

              addresses.map { address =>
                client.sendTransfer(depositAccount, address, amountGoingToAddress)
              }
            } catch {
              case e: Exception => println("Error! Balance was not a readable number for deposit account: " + depositAccount + "!!")
            }

          }
        }
      }
    }
    scheduler.schedule(
      initialDelay = Duration(5, TimeUnit.SECONDS),
      interval = Duration(10, TimeUnit.SECONDS),
      runnable = task)
  }
}
