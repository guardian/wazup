package com.gu.wazup

import com.gu.wazup.aws.S3
import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.{GetParametersByPathRequest, GetParametersByPathResponse}
import zio.{IO, ZIO}
import zio.blocking.Blocking

import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.util.control.NonFatal


object Logic  extends LazyLogging {
  // TODO: change left to be a failure so we can return more information
  def fetchFiles(client: S3Client, bucket: String, prefix: String): ZIO[Blocking, String, WazuhFiles] = {
    for {
      ossecConf <- S3.getObjectContent(client, bucket, s"$prefix/ossec.conf")
      decoders <- S3.listObjects(client, bucket, s"$prefix/decoders/")
      lists <- S3.listObjects(client, bucket, s"$prefix/lists/")
      rules <- S3.listObjects(client, bucket, s"$prefix/rules/")
    } yield WazuhFiles(ossecConf, List.empty, List.empty, List.empty)
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

  def parseParameters(response: GetParametersByPathResponse, prefix: String): WazuhParameters = {
    val parameters = response.parameters.asScala.toList.map { param =>
      param.name.stripPrefix(prefix) -> param.value
    }.toMap

    WazuhParameters(
      // TODO: should missing parameters raise an exception?
      // what do we want to happen if the parameter is missing?
      // validate params and notify (CloudWatch log)
      parameters.get("wazuhClusterKey"),
      parameters.get("coordinatorIP"),
      parameters.get("agentSecretArn"),
      parameters.get("cloudtrailRoleArn"),
      parameters.get("guarddutyRoleArn"),
      parameters.get("umbrellaRoleArn"),
    )
  }

  // TODO: ossec.conf should be valid XML, can we return XML or add a method to validate the conf?
  def createConf(wazuhFiles: WazuhFiles, parameters: WazuhParameters, nodeType: NodeType): WazuhFiles = {
    // 1. Replace value of 'node_name' with unique identifier
        // Each node of the cluster must have a unique name.
        // If two nodes share the same name, one of them will be rejected.
        // Can new workers have the same name as historical ones that have left the cluster?
    val newConf = wazuhFiles.ossecConf
      .replaceAll("<node_name>.+</node_name>", s"<node_name>${nodeType.toString.toLowerCase}</node_name>")
      // 2. Replace value of 'key' with the clusterKey value
      // This key must be the same for all of the nodes of the cluster.
      .replaceAll("<key>.+</key>", s"<key>${parameters.wazuhClusterKey.getOrElse("")}</key>")
      // 3. Replace value of 'node' with the coordinatorIP value
      .replaceAll("<node>.+</node>", s"<node>${parameters.coordinatorIP.getOrElse("")}</node>")

    // If NodeType is not Leader then removeSections
    if (nodeType == Worker) {
      wazuhFiles.copy(
        ossecConf = newConf.replace("<node_type>master</node_type>", "<node_type>worker</node_type>")
          .replaceAll("<gcp-pubsub>.+</gcp-pubsub>", "")
      )
    } else wazuhFiles.copy(ossecConf = newConf)
  }

  def getNodeType(instanceIp: String, parameters: WazuhParameters): NodeType = {
    if (parameters.coordinatorIP.contains(instanceIp)) Leader
    else Worker
  }

  // Only the leader instance should ingest logs from GCP and AWS
  // TODO: example input and outputs in resources
  def configureWorker(ossecConf: String): String = {
    // remove <wodle name="aws-s3"> and remove <gcp-pubsub>
    ???
  }

  // if it is the first time the service is running, the current files may be None
  // how do we tell the difference between populated and unpopulated files
  // TODO: decide if current conf should always be read from disk or cached?
  def getCurrentConf(path: String): IO[String, WazuhFiles] = {
    ???
  }

  // TODO: consider case class that represents the interpolation
  def hasChanges(incoming: WazuhFiles, current: WazuhFiles): Boolean = {
    ???
  }

  // TODO: decide if it would be helpful for this to indicate success / fail
  def writeConf(wazuhFiles: WazuhFiles): IO[String, Unit] = {
    ???
  }

  // should only be called if the conf has changed and should ideally return the status code
  // TODO: check if we need to poll to find out when restart is complete
  // TODO: check if sudo is needed to restart and work out how to avoid running as root
  // TODO: Consider if wazup should be responsible for running wazuh-manager entirely
  def restartWazuh(): IO[String, Unit] = {
    ???
  }

}
