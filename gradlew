#!/usr/bin/env sh
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VER=8.7
DIST="$DIR/.gradle-bootstrap/gradle-$VER"
ZIP="$DIR/.gradle-bootstrap/gradle-$VER-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$DIR/.gradle-bootstrap"
  URL="https://services.gradle.org/distributions/gradle-$VER-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -L --fail "$URL" -o "$ZIP";
  elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" "$URL";
  else echo "Need curl or wget to bootstrap Gradle $VER" >&2; exit 1; fi
  unzip -q -o "$ZIP" -d "$DIR/.gradle-bootstrap"
fi
exec "$DIST/bin/gradle" "$@"
