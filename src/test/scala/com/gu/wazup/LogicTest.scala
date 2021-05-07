package com.gu.wazup

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class LogicTest extends AnyFreeSpec with Matchers {

  "getNodeType" - {
    val parameters = WazuhParameters(None, Some("10.0.0.1"), None, None, None, None)

    "should correctly identify the Leader" in {
      Logic.getNodeType("10.0.0.1", parameters) shouldEqual Leader
    }
    "should otherwise determine the NodeType to be Worker" in {
      Logic.getNodeType("10.0.0.2", parameters) shouldEqual Worker
    }
  }

  "createConf" - {
    val parameters: WazuhParameters = WazuhParameters(
      Some("12345FAKEKEY"),
      Some("10.0.0.1"),
      Some("agentSecretArn"),
      Some("cloudtrailRoleArn"),
      Some("guarddutyRoleArn"),
      Some("umbrellaRoleArn")
    )

    "generates the correct configuration for a Leader" in {
      val testConf = Source.fromResource("ossec/cluster-input.conf").getLines.mkString("\n")
      val expectedConf = Source.fromResource("ossec/cluster-leader-output.conf").getLines.mkString("\n")
      Logic.createConf(WazuhFiles(testConf), parameters, Leader) shouldEqual WazuhFiles(expectedConf)
    }

    "generates the correct configuration for a Worker" in {
      val testConf = Source.fromResource("ossec/cluster-input.conf").getLines.mkString("\n")
      val expectedConf = Source.fromResource("ossec/cluster-worker-output.conf").getLines.mkString("\n")
      Logic.createConf(WazuhFiles(testConf), parameters, Worker) shouldEqual WazuhFiles(expectedConf)
    }
  }
}