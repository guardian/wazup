package com.gu.wazup


sealed trait NodeType
case object Leader extends NodeType
case object Worker extends NodeType

case class WazuhConf(
  wazuhParameters: WazuhParameters,
  wazuhFiles: WazuhFiles,
)

case class WazuhParameters(
  wazuhClusterKey: Option[String],
  coordinatorIP: Option[String],
  agentSecretArn: Option[String],
  cloudtrailRoleArn: Option[String],
  guarddutyRoleArn: Option[String],
  umbrellaRoleArn: Option[String],
)

case class WazuhFiles(
  ossecConf: String,
  // decoders/ contains one or more files
  decoders: List[ConfigFile] = List.empty,
  // lists/ contains one or more files
  lists: List[ConfigFile] = List.empty,
  // rules/ contains one or more files
  rules: List[ConfigFile] = List.empty,
)

case class ConfigFile(
  filename: String,
  content: String,
)