import sbt.*
import sbt.Keys.*

object Dependencies {
  object Versions {
    val auth0                 = "4.5.0"
    val chimney               = "1.6.0"
    val flywaydb              = "11.8.1"
    val logback               = "1.5.18"
    val mUnit                 = "1.0.2"
    val postgresql            = "42.7.5"
    val quill                 = "4.8.6"
    val slf4j                 = "2.0.17"
    val tapir                 = "1.11.24"
    val zio                   = "2.1.17"
    val zioConfig             = "4.0.2"
    val zioLogging            = "2.2.4"
    val zioMock               = "1.0.0-RC12"
    val zioPrelude            = "1.0.0-RC36"
    val zioTestContainers     = "0.10.0"
  }

  private val configDependencies = Seq(
    "dev.zio" %% "zio-config"          % Versions.zioConfig,
    "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
    "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig
  )

  private val databaseDependencies = Seq(
    "org.flywaydb"           % "flyway-core"                       % Versions.flywaydb,
    "org.flywaydb"           % "flyway-database-postgresql"        % Versions.flywaydb,
    "org.postgresql"         % "postgresql"                        % Versions.postgresql,
    "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % Versions.zioTestContainers % Test
  )

  private val loggingDependencies = Seq(
    "dev.zio"       %% "zio-logging"       % Versions.zioLogging,
    "dev.zio"       %% "zio-logging-slf4j" % Versions.zioLogging,
    "ch.qos.logback" % "logback-classic"   % Versions.logback
  )

  private val quillDependencies = Seq(
    "io.getquill" %% "quill-jdbc-zio" % Versions.quill
  )

  private val jwtDependencies = Seq(
    "com.auth0" % "java-jwt" % Versions.auth0
  )

  val serverLibraryDependencies =
    libraryDependencies ++= Seq(
      "io.scalaland"                %% "chimney"                  % Versions.chimney,
      "com.softwaremill.sttp.tapir" %% "tapir-zio"                % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"    % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"   % Versions.tapir   % Test,
      "dev.zio"                     %% "zio-test"                 % Versions.zio,
      "dev.zio"                     %% "zio-test-junit"           % Versions.zio     % Test,
      "dev.zio"                     %% "zio-test-sbt"             % Versions.zio     % Test,
      "dev.zio"                     %% "zio-test-magnolia"        % Versions.zio     % Test,
      "dev.zio"                     %% "zio-mock"                 % Versions.zioMock % Test
    ) ++
      configDependencies ++
      databaseDependencies ++
      quillDependencies ++
      jwtDependencies ++
      loggingDependencies

  val testingLibraryDependencies =
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit"        % Versions.mUnit % Test,
      "dev.zio"       %% "zio-test"     % Versions.zio   % Test,
      "dev.zio"       %% "zio-test-sbt" % Versions.zio   % Test
    )

  val sharedLibraryDependencies: Setting[Seq[ModuleID]] =
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio"            % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-iron"           % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"       % Versions.tapir,
      "dev.zio"                     %% "zio-prelude"          % Versions.zioPrelude,
      "dev.zio"                     %% "zio-prelude-magnolia" % Versions.zioPrelude
    )
}
