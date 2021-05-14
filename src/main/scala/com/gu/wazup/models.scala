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
  cloudtrailRoleArn: Option[String],
  guarddutyRoleArn: Option[String],
  umbrellaRoleArn: Option[String],
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
