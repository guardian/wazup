package com.gu.wazup

import com.gu.wazup.aws.AWS
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.console.Console
import zio.{ExitCode, URIO}


object Main extends zio.App {
  private val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
  private val ssmClient = AWS.ssmClient("infosec", Region.of("eu-west-1"))
  private val cwClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))

  private val bucket = "BUCKET"
  private val bucketPath = "REPO/wazuh/etc"
  private val parameterPrefix = "/wazuh/CODE/"
  private val confPath = "/var/ossec/etc/"

  def run(args: List[String]): URIO[Console with Blocking, ExitCode] =
    Wazup.wazup(s3Client, ssmClient, bucket, bucketPath, confPath, parameterPrefix).forever.exitCode
}
