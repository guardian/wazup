package com.gu.wazup.model

sealed trait NodeType
case object Leader extends NodeType
case object Worker extends NodeType

case class Configuration(
  nodeType: NodeType,
  bucket: String,
  bucketPath: String,
  parameterPrefix: String,
  confPath: String,
)

case class WazuhConf(
  wazuhParameters: WazuhParameters,
  wazuhFiles: WazuhFiles,
)

case class WazuhParameters(
  clusterKey: Option[String],
  leaderAddress: Option[String],
  hiveParamPath: Option[String],
)

case class WazuhFiles(
  ossecConf: String,
  otherConf: List[ConfigFile] = List.empty,
)

case class ConfigFile(
  filename: String,
  content: String,
)
object ConfigFile {
  implicit val ordering: Ordering[ConfigFile] = Ordering.by(_.filename)
}