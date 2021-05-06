package com.gu.wazup.aws

import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain, InstanceProfileCredentialsProvider, ProfileCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient

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
}
