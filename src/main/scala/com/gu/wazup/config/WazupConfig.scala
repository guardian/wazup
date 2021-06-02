package com.gu.wazup.config

import com.gu.wazup.Configuration
import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import scala.util.Try


object WazupConfig {

  def load(): Configuration = {
    loadContents(ConfigFactory.load()).fold(
      { errMsg =>
        throw new WazupConfigException(s"Failed to load configuration: $errMsg")
      },
      identity
    )
  }

  private def loadContents(config: Config): Either[String, Configuration] = {
    for {
      bucket <- handleErrors(config, "wazup.bucket")
      bucketPath <- handleErrors(config, "wazup.bucketPath")
      parameterPrefix <- handleErrors(config, "wazup.parameterPrefix")
      confPath <- handleErrors(config, "wazup.confPath")
    } yield Configuration(bucket, bucketPath, parameterPrefix, confPath)
  }

  private def handleErrors(config: Config, path: String): Either[String, String] = {
    Try(config.getString(path)).toEither.left.map(_.getMessage)
  }

  class WazupConfigException(message: String) extends ConfigException(message)
}
