# NapNotes Backend

Backend-only Scala 3 project built with ZIO, Tapir and ZIO HTTP. The former Scala.js/Laminar frontend has been removed so the focus is purely on the API stack.

## Prerequisites

- JDK 21+
- sbt
- Docker (optional, for container images)

## Usage

- Run the service in the foreground:

  ```bash
  sbt "server/run"
  ```

- Reload on code changes:

  ```bash
  sbt "~server/reStart"
  ```

- Run the test suite:

  ```bash
  sbt "server/test"
  ```

- Publish a Docker image:

  ```bash
  sbt "server/Docker/publishLocal"
  ```

## Modules

- `modules/shared`: shared domain and endpoint models for the backend.
- `modules/server`: ZIO HTTP server, repositories and services.

Everything related to Node.js, Scala.js and Laminar has been deleted.
