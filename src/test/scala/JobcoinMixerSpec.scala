package com.gemini.jobcoin

import org.scalatest._
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.nio.charset.StandardCharsets

class MixerTests extends FlatSpec with Matchers {

  "JobCoinBackend" should "distribute balance evenly" in {
    val addresses = Seq("address1", "address2", "address3")
    val balance = BigDecimal("9.0")
    val expectedResults = Seq( BigDecimal("3.0"),  BigDecimal("3.0"),  BigDecimal("3.0"))
    val answer = JobcoinBackend.getBalancesForAdresses(addresses, balance)

    assert(answer == expectedResults)
  }

  "JobCoinBackend" should "distribute balance randomly" in {
    val addresses = Seq("address1", "address2", "address3")
    val balance = BigDecimal("9.0")
    JobcoinBackend.getBalancesForAddressesRandomized(addresses, balance)
  }
}
