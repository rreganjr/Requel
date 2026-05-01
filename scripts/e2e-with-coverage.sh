#!/usr/bin/env bash
# scripts/e2e-with-coverage.sh
#
# Runs the full Playwright E2E suite against the Docker-composed application
# and collects coverage for both layers:
#
#   JavaScript  — Playwright V8 coverage via monocart-reporter
#                 coverage HTML: requel-angular/coverage/index.html
#                 test report:   requel-angular/playwright-report/e2e-coverage.html
#
#   Java        — JaCoCo TCP-server mode; agent attaches to the running JVM,
#                 coverage dumped after tests finish
#                 output: coverage/java/index.html
#
# Prerequisites:
#   • Docker + Docker Compose
#   • Maven 3.6.3+ (mvn on PATH)
#   • Node 22+ / npm (for Playwright)
#   • Project built at least once so compiled classes exist in target/:
#       mvn -pl modules/requel-app -am package -DskipTests
#   • monocart-reporter installed:
#       cd requel-angular && npm install
#
# Usage:
#   bash scripts/e2e-with-coverage.sh
#
# Environment variables (optional):
#   E2E_ADMIN_USERNAME / E2E_ADMIN_PASSWORD   (default: admin / admin)
#   E2E_PROJECT_USERNAME / E2E_PROJECT_PASSWORD (default: project / project)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COVERAGE_DIR="$REPO_ROOT/coverage"
JACOCO_DIR="$COVERAGE_DIR/jacoco"
BASE_URL="http://localhost:8080"

# ---------------------------------------------------------------------------
# Cleanup: stop Docker services on exit (success or failure)
# ---------------------------------------------------------------------------
cleanup() {
  echo ""
  echo "Stopping services..."
  docker compose \
    -f "$REPO_ROOT/docker-compose.yml" \
    -f "$REPO_ROOT/docker-compose.e2e-coverage.yml" \
    down --remove-orphans 2>/dev/null || true
}
trap cleanup EXIT

mkdir -p "$JACOCO_DIR" "$COVERAGE_DIR/java"
rm -f "$JACOCO_DIR/jacoco-e2e.exec"

# ---------------------------------------------------------------------------
# 1. Locate JaCoCo agent JAR
#    Maven downloads it as a transitive dependency of jacoco-maven-plugin.
#    If not yet in the local repo, fall back to an explicit download.
# ---------------------------------------------------------------------------
echo "Locating JaCoCo agent..."
JACOCO_AGENT=$(find "${HOME}/.m2/repository/org/jacoco/org.jacoco.agent" \
  -name "*-runtime.jar" 2>/dev/null | sort -V | tail -1 || true)

if [ -z "$JACOCO_AGENT" ]; then
  echo "  Not in local Maven repo — downloading JaCoCo 0.8.12 agent..."
  mvn -pl "$REPO_ROOT/modules/requel-app" -q \
    org.apache.maven.plugins:maven-dependency-plugin:3.6.0:copy \
    '-Dartifact=org.jacoco:org.jacoco.agent:0.8.12:jar:runtime' \
    "-DoutputDirectory=$JACOCO_DIR" \
    -Dmdep.stripVersion=true
  JACOCO_AGENT="$JACOCO_DIR/org.jacoco.agent-runtime.jar"
fi

cp "$JACOCO_AGENT" "$JACOCO_DIR/jacocoagent.jar"
echo "  Using: $JACOCO_AGENT"

# ---------------------------------------------------------------------------
# 2. Build a fresh Docker image with Angular source maps enabled
#
#    The coverage report needs source maps inside the packaged frontend assets
#    so monocart can map Chromium V8 coverage back to src/app/*. The normal
#    app package uses Angular's production build; for E2E coverage we rebuild
#    the app image with the development frontend configuration.
# ---------------------------------------------------------------------------
echo "Building app image with frontend source maps..."
mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests \
  -DangularBuildArguments="run build -- --configuration development" -q

# ---------------------------------------------------------------------------
# 3. Start Docker services with JaCoCo agent attached
# ---------------------------------------------------------------------------
echo "Starting services..."
docker compose \
  -f "$REPO_ROOT/docker-compose.yml" \
  -f "$REPO_ROOT/docker-compose.e2e-coverage.yml" \
  up -d

# ---------------------------------------------------------------------------
# 4. Wait for backend AND data initializer to complete
#
#    Why login, not wait-on / HTTP health:
#      Spring Boot fires ApplicationReadyEvent AFTER the HTTP server opens its
#      port. DatabaseInitializationRunner (@EventListener) runs in that event,
#      creating the admin and project seed users. A plain HTTP check returns
#      ready before those users exist; the first global-setup login would fail.
#
#    A successful POST /api/auth/login proves:
#      • Flyway migrations finished (HTTP server only starts post-Flyway)
#      • ApplicationReadyEvent handlers finished (users exist in DB)
#      • JaCoCo agent is accepting connections (started with the JVM)
# ---------------------------------------------------------------------------
echo -n "Waiting for backend and data initializer"
ATTEMPTS=0
MAX_ATTEMPTS=90  # 90 × 10 s = up to 15 min (app + NLP model load can take ~10 min)
until curl -sf -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' \
  -o /dev/null 2>/dev/null; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge "$MAX_ATTEMPTS" ]; then
    echo ""
    echo "ERROR: Backend did not become ready after $((MAX_ATTEMPTS * 10))s."
    echo "  Check logs: docker compose -f docker-compose.yml -f docker-compose.e2e-coverage.yml logs web"
    exit 1
  fi
  printf '.'
  sleep 10
done
echo " ready."

# ---------------------------------------------------------------------------
# 5. Run Playwright E2E suite with coverage config (monocart collects V8 data)
# ---------------------------------------------------------------------------
echo "Running E2E tests..."
cd "$REPO_ROOT/requel-angular"
npx playwright test --config=playwright.coverage.config.ts
cd "$REPO_ROOT"

# ---------------------------------------------------------------------------
# 6. Stop the web container gracefully so the JaCoCo agent writes the exec file
#
#    output=file mode: the agent writes coverage data via a JVM shutdown hook
#    when the JVM receives SIGTERM. docker stop sends SIGTERM first (30s grace
#    period) then SIGKILL — the JVM should flush coverage well within 30s.
#    The exec file lands at coverage/jacoco/jacoco-e2e.exec via the bind mount.
# ---------------------------------------------------------------------------
# ---------------------------------------------------------------------------
# Capture web container log before stopping — useful for diagnosing exceptions
# that appear during startup or test execution. Written to coverage/web.log.
# ---------------------------------------------------------------------------
echo "Saving web container log..."
docker compose \
  -f "$REPO_ROOT/docker-compose.yml" \
  -f "$REPO_ROOT/docker-compose.e2e-coverage.yml" \
  logs --no-log-prefix web > "$COVERAGE_DIR/web.log" 2>&1
echo "  Log written to $COVERAGE_DIR/web.log"

echo "Stopping web container to flush JaCoCo coverage..."
docker compose \
  -f "$REPO_ROOT/docker-compose.yml" \
  -f "$REPO_ROOT/docker-compose.e2e-coverage.yml" \
  stop --timeout 30 web

EXEC_FILE="$JACOCO_DIR/jacoco-e2e.exec"
if [ ! -f "$EXEC_FILE" ]; then
  echo "ERROR: JaCoCo exec file not found at $EXEC_FILE"
  echo "  The JVM may not have shut down cleanly. Check: docker compose logs web"
  exit 1
fi
echo "  Coverage written to $EXEC_FILE"

# ---------------------------------------------------------------------------
# 7. Generate Java coverage report (aggregate across all modules)
#
#    report-aggregate looks for target/jacoco.exec in each reactor module.
#    We have one exec file from the running JVM that contains coverage for all
#    modules; copying it to each module's target/ lets report-aggregate merge
#    them correctly. Compiled classes (target/classes) must already exist —
#    run a build first if you see "no class files found" warnings.
#
#    NOTE: report-aggregate's outputDirectory parameter has no -D property alias
#    in the JaCoCo Maven plugin, so we cannot override it on the command line.
#    The POM configures it to target/site/jacoco-aggregate; we copy from there.
# ---------------------------------------------------------------------------
echo "Generating Java coverage report..."
for module_dir in "$REPO_ROOT"/modules/*/; do
  target="${module_dir}target"
  if [ -d "$target" ]; then
    cp "$JACOCO_DIR/jacoco-e2e.exec" "$target/jacoco.exec"
  fi
done

mvn -pl modules/requel-app -am jacoco:report-aggregate -q

JACOCO_SITE="$REPO_ROOT/modules/requel-app/target/site/jacoco-aggregate"
if [ -d "$JACOCO_SITE" ]; then
  cp -r "$JACOCO_SITE/." "$COVERAGE_DIR/java/"
else
  echo "WARNING: Java report not found at $JACOCO_SITE — check for compilation errors above"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "Coverage reports:"
echo "  Java (aggregate):  $COVERAGE_DIR/java/index.html"
echo "  JavaScript:        $REPO_ROOT/requel-angular/coverage/index.html"
echo "  JS test report:    $REPO_ROOT/requel-angular/playwright-report/e2e-coverage.html"
echo "  JS lcov:           $REPO_ROOT/requel-angular/coverage/lcov.info"
echo "  Web server log:    $COVERAGE_DIR/web.log"
