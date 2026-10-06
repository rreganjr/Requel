## Requel 2.0

[![CI](https://github.com/rreganjr/Requel/actions/workflows/ci.yml/badge.svg)](https://github.com/rreganjr/Requel/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/rreganjr/Requel/branch/master/graph/badge.svg)](https://codecov.io/gh/rreganjr/Requel)
[![License](https://img.shields.io/github/license/rreganjr/Requel)](LICENSE)
[![Release](https://img.shields.io/github/v/release/rreganjr/Requel)](https://github.com/rreganjr/Requel/releases)
[![Last commit](https://img.shields.io/github/last-commit/rreganjr/Requel)](https://github.com/rreganjr/Requel/commits/master)
[![Docker Pulls](https://img.shields.io/docker/pulls/rreganjr/requel)](https://hub.docker.com/r/rreganjr/requel)
[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular)](https://angular.dev/)

Requel is a web-based requirements management system that supports collaboration among all
stakeholders and provides automated assistance to validate requirements and suggest improvements.
It models requirements as goals, stories, actors, scenarios, and use-cases with an IBIS-style
annotation and discussion layer for negotiating issues and tracking decisions.

For background on what requirements engineering is and why it matters, see the
[Thesis Document](https://github.com/rreganjr/Requel/raw/master/doc/archive/2009-thesis/ThesisFinalColor.pdf)
(Harvard ALM, 2009). The [User Guide](https://github.com/rreganjr/Requel/raw/master/doc/archive/2009-thesis/UserGuide.pdf)
covers the core concepts; note that Chapter 5 (_Requel Setup_) describes the old WAR deployment
and is no longer relevant.

An example project file that can be imported:
[Requel.xml](https://raw.githubusercontent.com/rreganjr/Requel/master/doc/samples/Requel.xml)

---

### What's new in 2.0 (2026)

- **A new web UI.** The Echo2 server-side UI is replaced by an Angular single-page app, bundled
  into the same jar, with live refresh when other people or the assistants change something.
- **An API.** Everything the UI does goes through `POST /api/commands/{type}` and `GET /api/...`,
  with personal API tokens for scripts.
- **AI requirements review.** Assistants review goals, stories, use cases and scenarios and
  raise issues you can discuss and resolve. Use OpenAI, Anthropic, Gemini or a local model
  (Ollama); projects can write their own review definitions. See
  [AI assistant setup](doc/guides/AI_ASSISTANT_SETUP.md).
- **MCP server and `requel-cli`.** AI clients (Claude, VS Code and others) can read and edit a
  project over MCP with OAuth sign-in; the CLI does the same from a terminal.
- **Stakeholder permissions that hang together.** Permissions granted together are shown
  together, deletes that remove owned items are explained, and granting needs Grant.
- **Also:** tags, report generators, project delete, links from entities to their sources
  (tickets, documents), actors on stories, and an audit log of every command.

---

### Running Requel

The quickest way is Docker Compose:

```bash
curl -fsSLO https://raw.githubusercontent.com/rreganjr/Requel/v2.0.0/docker-compose.yml
echo "REQUEL_JWT_SECRET=$(openssl rand -base64 48)" > .env
docker compose up -d
```

The first start takes a few minutes while the dictionary loads. Then open
http://localhost:8080/, log in as **admin** / **admin** and change the password.

[Installing and running Requel](doc/guides/INSTALL.md) covers Compose, plain Docker and the
jar, upgrading from 1.x, every setting, and what to check before anyone else can reach it.
Docker images: https://hub.docker.com/r/rreganjr/requel/

---

### Building from source

Requires **Java 17**, **Maven 3.6.3+** and **Node 22+**. The first build downloads the NLP data
jar from this repository's GitHub Releases, so it needs to reach github.com (see
[NLP data](doc/guides/NLP_DATA.md)).

```bash
# Full build (Java + Angular)
mvn -pl modules/requel-app -am package -DskipTests

# Java only (fast iteration, skips Angular)
mvn -pl modules/requel-app -am package -DskipAngularBuild=true -DskipTests=true

# Build Docker image
mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests
```

To run a development build with the Angular dev server and the reset endpoints the e2e tests
use, add `--spring.profiles.active=dev`
([`application-dev.properties`](modules/requel-app/src/main/resources/application-dev.properties)).
Never run a reachable server with `dev`: its reset endpoints are unauthenticated.

---

### Database migrations

Flyway applies the migrations in
[`modules/requel-app/src/main/resources/db/migration`](modules/requel-app/src/main/resources/db/migration)
on startup. A fresh database needs nothing extra. A 1.2 database is migrated in place; a 1.0 or
1.1 database is refused, and its projects move by XML export and import
([Upgrading](doc/guides/INSTALL.md#upgrading)).

---

### Version history

#### 2.0 (2026) — Angular SPA, CQRS API

Complete replacement of the Echo2 server-side UI with an Angular SPA backed by a CQRS API,
plus AI requirements review, an MCP server and `requel-cli`. The Angular build is bundled into
the JAR by Maven so the deployment model is unchanged: one JAR, one database.

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
