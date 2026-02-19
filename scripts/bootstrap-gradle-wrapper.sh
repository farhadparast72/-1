#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="${1:-8.2.1}"

if [[ -f "gradle/wrapper/gradle-wrapper.jar" ]]; then
  echo "Gradle wrapper JAR already exists: gradle/wrapper/gradle-wrapper.jar"
  exit 0
fi

echo "Bootstrap: generating Gradle Wrapper using a temporary Gradle distribution..."
echo "Target Gradle version: ${GRADLE_VERSION}"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

zip_path="$tmp_dir/gradle.zip"

curl -fL --retry 5 --retry-delay 2 --retry-connrefused   -o "$zip_path" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

file "$zip_path" | grep -qi "zip" || (echo "Downloaded file is not a ZIP!" && file "$zip_path" && exit 1)

unzip -q "$zip_path" -d "$tmp_dir"
gradle_bin="$tmp_dir/gradle-${GRADLE_VERSION}/bin/gradle"

if [[ ! -x "$gradle_bin" ]]; then
  echo "Gradle binary not found after unzip. Debug:"
  find "$tmp_dir" -maxdepth 3 -type f | head -200
  exit 1
fi

"$gradle_bin" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin

echo "Wrapper generated."
ls -la gradle/wrapper || true
