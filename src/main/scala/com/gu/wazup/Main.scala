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
  private val bucketPath = "REPO/wazuh/etc"
  private val parameterPrefix = "/wazuh/CODE/"

  val wazup: ZIO[Console with Blocking, Serializable, Unit] = {
    val result = for {
      wazuhFiles <- Logic.fetchFiles(s3Client, bucket, bucketPath)
      parameters <- Logic.fetchParameters(systemsManagerClient, parameterPrefix)
      wazuhParameters = Logic.parseParameters(parameters, parameterPrefix)
      // TODO: add validate parameters step and log to CloudWatch the result
      nodeType = Logic.getNodeType("ADDRESS", wazuhParameters)
      newConf = Logic.createConf(wazuhFiles, wazuhParameters, nodeType)
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
