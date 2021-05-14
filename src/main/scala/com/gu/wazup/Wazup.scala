package com.gu.wazup

import com.gu.wazup.aws.S3
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathRequest, GetParametersByPathResponse}
import zio.blocking.Blocking
import zio.console.{Console, putStrLn}
import zio.process.{Command, CommandError}
import zio.{ExitCode, IO, ZIO}

import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.util.control.NonFatal


object Wazup {
  def wazup(s3Client: S3Client, systemsManagerClient: SsmAsyncClient, bucket: String, bucketPath: String, confPath: String, parameterPrefix: String): ZIO[Console with Blocking, Serializable, Unit] = {
    val result = for {
      configFiles <- fetchFiles(s3Client, bucket, bucketPath)
      parameters <- fetchParameters(systemsManagerClient, parameterPrefix)
      wazuhParameters = Logic.parseParameters(parameters, parameterPrefix)
      // TODO: add validate parameters step and log to CloudWatch the result
      nodeAddress <- Logic.getNodeAddress
      nodeType = Logic.getNodeType(nodeAddress, wazuhParameters)
      wazuhFiles <- IO.fromEither(Logic.getWazuhFiles(configFiles, bucketPath))
      newConf = Logic.createConf(wazuhFiles, wazuhParameters, nodeType, nodeAddress)
      currentConf <- getCurrentConf(confPath)
      shouldUpdate = Logic.hasChanges(newConf, currentConf)
      // TODO: add CloudWatch logging step here
      _ <- ZIO.when(shouldUpdate)(writeConf(confPath, newConf))
      // TODO: check ZIO will wait for the conf to be written before restarting
      // TODO: add CloudWatch logging step here
      returnCode <- ZIO.when(shouldUpdate)(restartWazuh())
    } yield returnCode

    // TODO: replace println with logging to CloudWatch
    result.fold(err => println(err), identity)
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

  def getCurrentConf(path: String): ZIO[Blocking, String, WazuhFiles] = {
    for {
      files <- Logic.listFiles(path)
      configFiles <- ZIO.validatePar(files)(getFileContent).mapError(_.mkString(" "))
      wazuhFiles <- IO.fromEither(Logic.getWazuhFiles(configFiles, path))
    } yield wazuhFiles
  }

  def writeConf(path: String, wazuhFiles: WazuhFiles): ZIO[Blocking, String, List[Unit]] = {
    val ossecConf = ConfigFile("ossec.conf", wazuhFiles.ossecConf)
    ZIO.validatePar(List(ossecConf) ++ wazuhFiles.otherConf)(file => {
      Logic.writeFile(s"$path/${file.filename}", file.content)
    }).mapError(_.mkString(" "))
  }

  // should only be called if the conf has changed and should ideally return the status code
  // TODO: check if sudo is needed to restart and work out how to avoid running as root
  def restartWazuh(): ZIO[Blocking, CommandError, ExitCode] = {
    Command("systemctl", "restart", "wazuh-manager").run
      .flatMap(process => process.exitCode)
  }
}
