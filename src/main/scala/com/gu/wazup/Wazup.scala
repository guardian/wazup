package com.gu.wazup

import com.gu.wazup.aws.S3
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathRequest, GetParametersByPathResponse}
import zio.blocking.Blocking
import zio.console.Console
import zio.process.Command
import zio.{ExitCode, IO, ZIO}

import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.util.control.NonFatal


object Wazup extends LazyLogging {

  def wazup(s3Client: S3Client, ssmClient: SsmAsyncClient, config: Configuration): ZIO[Console with Blocking, String, Unit] = {
    val result = for {
      configFiles <- fetchFiles(s3Client, config.bucket, config.bucketPath)
      parameters <- fetchParameters(ssmClient, config.parameterPrefix)
      wazuhParameters = Logic.parseParameters(parameters, config.parameterPrefix)
      // TODO: add validate parameters step and log to CloudWatch the result
      nodeAddress <- Logic.getNodeAddress
      nodeType = Logic.getNodeType(nodeAddress, wazuhParameters)
      wazuhFiles <- IO.fromEither(Logic.getWazuhFiles(configFiles, config.bucketPath))
      _ <- IO(logger.info(s"Fetching new configuration for $nodeType $nodeAddress"))
      newConf = Logic.createConf(wazuhFiles, wazuhParameters, nodeType, nodeAddress, Date.today)
      currentConf <- getCurrentConf(configFiles.map(_.filename), config.bucketPath, config.confPath)
      _ <- IO(logger.info(s"Reading current configuration for $nodeType $nodeAddress"))
      shouldUpdate = Logic.hasChanges(newConf, currentConf)
      _ <- ZIO.when(shouldUpdate)(writeConf(config.confPath, newConf))
      _ <- ZIO.when(shouldUpdate)(restartWazuh())
      // TODO: add CloudWatch logging step here
    } yield logger.info(s"Run complete! restart required was: $shouldUpdate")

    result.fold(err => logger.error(err.toString), identity)
  }

  private def getConfigFile(client: S3Client, bucket: String, key: String): ZIO[Blocking, String, ConfigFile] = {
    S3.getObjectContent(client, bucket, key).map(content => ConfigFile(key, content))
  }

  // TODO: change left to be a failure so we can return more information
  def fetchFiles(client: S3Client, bucket: String, prefix: String): ZIO[Blocking, String, List[ConfigFile]] = {
    for {
      response <- S3.listObjects(client, bucket, prefix)
      objectList = response.contents.asScala.toList
      configFiles <- ZIO.validatePar(objectList)(obj =>
        getConfigFile(client, bucket, obj.key)).mapError(_.mkString(" "))
    } yield configFiles
  }

  // TODO: check pagination =- do we want to use getParametersByPathPaginator?
  def fetchParameters(client: SsmAsyncClient, prefix: String): IO[String, GetParametersByPathResponse] = {
    val request = GetParametersByPathRequest.builder()
      .path(prefix)
      .withDecryption(true)
      .recursive(true)
      .build()
    ZIO.fromFuture(implicit ec => client.getParametersByPath(request).asScala).refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }

  private def getFileContent(file: String): ZIO[Blocking, String, ConfigFile] = {
    Logic.readFile(file).map(content => ConfigFile(file, content))
  }

  def getCurrentConf(files: List[String], bucketPath: String, confPath: String): ZIO[Blocking, String, WazuhFiles] = {
    val filePaths = files.map(filename => filename.replace(bucketPath, confPath))
    for {
      configFiles <- ZIO.collectAllSuccessesPar(filePaths.map(getFileContent))
      wazuhFiles <- IO.fromEither(Logic.getWazuhFiles(configFiles, confPath))
    } yield wazuhFiles
  }

  def writeConf(path: String, wazuhFiles: WazuhFiles): ZIO[Blocking, String, List[Unit]] = {
    val ossecConf = ConfigFile("ossec.conf", wazuhFiles.ossecConf)
    ZIO.validatePar(List(ossecConf) ++ wazuhFiles.otherConf)(file => {
      Logic.writeFile(s"$path/${file.filename}", file.content)
    }).mapError(_.mkString(" "))
  }

  def restartWazuh(): ZIO[Blocking, String, ExitCode] = {
    Command("systemctl", "restart", "wazuh-manager").run
      .flatMap(process => process.exitCode)
      .mapError(err => err.getMessage)
  }
}
