#!/usr/bin/env bash
# #380: build the test fixture for an in-place upgrade from an old Requel release.
#
#   scripts/legacy-fixtures.sh <version> [sample-xml]
#
# version: 1.0.1 | 1.0.2 | 1.1.0. Starts the published rreganjr/requel:<version> image on an empty
# database (MySQL 5.7 for 1.0.x, which can't use 8.x; MySQL 8.4 for 1.1.0), lets it create its
# schema and load its dictionary, imports the sample project through its own UI
# (legacy-fixtures/import-sample.js), and dumps
#   - every table's schema,
#   - the rows of every non-dictionary table, categorydef included (WordNet's id-0 row is what
#     adding AUTO_INCREMENT trips over),
# into modules/requel-app/src/test/resources/upgrade/requel-<version>.sql. A 1.0.x dump is loaded
# into MySQL 8.4 and dumped again, which is the dump/load a 1.0.x user does to move to 8.4.
#
# Maintainer-only: needs Docker and Node with Playwright (NODE_PATH). The fixtures are committed,
# so CI never pulls the old images. Overrides:
#   SAMPLE        project XML to import (default: doc/samples/Requel.xml, see below)
#   RUN_JAR_WITH  a Java image to run the release's /app.jar in, instead of the image itself
#                 (1.1.0 is published for arm64 only; on amd64 use eclipse-temurin:17-jre)
#   MYSQL57 / MYSQL84   images (default mysql:5.7 / mysql:8.4)
set -euo pipefail

VERSION="${1:?usage: legacy-fixtures.sh <1.0.1|1.0.2|1.1.0> [sample-xml]}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/modules/requel-app/src/test/resources/upgrade"
case "$VERSION" in 1.0.1|1.0.2|1.1.0) ;; *) echo "unknown version $VERSION" >&2; exit 2 ;; esac
MYSQL57="${MYSQL57:-mysql:5.7}"
MYSQL84="${MYSQL84:-mysql:8.4}"
NET="requel-legacy-$$"
DB_OLD="requel-legacy-db-$$"
DB_84="requel-legacy-84-$$"
APP="requel-legacy-app-$$"
PORT="${PORT:-18080}"
WORK="$(mktemp -d)"
# The sample is doc/samples/Requel.xml without its glossary: neither 1.0.x nor 1.1.0 can import it
# (GlossaryTermImpl.compareTo NPEs on a term whose project isn't set yet). The upgrade IT's
# comparison import drops it too.
SAMPLE="${2:-${SAMPLE:-}}"
if [[ -z "$SAMPLE" ]]; then
  SAMPLE="$WORK/sample.xml"
  python3 -c 'import re,sys; s=open(sys.argv[1]).read(); open(sys.argv[2],"w").write(re.sub(r"<glossary>.*?</glossary>","<glossary/>",s,flags=re.S))' \
    "$ROOT/doc/samples/Requel.xml" "$SAMPLE"
fi

cleanup() {
  docker rm -f "$APP" "$DB_OLD" "$DB_84" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

# Dictionary tables: schema only (a real install has ~1.8M rows in them). categorydef keeps its rows.
DICTIONARY_TABLES="lexlinkref linkdef morphdef morphref semcor_file semcor_sentence semcor_sentence_word
  semlinkref sense synset synset_definition_word synset_subsumer_counts vnclass vnframedef vnframeref
  vnroleref vnroleselres vnroletype vnselres word"

wait_mysql() { until docker exec "$1" mysqladmin -ppassword ping >/dev/null 2>&1; do sleep 2; done; sleep 5; }

docker network create "$NET" >/dev/null
docker run -d --name "$DB_84" --network "$NET" -e MYSQL_ROOT_PASSWORD=password "$MYSQL84" >/dev/null
case "$VERSION" in
  1.0.*) docker run -d --name "$DB_OLD" --network "$NET" -e MYSQL_ROOT_PASSWORD=password "$MYSQL57" >/dev/null
         APP_DB="$DB_OLD"; wait_mysql "$DB_OLD" ;;
  *)     APP_DB="$DB_84" ;;
esac
wait_mysql "$DB_84"

JDBC="jdbc:mysql://$APP_DB:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
if [[ -n "${RUN_JAR_WITH:-}" ]]; then
  cid=$(docker create --platform linux/arm64 "rreganjr/requel:$VERSION" 2>/dev/null || docker create "rreganjr/requel:$VERSION")
  docker cp "$cid:/app.jar" "$WORK/app.jar"; docker rm "$cid" >/dev/null
  docker run -d --name "$APP" --network "$NET" -p "$PORT:8080" -v "$WORK:/app" "$RUN_JAR_WITH" \
    java -jar /app/app.jar "--spring.datasource.url=$JDBC" >/dev/null
else
  docker run -d --name "$APP" --network "$NET" -p "$PORT:8080" "rreganjr/requel:$VERSION" \
    "--spring.datasource.url=$JDBC" >/dev/null
fi
echo "waiting for Requel $VERSION (it loads its dictionary first; a few minutes)..."
until docker logs "$APP" 2>&1 | grep -q "Started Application"; do
  if ! docker ps -q -f "name=$APP" | grep -q .; then docker logs "$APP" | tail -40; exit 1; fi
  sleep 10
done

node "$ROOT/scripts/legacy-fixtures/import-sample.js" "http://localhost:$PORT/" "$SAMPLE"
sleep 10   # let the import's transaction and any background work finish
projects=$(docker exec "$APP_DB" mysql -ppassword -N -e "SELECT COUNT(*) FROM requel.pods" 2>/dev/null)
if [[ "$projects" != "1" ]]; then
  echo "the sample project wasn't imported ($projects projects); the old app said:" >&2
  docker logs "$APP" 2>&1 | grep -A12 -E "ERROR|Exception" | tail -60 >&2
  exit 1
fi
docker stop "$APP" >/dev/null

ignore=""; for t in $DICTIONARY_TABLES; do ignore="$ignore --ignore-table=requel.$t"; done
dump() { local c="$1"; shift; docker exec "$c" mysqldump -ppassword --skip-comments --skip-dump-date --single-transaction "$@"; }
if [[ "$APP_DB" == "$DB_OLD" ]]; then
  # The 5.7 -> 8.4 move a 1.0.x user makes: dump everything relevant, load it into 8.4.
  { dump "$DB_OLD" --no-data requel; dump "$DB_OLD" --no-create-info $ignore requel; } 2>/dev/null > "$WORK/old.sql"
  docker exec "$DB_84" mysql -ppassword -e "CREATE DATABASE requel" 2>/dev/null
  docker exec -i "$DB_84" mysql -ppassword requel < "$WORK/old.sql" 2>/dev/null
fi

OUT="$OUT_DIR/requel-$VERSION.sql"
{
  echo "-- #380 fixture: a Requel $VERSION database (schema, the sample project, no dictionary rows)."
  echo "-- Made by scripts/legacy-fixtures.sh; regenerate rather than edit."
  dump "$DB_84" --no-data requel
  dump "$DB_84" --no-create-info --skip-extended-insert $ignore requel
} 2>/dev/null | sed -E 's/ AUTO_INCREMENT=[0-9]+//' > "$OUT"
echo "wrote $OUT ($(wc -c < "$OUT") bytes)"
