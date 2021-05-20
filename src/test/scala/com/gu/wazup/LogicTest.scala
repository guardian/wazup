package com.gu.wazup

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathResponse, Parameter}

import scala.io.Source

class LogicTest extends AnyFreeSpec with Matchers {

  "parseParameters" - {

    "creates the WazuhParameters" in {
      val response = {
        GetParametersByPathResponse.builder().parameters(
          Parameter.builder().name("/wazuh/TEST/wazuhClusterKey").value("FAKEKEY").build(),
          Parameter.builder().name("/wazuh/TEST/coordinatorIP").value("10.0.0.1").build(),
          Parameter.builder().name("/wazuh/TEST/cloudtrailRoleArn").value("arn:cloudtrail").build(),
          Parameter.builder().name("/wazuh/TEST/guarddutyRoleArn").value("arn:guardduty").build(),
          Parameter.builder().name("/wazuh/TEST/umbrellaRoleArn").value("arn:umbrella").build(),
        ).build()
      }
      val expected = WazuhParameters(
        Some("FAKEKEY"), Some("10.0.0.1"), Some("arn:cloudtrail"), Some("arn:guardduty"), Some("arn:umbrella"))

      Logic.parseParameters(response, "/wazuh/TEST/") shouldEqual expected
    }
  }

  "getNodeType" - {
    val parameters = WazuhParameters(None, Some("10.0.0.1"), None, None, None)

    "returns Leader when the addresses match" in {
      Logic.getNodeType("10.0.0.1", parameters) shouldEqual Leader
    }
    "returns Worker when the addresses are different" in {
      Logic.getNodeType("10.0.0.2", parameters) shouldEqual Worker
    }
  }

  "getWazuhFiles" - {
    "removes the common prefix from the filenames" in {
      val files = List(
        ConfigFile("/var/ossec/etc/ossec.conf", "ossec"),
        ConfigFile("/var/ossec/etc/decoders/local_decoder.xml", "decoder"),
        ConfigFile("/var/ossec/etc/rules/local_rules.xml", "decoder"),
      )
      val expected = Right(WazuhFiles("ossec", List(
        ConfigFile("decoders/local_decoder.xml", "decoder"),
        ConfigFile("rules/local_rules.xml", "decoder"))
      ))
      Logic.getWazuhFiles(files, "/var/ossec/etc/") shouldEqual expected
    }
  }

  "createConf" - {
    val parameters: WazuhParameters = WazuhParameters(
      Some("12345FAKEKEY"),
      Some("10.0.0.1"),
      Some("cloudtrailRoleArn"),
      Some("guarddutyRoleArn"),
      Some("umbrellaRoleArn")
    )

    "generates the correct configuration for a Leader" in {
      val testConf = Source.fromResource("ossec/cluster-input.conf").getLines().mkString("\n")
      val expectedConf = Source.fromResource("ossec/cluster-leader-output.conf").getLines().mkString("\n")
      Logic.createConf(WazuhFiles(testConf), parameters, Leader, "10.0.0.1", Date.today) shouldEqual WazuhFiles(expectedConf)
    }

    "generates the correct configuration for a Worker" in {
      val testConf = Source.fromResource("ossec/cluster-input.conf").getLines().mkString("\n")
      val expectedConf = Source.fromResource("ossec/cluster-worker-output.conf").getLines().mkString("\n")
      Logic.createConf(WazuhFiles(testConf), parameters, Worker, "10.0.0.2", Date.today) shouldEqual WazuhFiles(expectedConf)
    }
  }
}