ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.gu"
ThisBuild / organizationName := "example"

val awsSdkVersion = "2.16.25"

lazy val root = (project in file("."))
  .settings(
    name := "wazup",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.5",
      "dev.zio" %% "zio-process" % "0.3.0",
      "software.amazon.awssdk" % "s3" % awsSdkVersion,
      "software.amazon.awssdk" % "auth" % awsSdkVersion,
      "software.amazon.awssdk" % "ssm" % awsSdkVersion,
      "software.amazon.awssdk" % "cloudwatch" % awsSdkVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.scalatest" %% "scalatest" % "3.2.2" % Test
    )
  )
