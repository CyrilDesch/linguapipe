import Dependencies._
import DeploymentSettings._

val scala3 = "3.7.3"

name := "LinguaPipe"

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

lazy val root = project
  .in(file("."))
  .aggregate(
    domain,
    application,
    infrastructure
  )
  .disablePlugins(RevolverPlugin)
  .settings(
    publish / skip := true
  )

lazy val domain = project
  .in(file("modules/domain"))
  .disablePlugins(RevolverPlugin)
  .settings(
    domainLibraryDependencies,
    publish / skip := true
  )

lazy val application = project
  .in(file("modules/application"))
  .disablePlugins(RevolverPlugin)
  .dependsOn(domain)
  .settings(
    applicationLibraryDependencies,
    publish / skip := true
  )

lazy val infrastructure = project
  .in(file("modules/infrastructure"))
  .enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)
  .dependsOn(application)
  .settings(
    fork      := true,
    mainClass := Some("linguapipe.infrastructure.Main"),
    infrastructureLibraryDependencies,
    testingLibraryDependencies,
    publish / skip := true
  )
  .settings(dockerSettings)
  .settings(assemblySettings)

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

lazy val assemblySettings = {
  import sbtassembly.AssemblyPlugin.autoImport._
  import sbtassembly.MergeStrategy
  Seq(
    assembly / assemblyMergeStrategy := {
      case PathList("deriving.conf")                                  => MergeStrategy.concat
      case PathList("scala", "annotation", "unroll.tasty")            => MergeStrategy.first
      case PathList("scala", "annotation", "unroll.class")            => MergeStrategy.first
      case PathList("module-info.class")                              => MergeStrategy.discard
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties")       => MergeStrategy.first
      case x if x.contains("io/getquill/")                            => MergeStrategy.first
      case x                                                          =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )
}
