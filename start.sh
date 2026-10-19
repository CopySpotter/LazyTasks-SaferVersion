#!/usr/bin/env bash
set -euo pipefail
DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
if ! command -v java >/dev/null 2>&1; then
  echo 'Java fehlt. Bitte im Terminal ausführen: sudo apt install openjdk-17-jre' >&2
  exit 1
fi
exec java -jar "$DIR/dist/BusyTasks-Safe-1.1.jar" "$@"
