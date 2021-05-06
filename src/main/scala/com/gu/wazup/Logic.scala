package com.gu.wazup

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import zio.IO


object Logic {
  // TODO: change left to be a failure so we can return more information
  def fetchFiles(client: S3Client, bucket: String, prefix: String): IO[String, WazuhFiles] = {
    ???
  }

  def fetchParameters(client: SsmAsyncClient, prefix: String): IO[String, Map[String, String]] = {
    ???

  }

  def parseParameters(parameters: Map[String, String]): WazuhParameters = {
    ???
  }

  // TODO: ossec.conf should be valid XML, can we return XML or add a method to validate the conf?
  def createConf(wazuhFiles: WazuhFiles, parameters: WazuhParameters, nodeType: NodeType): IO[String, WazuhFiles] = {
    ???
  }

  // Determine what type of node this is, by one of these options:
    // 1. Check is our IP matches coordinatorIP (current selection)
    // 2. Lookup the instance tags for a particular set
    // 3. lookup value from /etc/nodetype
  def getNodeType(instanceIp: String, parameters: WazuhParameters): NodeType = {
      instanceIp match {
        case parameters.coordinatorIP => Leader
        case _ => Worker
      }
  }

  // Only one instance should ingest logs from GCP and AWS
  private[logic] def removeWodleSections(ossecConf: String): String = {
    ???
  }

  // if it is the first time the service is running, the current files may be None
  // how do we tell the difference between populated and unpopulated files
  // TODO: decide if current conf should always be read from disk or cached?
  def getCurrentConf(path: String): IO[String, WazuhFiles] = {
    ???
  }

  // if it is the first time the service is running, the current files will be None
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
