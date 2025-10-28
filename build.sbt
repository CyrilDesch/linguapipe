import Dependencies.*
import DeploymentSettings.*

val scala3 = "3.7.3"

name := "LinguaPipe"

inThisBuild(
  List(
    scalaVersion := scala3,
    dependencyOverrides ++= Seq(
      "org.scala-lang" %% "scala3-library" % scala3,
      "dev.zio"        %% "zio-json"       % "0.7.40"
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
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
    Docker / maintainer     := "Cyril Deschamps",
    Docker / dockerUsername := Some("cyril-deschamps"),
    Docker / packageName    := "linguapipe",
    dockerBaseImage         := "azul/zulu-openjdk-alpine:21-jre-headless",
    dockerRepository        := Some("ghcr.io"),
    dockerUpdateLatest      := true,
    dockerExposedPorts      := Seq(8000),
    // Use a simple tag format without version to avoid invalid characters
    Docker / version := "latest"
  )
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
    },
    // Exclude unnecessary files to reduce JAR size
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      cp.filter { file =>
        val name = file.data.getName
        name.contains("scala-compiler") ||
        name.contains("scala-reflect") ||
        name.contains("scalap") ||
        name.contains("test") ||
        name.contains("junit") ||
        name.contains("scalatest") ||
        name.contains("munit")
      }
    }
  )
}
