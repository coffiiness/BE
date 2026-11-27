#!/bin/sh
# Official Eclipse Temurin release assets; hashes are pinned, never fetched at build time.
set -eu
kind="${1:?jdk or jre required}"
arch="${2:?amd64 or arm64 required}"
destination="${3:-${JAVA_HOME:?JAVA_HOME required}}"
case "$kind:$arch" in
  jdk:amd64) asset=x64; checksum=0b0ac5eab6db6abaec68c9913bcddb31321e5da411c45073d520d9c125e6f7b4 ;;
  jdk:arm64) asset=aarch64; checksum=1d808f4b558e9de63d8ba3ead21ffc8b9de204894eff3c19d52c1480aa17cc58 ;;
  jre:amd64) asset=x64; checksum=5a092a0bc21de4c3d855480edd7945be9cfe157b38cdb84dac280511893bb009 ;;
  jre:arm64) asset=aarch64; checksum=ed968eb2cde06ba38c3d957956cab7faced9181aaeebed4904f89e2e6efa8d11 ;;
  *) echo "Unsupported Temurin kind/architecture: $kind/$arch" >&2; exit 1 ;;
esac
archive="$(mktemp)"
trap 'rm -f "$archive"' EXIT INT TERM
url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21.1%2B2/OpenJDK17U-${kind}_${asset}_linux_hotspot_17.0.21.1_2.tar.gz"
curl --fail --location --retry 3 --connect-timeout 20 --output "$archive" "$url"
printf '%s  %s\n' "$checksum" "$archive" | sha256sum --check --strict -
mkdir -p "$destination"
tar --extract --gzip --file "$archive" --directory "$destination" --strip-components=1 --no-same-owner

