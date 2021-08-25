package com.gu.wazup

import java.io.{BufferedWriter, File, FileWriter}
import java.net.InetAddress

import com.gu.wazup.model._
import org.joda.time.DateTime
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse
import zio.ZIO
import zio.blocking.{Blocking, effectBlocking}
import zio.process.Command

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
      parameters.get("cluster-key"),
      parameters.get("leader-address"),
    )
  }

  def getWazuhFiles(configFiles: List[ConfigFile], path: String): Either[String, WazuhFiles] = {
    configFiles.map { file =>
      file.copy(filename = file.filename.stripPrefix(path))
    }.partition(file => file.filename.endsWith("ossec.conf")) match {
      case (ossecConf :: Nil, otherFiles) => Right(WazuhFiles(ossecConf.content, otherFiles))
      case (Nil, _) => Left(s"Could not retrieve ossec.conf from $path")
      case _ => Left("More than one ossec.conf found")
    }
  }

  // TODO: ossec.conf should be valid XML, can we return XML or add a method to validate the conf?
  def createConf(wazuhFiles: WazuhFiles, parameters: WazuhParameters, nodeType: NodeType, nodeAddress: String, logsAfter: DateTime): WazuhFiles = {
    val nodeName = s"${nodeType.toString.toLowerCase}-$nodeAddress"
    val newConf = wazuhFiles.ossecConf
      .replaceAll("<node_name>.+</node_name>", s"<node_name>$nodeName</node_name>")
      .replaceAll("<key>.+</key>", s"<key>${parameters.clusterKey.getOrElse("")}</key>")
      .replaceAll("<node>.+</node>", s"<node>${parameters.leaderAddress.getOrElse("")}</node>")
      .replaceAll("<only_logs_after>.+</only_logs_after>", s"<only_logs_after>${Date.formatDate(logsAfter)}</only_logs_after>")
    if (nodeType == Worker) wazuhFiles.copy(ossecConf = configureWorker(newConf))
    else wazuhFiles.copy(ossecConf = newConf)
  }

  def getNodeAddress: ZIO[Blocking, String, String] = {
    effectBlocking(InetAddress.getLocalHost.getHostAddress).refineOrDie(_.getMessage)
  }

  // Only one node in the cluster needs to ingest logs from GCP and AWS
  // The GCP and AWS wodle sections are therefore removed for the Workers
  def configureWorker(ossecConf: String): String = {
    ossecConf.replace("<node_type>master</node_type>", "<node_type>worker</node_type>")
      .replaceAll("[\\s]*<gcp-pubsub>(?s)(.*)</gcp-pubsub>", "")
      .replaceAll("[\\s]*<wodle name=\"aws-s3\">(?s)(.*)</wodle>", "")
  }

  def hasChanges(incoming: WazuhFiles, current: WazuhFiles): Boolean = {
    incoming.ossecConf != current.ossecConf ||
      incoming.otherConf.toSeq.sorted != current.otherConf.toSeq.sorted
  }

  def notRunning(): ZIO[Blocking, String, Boolean] = {
    Command("systemctl", "status", "wazuh-manager").successfulExitCode
      .mapEffect(process => process.code == 0)
      .mapError(err => err.getMessage)
  }

  def readFile(filePath: String): ZIO[Blocking, String, String] = {
    effectBlocking {
      val file = new File(filePath)
      val source = Source.fromFile(file)
        try {
          source.mkString
        } finally {
          source.close()
        }
    }.refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }

  def writeFile(filePath: String, content: String): ZIO[Blocking, String, Unit] = {
    effectBlocking {
      val file = new File(filePath)
      file.getParentFile.mkdirs()
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

  private def listDirectory(directory: File): Array[File] = {
    val these = directory.listFiles
    these ++ these.filter(_.isDirectory).flatMap(listDirectory)
  }

  def listFiles(directory: String): ZIO[Blocking, String, List[String]] = {
    effectBlocking {
      listDirectory(new File(directory))
        .filter(_.isFile)
        .map(_.getPath)
        .toList
    }.refineOrDie {
      case NonFatal(t) => t.getMessage
    }
  }
}
