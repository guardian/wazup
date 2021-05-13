package com.gu.wazup

import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse
import zio.blocking.{Blocking, effectBlocking}
import zio.process.{Command, CommandError}
import zio.{ExitCode, ZIO}

import java.io.{BufferedWriter, File, FileWriter}
import java.net.InetAddress
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal


object Logic {

  def parseParameters(response: GetParametersByPathResponse, prefix: String): WazuhParameters = {
    val parameters = response.parameters.asScala.toList.map { param =>
      param.name.stripPrefix(prefix) -> param.value
    }.toMap

    WazuhParameters(
      // TODO: should missing parameters raise an exception?
      parameters.get("wazuhClusterKey"),
      parameters.get("coordinatorIP"),
      parameters.get("cloudtrailRoleArn"),
      parameters.get("guarddutyRoleArn"),
      parameters.get("umbrellaRoleArn"),
    )
  }

  def getWazuhFiles(configFiles: List[ConfigFile], bucketPath: String): Either[String, WazuhFiles] = {
    configFiles.map { file =>
      file.copy(filename = file.filename.stripPrefix(bucketPath))
    }.partition(file => file.filename.endsWith("ossec.conf")) match {
      case (ossecConf :: Nil, otherFiles) => Right(WazuhFiles(ossecConf.content, otherFiles))
      case (Nil, _) => Left(s"Missing ossec.conf file in $bucketPath")
      case _ => Left("More than one ossec.conf found")
    }
  }

  // TODO: ossec.conf should be valid XML, can we return XML or add a method to validate the conf?
  def createConf(wazuhFiles: WazuhFiles, parameters: WazuhParameters, nodeType: NodeType, nodeAddress: String): WazuhFiles = {
    val newConf = wazuhFiles.ossecConf
      .replaceAll("<node_name>.+</node_name>", s"<node_name>${nodeType.toString.toLowerCase}-$nodeAddress</node_name>")
      .replaceAll("<key>.+</key>", s"<key>${parameters.wazuhClusterKey.getOrElse("")}</key>")
      .replaceAll("<node>.+</node>", s"<node>${parameters.coordinatorIP.getOrElse("")}</node>")
    if (nodeType == Worker) wazuhFiles.copy(ossecConf = configureWorker(newConf))
    else wazuhFiles.copy(ossecConf = newConf)
  }

  def getNodeAddress: ZIO[Blocking, String, String] = {
    effectBlocking(InetAddress.getLocalHost.getHostAddress).refineOrDie(_.getMessage)
  }

  def getNodeType(instanceIp: String, parameters: WazuhParameters): NodeType = {
    if (parameters.coordinatorIP.contains(instanceIp)) Leader
    else Worker
  }

  def configureWorker(ossecConf: String): String = {
    ossecConf.replace("<node_type>master</node_type>", "<node_type>worker</node_type>")
      .replaceAll("[\\s]*<gcp-pubsub>(?s)(.*)</gcp-pubsub>", "")
      .replaceAll("[\\s]*<wodle name=\"aws-s3\">(?s)(.*)</wodle>", "")
  }

  def getCurrentConf(path: String, directories: List[String]): ZIO[Blocking, String, WazuhFiles] = {
    readTextFile(s"$path/ossec.conf").map(conf => WazuhFiles(conf.getOrElse("")))
  }

  def hasChanges(incoming: WazuhFiles, current: WazuhFiles): Boolean = {
    incoming.ossecConf != current.ossecConf
  }

  // TODO: decide if it would be helpful for this to indicate success / fail
  def writeConf(path: String, wazuhFiles: WazuhFiles): ZIO[Blocking, String, Unit] = {
    writeTextFile(s"$path/ossec.conf", wazuhFiles.ossecConf)
  }

  // should only be called if the conf has changed and should ideally return the status code
  // TODO: check if we need to poll to find out when restart is complete
  // TODO: check if sudo is needed to restart and work out how to avoid running as root
  def restartWazuh(): ZIO[Blocking, CommandError, ExitCode] = {
    Command("systemctl", "restart", "wazuh-manager").run
      .flatMap(process => process.exitCode)
  }

  private def readTextFile(fileName: String): ZIO[Blocking, String, Option[String]] = {
    effectBlocking {
      val file = new File(fileName)
      if (file.exists) {
        val source = Source.fromFile(fileName)
        try {
          Some(source.mkString)
        } finally {
          source.close()
        }
      } else None
    }.refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }

  private def writeTextFile(filePath: String, content: String): ZIO[Blocking, String, Unit] = {
    effectBlocking {
      val file = new File(filePath)
      val writer = new BufferedWriter(new FileWriter(file))
      try {
        writer.write(content)
      } finally {
        writer.close()
      }
    }.refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }
}
