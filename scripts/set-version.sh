#!/usr/bin/env bash
#
# set-version.sh — set the release version everywhere it is written down (#376).
#
# `mvn versions:set` changes the poms. Two more places name the version and used to be
# missed: the compose files' image tags (rreganjr/requel:<version>) and requel-cli's
# --version text. spring.ai.mcp.server.version is filtered from the pom (@project.version@)
# and needs nothing.
#
# Usage (from the repo root):
#   ./scripts/set-version.sh 2.0.0-rc1
#   ./scripts/set-version.sh 2.0.0
#   ./scripts/set-version.sh 2.1.0-dev
# Then stage what it lists and commit (see doc/guides/RELEASE_PROCESS.md).
#
set -euo pipefail
VERSION="${1:?usage: scripts/set-version.sh <version>}"
cd "$(git rev-parse --show-toplevel)"

mvn -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

for f in docker-compose.yml docker-compose.local-ai.yml; do
  sed -i.bak -E "s#(image: rreganjr/requel:)[^[:space:]\"]+#\1${VERSION}#" "$f" && rm -f "$f.bak"
done
CLI=modules/requel-cli/src/main/java/com/rreganjr/requel/cli/RequelCli.java
sed -i.bak -E "s#(version = \"requel-cli )[^\"]+\"#\1${VERSION}\"#" "$CLI" && rm -f "$CLI.bak"

echo "Version set to $VERSION. Changed:"
git status --short -- '*pom.xml' docker-compose.yml docker-compose.local-ai.yml "$CLI"
