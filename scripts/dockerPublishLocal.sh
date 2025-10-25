#!/usr/bin/env bash
set -e
sbt "server/Docker/publishLocal"
