package com.gu.wazup

import com.gu.wazup.aws.AWS
import com.gu.wazup.config.WazupConfig
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.console.Console
import zio.{ExitCode, URIO}


object Main extends zio.App {
  private val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
  private val ssmClient = AWS.ssmClient("infosec", Region.of("eu-west-1"))
  private val cwClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))

  private val config = WazupConfig.load()

  def run(args: List[String]): URIO[Console with Blocking, ExitCode] =
    Wazup.wazup(s3Client, ssmClient, config)
      .forever.exitCode
}
