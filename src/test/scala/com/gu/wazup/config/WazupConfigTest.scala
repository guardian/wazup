package com.gu.wazup.config

import com.gu.wazup.model.{Leader, Worker}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class WazupConfigTest extends AnyFreeSpec with Matchers {

  "getNodeType" - {

    "returns Leader when the addresses match" in {
      WazupConfig.getNodeType("Leader") shouldEqual Leader
    }
    "returns Worker when the addresses are different" in {
      WazupConfig.getNodeType("Worker") shouldEqual Worker
      WazupConfig.getNodeType("None") shouldEqual Worker
    }
  }

}