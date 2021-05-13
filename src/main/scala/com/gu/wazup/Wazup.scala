package com.gu.wazup

import com.gu.wazup.aws.S3
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathRequest, GetParametersByPathResponse}
import zio.blocking.Blocking
import zio.console.Console
import zio.{IO, ZIO}

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
      currentConf <- Logic.getCurrentConf(confPath, List.empty)
      shouldUpdate = Logic.hasChanges(newConf, currentConf)
      // TODO: add CloudWatch logging step here
      _ <- ZIO.when(shouldUpdate)(Logic.writeConf(confPath, newConf))
      // TODO: check ZIO will wait for the conf to be written before restarting
      // TODO: add CloudWatch logging step here
      returnCode <- ZIO.when(shouldUpdate)(Logic.restartWazuh())
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
}
