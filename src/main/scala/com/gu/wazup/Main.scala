package com.gu.wazup

import com.gu.wazup.aws.AWS
import com.gu.wazup.config.WazupConfig
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.duration.Duration
import zio.{ExitCode, Schedule, URIO}


object Main extends zio.App {
  private val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
  private val ssmClient = AWS.ssmClient("infosec", Region.of("eu-west-1"))
  private val cwClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))
  private val ec2Client = AWS.ec2Client("infosec", Region.of("eu-west-1"))

  private val config = WazupConfig.load()
  private val spaced = Schedule.spaced(Duration.fromMillis(60000))

  def run(args: List[String]): URIO[Console with Blocking with Clock, ExitCode] =
    Wazup.wazup(s3Client, ssmClient, cwClient, ec2Client, config)
      .repeat(spaced).exitCode
}
