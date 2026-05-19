## Requel 2.0

Requel is a web-based requirements management system that supports collaboration among all
stakeholders and provides automated assistance to validate requirements and suggest improvements.
It models requirements as goals, stories, actors, scenarios, and use-cases with an IBIS-style
annotation and discussion layer for negotiating issues and tracking decisions.

For background on what requirements engineering is and why it matters, see the
[Thesis Document](https://github.com/rreganjr/Requel/raw/master/doc/ThesisFinalColor.pdf)
(Harvard ALM, 2009). The [User Guide](https://github.com/rreganjr/Requel/raw/master/doc/UserGuide.pdf)
covers the core concepts; note that Chapter 5 (_Requel Setup_) describes the old WAR deployment
and is no longer relevant.

An example project file that can be imported:
[Requel.xml](https://raw.githubusercontent.com/rreganjr/Requel/v1.0.1-beta/doc/samples/Requel.xml)

---

### What's new in 2.0 (2026)

Version 2.0 replaces the Echo2 server-side Java UI with a modern Angular 17+ single-page
application. The Angular SPA is built as part of the Maven build and bundled directly into the
Spring Boot JAR, so there is no separate web server to run or configure.

Key changes from 1.2:

- **Angular SPA** — full rewrite of the UI in Angular 17 with PrimeNG components. All
  requirements editing screens, the IBIS annotation/discussion layer, and the project sidebar
  are now client-side with SSE-based live refresh when background NLP analysis updates entities.
- **CQRS API** — a clean REST API backs the SPA: `POST /api/commands/{type}` for writes,
  `GET /api/...` for reads. The same API is available for integration or scripting.
- **Command audit log** — every API-dispatched command is recorded in `command_audit_log`
  (user, timestamp, command type, project). Background NLP commands are excluded.
- **Actors on stories** — stories now support a primary actor and a set of additional actors,
  consistent with use-cases.
- **JWT authentication** — the Angular client uses JWT tokens; sessions are enforced per-user
  on the SSE stream.

---

### Quickstart with Docker Compose (recommended)

The easiest way to run Requel is with the included `docker-compose.yml`, which starts MySQL 8.4
and the Requel server together:

```bash
docker-compose up
```

Then open http://localhost:8080/ and log in as **admin** with password **admin**.

MySQL is available on host port 3307 if you need direct access.

To stop and remove containers:

```bash
docker-compose down
```

---

### Running the JAR directly

Requires **Java 17** and a running **MySQL 8.4** instance.

```bash
# macOS
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
java -jar modules/requel-app/target/requel-app-2.0.0.jar \
  --spring.profiles.active=dev \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root \
  --spring.datasource.password=password \
  --server.port=8080

# Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -jar modules/requel-app/target/requel-app-2.0.0.jar \
  --spring.profiles.active=dev \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root \
  --spring.datasource.password=password \
  --server.port=8080
```

Then open http://localhost:8080/ and log in as **admin** / **admin**.

`--spring.profiles.active=dev` activates [`application-dev.properties`](modules/requel-app/src/main/resources/application-dev.properties) which:

- allows the Angular dev server on `:4200` to reach the API (CORS),
- registers `/api/dev/reset-admin` and `/api/dev/reset-project` endpoints used by the Playwright E2E global-setup to put built-in users back to canonical state before each run.

The reset endpoints are unauthenticated and destructive, so leaving the `dev` profile off (the default, what Docker Compose runs with) keeps them out of the running server.

> **zsh users:** quote the JDBC URL (contains `?`) or prefix the command with `noglob`.

---

### Running with Docker (manual)

```bash
docker network create requel-net || true

# MySQL 8.4
docker run --name requelDB --net=requel-net -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=pa33w0rd -d mysql:8.4

# Requel 2.0.0
docker run --name requel --net=requel-net -p 8080:8080 -d \
  rreganjr/requel:2.0.0 \
  --spring.datasource.url=jdbc:mysql://requelDB:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
  --spring.datasource.username=root \
  --spring.datasource.password=pa33w0rd
```

Then open http://localhost:8080/

Docker images: https://hub.docker.com/r/rreganjr/requel/

---

### Building from source

Requires **Java 17**, **Maven 3.6.3+**, and **Node 22+**.

```bash
# Full build (Java + Angular)
mvn -pl modules/requel-app -am package -DskipTests

# Java only (fast iteration, skips Angular)
mvn -pl modules/requel-app -am package -DskipAngularBuild=true -DskipTests=true

# Build Docker image
mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests
```

---

### Database initialization and upgrades

On a **fresh database** nothing extra is needed. Flyway runs all migrations automatically on
startup.

On an **existing pre-2.0 database** (no `flyway_schema_history` table), start once with:

```
-e SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
-e SPRING_FLYWAY_BASELINE_VERSION=1
```

This registers the existing schema as version 1 and applies newer migrations. Remove these
flags after the first successful start.

#### Migrations

| Version | Description |
|---------|-------------|
| V1 | Initial schema (projects, goals, actors, stories, use-cases, scenarios, annotations) |
| V2 | Identity cleanup — drop legacy `*_seq` tables, set PKs to `AUTO_INCREMENT` |
| V3 | User preferences (sidebar project limit and staleness filter) |
| V4 | Command audit log table |
| V5 | Use-case additional scenarios join table |
| V6 | Fix `proposed_word` column spelling in dictionary |
| V7 | Story primary actor (`primary_actor_id` column on `stories` table) |

---

### Version history

#### 2.0 (2026) — Angular SPA, CQRS API

Complete replacement of the Echo2 server-side UI with an Angular 17 SPA. The server-side
rendering model is gone; the backend now exposes a clean CQRS API. The Angular build is
bundled into the JAR by Maven so the deployment model is unchanged — one JAR, one database.

#### 1.2 (2025) — Java 17, Spring Boot 3

Modernized the original 2009 codebase to run on a current Java stack without changing the
application behavior or UI. Migrated from Java 8 / Spring 4 / Hibernate 4 / Tomcat WAR to
Java 17 / Spring Boot 3.3 / Hibernate 6 / embedded Tomcat JAR. Introduced Flyway for schema
management. The Echo2 UI was retained unchanged.

#### 1.0 (2009) — Original Harvard ALM thesis release

Requel was developed as a Harvard Extension School ALM thesis project. It ran as a Java EE WAR
on Tomcat with an Echo2 Ajax UI, MySQL for persistence, and Stanford CoreNLP / OpenNLP for the
automated requirements analysis features (glossary term extraction, ambiguity detection). The
thesis document and user guide from this release are still included in `doc/`.
