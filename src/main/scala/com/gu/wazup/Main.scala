package com.gu.wazup

import com.gu.wazup.aws.AWS
import software.amazon.awssdk.regions.Region
import zio.console._
import zio.{ExitCode, IO, URIO, ZIO}


object Main extends zio.App {
  private val s3Client = AWS.s3Client("infosec", Region.of("eu-west-1"))
  private val systemsManagerClient = AWS.systemsManagerClient("infosec", Region.of("eu-west-1"))
  private val cloudwatchClient = AWS.cloudwatchClient("infosec", Region.of("eu-west-1"))

  def run(args: List[String]): URIO[Console, ExitCode] =
    wazup.forever.exitCode

  val wazup: ZIO[Console, Serializable, Unit] =
    for {
      _    <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
    } yield ()

}
