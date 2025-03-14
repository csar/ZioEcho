ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

enablePlugins(JavaAppPackaging,DockerPlugin)
enablePlugins(LauncherJarPlugin)

Compile / mainClass := Some("Run")
dockerExposedPorts := Seq(8080)
dockerBaseImage:="amazoncorretto:21"

lazy val root = (project in file("."))
  .settings(
    name := "ZioEcho",
    libraryDependencies := Seq(
      "dev.zio" %% "zio-http" % "3.1.0",
      "dev.zio" %% "zio-schema" % "1.6.5",
      "com.spotify" % "docker-client" % "8.16.0"
    )
  )
