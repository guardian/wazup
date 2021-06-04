import com.typesafe.sbt.packager.archetypes.systemloader.ServerLoader.Systemd

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.gu"
ThisBuild / organizationName := "The Guardian"

ThisBuild / scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code",
  "-deprecation",
  "-explaintypes",
)

val awsSdkVersion = "2.16.25"

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, JDebPackaging, SystemdPlugin)
  .settings(
    name := "wazup",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.1",
      "dev.zio" %% "zio" % "1.0.5",
      "dev.zio" %% "zio-process" % "0.3.0",
      "joda-time" % "joda-time" % "2.10.6",
      "software.amazon.awssdk" % "s3" % awsSdkVersion,
      "software.amazon.awssdk" % "auth" % awsSdkVersion,
      "software.amazon.awssdk" % "ssm" % awsSdkVersion,
      "software.amazon.awssdk" % "cloudwatch" % awsSdkVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.2.2" % Test
    ),
    javaOptions in Universal ++= Seq(
      s"-Dpidfile.path=/dev/null",
      s"-Dconfig.file=/etc/gu/wazup.conf",
    ),
    serverLoading in Debian := Some(Systemd),
    debianPackageDependencies := Seq("java8-runtime-headless"),
    // daemonUser and daemonGroup must be scoped to Linux to be applied
    // membership of ossec gives wazup permission to modify /var/ossec/
    daemonGroup in Linux := "ossec",

    maintainer in Debian := "Security Engineering",
    packageSummary in Debian := "Wazup: Automatically update Wazuh configuration",
    packageDescription in Debian := "Wazup: Automatically update Wazuh configuration",
  )
