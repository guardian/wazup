package com.gu.wazup


case class WazuhConf(
  wazuhParameters: WazuhParameters,
  wazuhFiles: WazuhFiles,
)

case class WazuhParameters(
  wazuhClusterKey: String,
  coordinatorIP: String,
  agentSecretArn: String,
  cloudtrailRoleArn: Option[String],
  guarddutyRoleArn: Option[String],
  umbrellaRoleArn: Option[String],
)

// TODO: consider switching from tuples to case classes
case class WazuhFiles(
  ossecConf: String,
  // decoders/ contains one or more files
  decoders: List[(String, String)],
  // lists/ contains one or more files
  lists: List[(String, String)],
  // rules/ contains one or more files
  rules: List[(String, String)],
)