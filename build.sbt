ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.1"

Test / fork := true

val circeVersion = "0.14.2"
val doobieVersion = "1.0.0-RC1"
val http4sVersion = "0.23.12"
val tapirVersion = "1.0.0"
val testContainersVersion = "0.40.8"

lazy val root = (project in file("."))
  .settings(
    name := "blindnet-backend-scala",
    organization := "io.blindnet",
    organizationName := "Blindnet",
    organizationHomepage := Some(url("https://blindnet.io")),
    idePackagePrefix := Some("io.blindnet.backend"),
    libraryDependencies ++= Seq(
      "com.azure"                   %  "azure-storage-blob"              % "12.17.1",
      "com.dimafeng"                %% "testcontainers-scala-scalatest"  % testContainersVersion % Test,
      "com.dimafeng"                %% "testcontainers-scala-postgresql" % testContainersVersion % Test,
      "com.github.jwt-scala"        %% "jwt-circe"                       % "9.0.5",
      "com.softwaremill.sttp.tapir" %% "tapir-core"                      % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"             % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"                % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"         % tapirVersion,
      "io.circe"                    %% "circe-core"                      % circeVersion,
      "io.circe"                    %% "circe-generic"                   % circeVersion,
      "io.circe"                    %% "circe-literal"                   % circeVersion % Test,
      "io.netty"                    %  "netty-transport-native-epoll"    % "4.1.77.Final",
      "org.apache.commons"          %  "commons-lang3"                   % "3.12.0" % Test,
      "org.bouncycastle"            %  "bcprov-jdk15on"                  % "1.70",
      "org.flywaydb"                %  "flyway-core"                     % "8.5.12",
      "org.http4s"                  %% "http4s-blaze-server"             % http4sVersion,
      "org.http4s"                  %% "http4s-circe"                    % http4sVersion,
      "org.http4s"                  %% "http4s-dsl"                      % http4sVersion,
      "org.scalatest"               %% "scalatest"                       % "3.2.12" % Test,
      "org.slf4j"                   %  "slf4j-simple"                    % "1.7.36",
      "org.tpolecat"                %% "doobie-core"                     % doobieVersion,
      "org.tpolecat"                %% "doobie-hikari"                   % doobieVersion,
      "org.tpolecat"                %% "doobie-postgres"                 % doobieVersion,
      "org.typelevel"               %% "cats-effect"                     % "3.3.12",
      "org.typelevel"               %% "cats-effect-testing-scalatest"   % "1.4.0" % Test,
      "org.typelevel"               %% "log4cats-slf4j"                  % "2.3.1",
    ),
    assembly / mainClass := Some("io.blindnet.backend.Main"),
    assembly / assemblyJarName := "blindnet.jar",
    assembly / assemblyMergeStrategy := {
      case PathList(ps @ _*) if ps.last == "module-info.class" => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.discard
      case x => assemblyMergeStrategy.value(x)
    },
    assembly / packageOptions += Package.ManifestAttributes(
      "Multi-Release" -> "true"
    )
  )
