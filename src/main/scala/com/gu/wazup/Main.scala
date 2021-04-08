package com.gu.wazup

import com.gu.wazup.aws.AWS
import software.amazon.awssdk.regions.Region


object Main {
  def main(args: Array[String]): Unit = {
    val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
    val systemsManagerClient = AWS.systemsManagerClient("infosec", Region.of("eu-west-1"))
    val cloudwatchClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))

  }
}
