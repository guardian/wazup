package com.gu.wazup.aws

import com.gu.wazup.{ConfigFile, WazuhFiles}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, InstanceProfileCredentialsProvider, ProfileCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathRequest, GetParametersByPathResponse}
import zio.blocking.Blocking
import zio.{IO, ZIO}

import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.util.control.NonFatal


object AWS {

  private def credentialsProvider(profile: String): AwsCredentialsProviderChain = {
    AwsCredentialsProviderChain.builder()
      .credentialsProviders(
        InstanceProfileCredentialsProvider.builder().build(),
        ProfileCredentialsProvider.builder().profileName(profile).build())
      .build()
  }

  def s3Client(profile: String, region: Region): S3Client = {
    S3Client.builder()
      .credentialsProvider(credentialsProvider(profile))
      .region(region)
      .build()
  }

  def systemsManagerClient(profile: String, region: Region): SsmAsyncClient = {
    SsmAsyncClient.builder()
      .credentialsProvider(credentialsProvider(profile))
      .region(region)
      .build()
  }

  def cloudwatchClient(profile: String, region: Region): CloudWatchAsyncClient = {
    CloudWatchAsyncClient.builder()
      .credentialsProvider(credentialsProvider(profile))
      .region(region)
      .build()
  }

  // TODO: change left to be a failure so we can return more information
  def fetchFiles(client: S3Client, bucket: String, prefix: String): ZIO[Blocking, String, WazuhFiles] = {
    for {
      ossecConf <- S3.getObjectContent(client, bucket, s"$prefix/ossec.conf")
      decoders <- S3.listObjects(client, bucket, s"$prefix/decoders/")
      lists <- S3.listObjects(client, bucket, s"$prefix/lists/")
      rules <- S3.listObjects(client, bucket, s"$prefix/rules/")
    } yield WazuhFiles(ossecConf)
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
