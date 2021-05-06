package com.gu.wazup

import com.gu.wazup.aws.AWS
import software.amazon.awssdk.regions.Region
import zio.blocking.Blocking
import zio.console._
import zio.{ExitCode, URIO, ZIO}


object Main extends zio.App {
  private val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
  private val systemsManagerClient = AWS.systemsManagerClient("infosec", Region.of("eu-west-1"))
  private val cloudwatchClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))

  def run(args: List[String]): URIO[Console with Blocking, ExitCode] =
    wazup.forever.exitCode

  private val bucket = "BUCKET"
  private val prefix = "REPO/wazuh/etc/"

  val wazup: ZIO[Console with Blocking, Serializable, Unit] = {
    val result = for {
      wazuhFiles <- Logic.fetchFiles(s3Client, bucket, prefix)
      parameters <- Logic.fetchParameters(systemsManagerClient, "STACK/STAGE/")
      wazuhParameters = Logic.parseParameters(parameters, prefix)
      nodeType = Logic.getNodeType("ADDRESS", wazuhParameters)
      newConf <- Logic.createConf(wazuhFiles, wazuhParameters, nodeType)
      currentConf <- Logic.getCurrentConf("/var/ossec/etc/")
      shouldUpdate = Logic.hasChanges(newConf, currentConf)
      // TODO: add CloudWatch logging step here
      _ <- ZIO.when(shouldUpdate)(Logic.writeConf(newConf))
      // TODO: add CloudWatch logging step here
      returnCode <- ZIO.when(shouldUpdate)(Logic.restartWazuh())
    } yield returnCode

    // TODO: replace println with logging to CloudWatch
    result.fold(err => println(err), identity)
  }

}
