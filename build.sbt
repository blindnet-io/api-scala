ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.1.1"

val circeVersion = "0.14.1"
val doobieVersion = "1.0.0-RC1"
val http4sVersion = "0.23.10"

lazy val root = (project in file("."))
  .settings(
    name := "blindnet-backend-scala",
    idePackagePrefix := Some("io.blindnet.backend"),
    libraryDependencies ++= Seq(
      "com.github.jwt-scala"   %% "jwt-circe"                  % "9.0.4",
      "org.typelevel"          %% "cats-effect"                % "3.3.5",
      "org.http4s"             %% "http4s-blaze-server"        % http4sVersion,
      "org.http4s"             %% "http4s-circe"               % http4sVersion,
      "org.http4s"             %% "http4s-dsl"                 % http4sVersion,
      "org.slf4j"              %  "slf4j-simple"               % "1.7.36",
      "org.tpolecat"           %% "doobie-core"                % doobieVersion,
      "org.tpolecat"           %% "doobie-postgres"            % doobieVersion,
      "io.circe"               %% "circe-core"                 % circeVersion,
      "io.circe"               %% "circe-generic"              % circeVersion,
    )
  )
