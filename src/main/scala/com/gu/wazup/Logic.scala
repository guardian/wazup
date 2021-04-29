package com.gu.wazup

import zio.IO


object Logic {
  // TODO: change left to be a failure so we can return more information
  def fetchConf(bucket: String, path: String): IO[String, WazuhConf] = {
    ???
    // 1. fetch parameters from parameter store
    // 2. fetch files from S3 bucket and (optional) path
  }

  // TODO: ossec.conf should be valid XML, can we return XML or add a method to validate the conf?
  def interpolateOssecConf(parameters: WazuhParameters, ossecConf: String): IO[String, String] = ???

  // if it is the first time the service is running, the current files will be None
  // how do we tell the difference between populated and unpopulated files
  // TODO: decide if current conf should always be read from disk or cached?
  def getCurrentConf: IO[String, Option[WazuhFiles]] = {
    ???
  }

  // if it is the first time the service is running, the current files will be None
  // TODO: consider case class that represents the interpolation
  def shouldUpdate(incoming: WazuhFiles, current: Option[WazuhFiles]): Boolean = {
    ???
  }

  // TODO: decide if it would be helpful for this to indicate success / fail
  def writeConf(wazuhFiles: WazuhFiles): IO[String, Unit] = {
    ???
  }

  // should only be called if the conf has changed and should ideally return the status code
  // TODO: check if we need to poll to find out when restart is complete
  // TODO: check if sudo is needed to restart and work out how to avoid running as root
  def restartWazuh(): IO[String, Unit] = ???

}
