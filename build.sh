#!/usr/bin/env bash
# Build BusyTasks Safe 1.1 completely offline using the supplied upstream jar.
set -euo pipefail
export LC_ALL=C.UTF-8
ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
OUT="$ROOT/dist"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$OUT" "$TMP/classes" "$TMP/stage"
UPSTREAM="$ROOT/vendor/BusyTasks-u2.9.jar"
if [[ ! -f "$UPSTREAM" ]]; then echo "Original JAR fehlt: $UPSTREAM" >&2; exit 2; fi
command -v javac >/dev/null || { echo "javac fehlt: sudo apt install openjdk-17-jdk" >&2; exit 2; }
javac -encoding UTF-8 --release 17 -cp "$UPSTREAM" -d "$TMP/classes" "$ROOT"/src/BusyTasks/*.java
(
  cd "$TMP/stage"
  unzip -qq "$UPSTREAM"
  # All original app classes are replaced with audited sources. Vendor libraries and resources remain.
  find BusyTasks -type f -name '*.class' -delete
  cp "$TMP/classes/BusyTasks/"*.class BusyTasks/
  cp "$ROOT"/src/BusyTasks/*.java BusyTasks/
  printf 'BusyTasks Safe 1.1 (inoffiziell)\n' > BusyTasks/Info/VERSION.txt
  # Repackage with an explicit new entrypoint; the classic widget remains available via --classic.
  rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA
  mkdir -p META-INF
  printf 'Manifest-Version: 1.0\r\nMain-Class: BusyTasks.BusyTasksDashboard\r\n\r\n' > META-INF/MANIFEST.MF
  jar --create --file "$OUT/BusyTasks-Safe-1.1.jar" --no-manifest -C "$TMP/stage" .
)
if [[ "${1:-}" == test ]]; then
   mkdir -p "$TMP/tests"
   javac -encoding UTF-8 --release 17 -cp "$OUT/BusyTasks-Safe-1.1.jar" -d "$TMP/tests" "$ROOT"/tests/BusyTasks/*.java
   for test in BusyTasks.SafeConfigTest BusyTasks.DetailsFilterTest BusyTasks.MonitorServiceTest; do
     java -cp "$TMP/tests:$OUT/BusyTasks-Safe-1.1.jar" "$test"
   done
fi
printf 'Build OK: %s\n' "$OUT/BusyTasks-Safe-1.1.jar"
