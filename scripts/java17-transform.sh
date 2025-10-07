#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <input-jar> <output-jar> [CLI options...]" >&2
  exit 1
fi

INPUT_JAR=$1
shift
OUTPUT_JAR=$1
shift

# Resolve project root relative to this script
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

# Allow callers to override the Java 17 home by exporting JAVA_17_HOME
JAVA_17_HOME=${JAVA_17_HOME:-}

if [ -z "$JAVA_17_HOME" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
fi

if [ -z "$JAVA_17_HOME" ] && [ -n "${JAVA_HOME_17_X64:-}" ]; then
  JAVA_17_HOME=$JAVA_HOME_17_X64
fi

if [ -z "$JAVA_17_HOME" ] && [ -n "${JAVA_HOME:-}" ]; then
  if "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q '"17'; then
    JAVA_17_HOME=$JAVA_HOME
  fi
fi

if [ -z "$JAVA_17_HOME" ] && [ -x "/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
  JAVA_17_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home"
fi

if [ -z "$JAVA_17_HOME" ] || [ ! -x "$JAVA_17_HOME/bin/java" ]; then
  echo "Unable to locate a Java 17 runtime. Set JAVA_17_HOME before running." >&2
  exit 1
fi

# Locate required transformer artifacts in the local Maven repository.
REPO_BASE="${HOME}/.m2/repository"

declare -a REQUIRED_JARS=(
  "org/eclipse/transformer/org.eclipse.transformer.cli/1.0.0/org.eclipse.transformer.cli-1.0.0.jar"
  "org/eclipse/transformer/org.eclipse.transformer/1.0.0/org.eclipse.transformer-1.0.0.jar"
  "org/eclipse/transformer/org.eclipse.transformer.jakarta/1.0.0/org.eclipse.transformer.jakarta-1.0.0.jar"
  "biz/aQute/bnd/biz.aQute.bnd.transform/7.0.0/biz.aQute.bnd.transform-7.0.0.jar"
  "commons-cli/commons-cli/1.9.0/commons-cli-1.9.0.jar"
  "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
  "org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"
)

CLASSPATH_ENTRIES=()
for rel_path in "${REQUIRED_JARS[@]}"; do
  jar_path="${REPO_BASE}/${rel_path}"
  if [ ! -f "$jar_path" ]; then
    cat >&2 <<EOF
Required dependency not found: $jar_path
Run a Maven build (e.g. `mvn dependency:go-offline`) or manually fetch the artifact before invoking this script.
EOF
    exit 1
  fi
  CLASSPATH_ENTRIES+=("$jar_path")
done

TRANSFORMER_CP=$(IFS=:; echo "${CLASSPATH_ENTRIES[*]}")

OUTPUT_DIR=$(dirname "$OUTPUT_JAR")
mkdir -p "$OUTPUT_DIR"

cd "$PROJECT_ROOT"

"$JAVA_17_HOME/bin/java" \
  -cp "$TRANSFORMER_CP" \
  org.eclipse.transformer.cli.JakartaTransformerCLI \
  "$INPUT_JAR" "$OUTPUT_JAR" "$@"
