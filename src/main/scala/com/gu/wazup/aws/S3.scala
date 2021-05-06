package com.gu.wazup.aws

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, ListObjectsV2Request, ListObjectsV2Response}
import zio.ZIO
import zio.blocking._

import scala.io.Source
import scala.util.control.NonFatal


object S3 {

  def getObjectContent(client: S3Client, bucket: String, key: String): ZIO[Blocking, String, String] = {
    val request = GetObjectRequest.builder.bucket(bucket).key(key).build
    effectBlocking(client.getObjectAsBytes(request).asByteArray())
      .map(bytes => Source.fromBytes(bytes, "UTF-8").mkString)
      .refineOrDie {
        case NonFatal(t) => t.getMessage
      }
  }

  def listObjects(client: S3Client, bucket: String, prefix: String): ZIO[Blocking, String, ListObjectsV2Response] = {
    val request = ListObjectsV2Request.builder.bucket(bucket).prefix(prefix).build
    effectBlocking(client.listObjectsV2(request)).refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }
}
