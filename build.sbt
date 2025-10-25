import Dependencies._
import DeploymentSettings._

val scala3 = "3.7.3"

name := "TP_API"

inThisBuild(
  List(
    scalaVersion                            := scala3,
    dependencyOverrides += "org.scala-lang" %% "scala3-library" % scala3,
    semanticdbEnabled                       := true,
    semanticdbVersion                       := scalafixSemanticdb.revision,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Wunused:all"
//      "-Xfatal-warnings"
    ),
    run / fork := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
)

// Aggregate root project
// This is the root project that aggregates all other projects
// It is used to run tasks on all projects at once.
lazy val root = project
  .in(file("."))
  .aggregate(server, shared)
  .disablePlugins(RevolverPlugin)
  .settings(
    publish / skip := true
  )

//
// Server project depending on the shared domain module.
//
lazy val server = project
  .in(file("modules/server"))
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)
  .settings(
    fork := true,
    serverLibraryDependencies,
    testingLibraryDependencies
  )
  .settings(dockerSettings)
  .dependsOn(shared)
  .settings(
    publish / skip := true
  )

//
// Shared project
//
lazy val shared = project
  .in(file("modules/shared"))
  .disablePlugins(RevolverPlugin)
  .settings(
    sharedLibraryDependencies
  )
  .settings(
    publish / skip := true
  )

Test / fork := false

lazy val dockerSettings = {
  import DockerPlugin.autoImport._
  import DockerPlugin.globalSettings._
  import sbt.Keys._
  Seq(
    Docker / maintainer     := "Joh doe",
    Docker / dockerUsername := Some("johndoe"),
    Docker / packageName    := "tp_api",
    dockerBaseImage         := "azul/zulu-openjdk-alpine:24-latest",
    dockerRepository        := Some("registry.orb.local"),
    dockerUpdateLatest      := true,
    dockerExposedPorts      := Seq(8000)
  ) ++ (overrideDockerRegistry match {
    case true =>
      Seq(
        Docker / dockerRepository := Some("registry.orb.local"),
        Docker / dockerUsername   := Some("napnotes-backend")
      )
    case false =>
      Seq()
  })
}
