#!/bin/sh
set -eu
arch="$1"
[ "$(dpkg --print-architecture)" = "$arch" ] || { echo "Architecture mismatch" >&2; exit 1; }
properties="$(mktemp)"
trap 'rm -f "$properties"' EXIT INT TERM
"$JAVA_HOME/bin/java" -XshowSettings:properties -version >"$properties" 2>&1
grep -F 'java.runtime.version = 17.0.21.1+2' "$properties"
grep -F 'java.vendor = Eclipse Adoptium' "$properties"
case "$arch" in
  arm64) grep -F 'os.arch = aarch64' "$properties" ;;
  amd64) grep -F 'os.arch = amd64' "$properties" ;;
  *) echo "Unsupported architecture" >&2; exit 1 ;;
esac

