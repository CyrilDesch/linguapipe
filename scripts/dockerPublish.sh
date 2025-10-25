#!/usr/bin/env bash
set -e
sbt -mem 4096 "server/Docker/publish"
