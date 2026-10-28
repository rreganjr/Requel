#!/usr/bin/env bash
#
# check-coverage-modules.sh — fail when a module with main code is missing from the coverage
# aggregate (#321).
#
# modules/coverage-report builds the one JaCoCo aggregate CI uploads to Codecov. report-aggregate
# covers only the modules that pom declares as dependencies, so a new module, or one only reached
# transitively, silently reads as its own-tests-only figure in Codecov. This compares every
# modules/*/ with src/main/java (the directory name is its artifactId) with the <group> names in
# the aggregate XML.
#
# Usage (from the repo root, after `mvn verify -pl modules/coverage-report -am`):
#   .github/scripts/check-coverage-modules.sh [path/to/jacoco.xml]
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
XML="${1:-modules/coverage-report/target/site/jacoco-aggregate/jacoco.xml}"
[ -f "$XML" ] || { echo "No coverage aggregate at $XML; build modules/coverage-report first." >&2; exit 1; }

expected="$(for dir in modules/*/; do
  [ -d "${dir}src/main/java" ] && basename "$dir"
done | sort)"
present="$(grep -o '<group name="[^"]*"' "$XML" | sed 's/<group name="\(.*\)"/\1/' | sort -u)"
missing="$(comm -23 <(echo "$expected") <(echo "$present"))"

if [ -n "$missing" ]; then
  echo "Missing from the coverage aggregate ($XML):" >&2
  echo "$missing" | sed 's/^/  /' >&2
  echo "Add each as a dependency of modules/coverage-report/pom.xml (see doc/guides/COVERAGE.md)." >&2
  exit 1
fi
echo "Coverage aggregate covers all $(echo "$expected" | wc -l | tr -d ' ') modules with main code."
