# Installing and running Requel 2.0

Requel is one Spring Boot application with a MySQL database. You can run it three ways:

- **Docker Compose** (recommended): Requel and MySQL together, one command.
- **Docker** without Compose: the same two containers, started by hand.
- **The jar**: Requel on Java, pointed at a MySQL you already run.

Whichever you pick, read [Before you expose it](#before-you-expose-it) before anyone else can
reach the server.

## Requirements

- Docker with Compose v2, **or** Java 17 and MySQL 8.4.
- About 3 GB of memory for Requel (the compose file gives the JVM a 2 GB heap) plus MySQL.
- The first start on an empty database loads the dictionary, which takes a few minutes. The
  web port opens when it is done.

## Docker Compose

1. Download the compose file for the release into an empty directory:

   ```bash
   mkdir requel && cd requel
   curl -fsSLO https://raw.githubusercontent.com/rreganjr/Requel/v2.0.0/docker-compose.yml
   ```

2. Create the secret Requel signs login tokens with. Keep this file: a new secret logs
   everyone out.

   ```bash
   echo "REQUEL_JWT_SECRET=$(openssl rand -base64 48)" > .env
   ```

3. Start it and wait for the first start to finish:

   ```bash
   docker compose up -d
   docker compose logs -f web     # Ctrl-C once "Started Application" appears
   ```

4. Open http://localhost:8080/ and log in as **admin** with password **admin**. Change the
   password straight away (the account menu, **Edit Account**).

MySQL is published on host port 3307 if you need to reach it directly. `docker compose down`
stops both containers and keeps the data; `docker compose down --volumes` deletes it.

To try the AI review with a local model and no API key, use `docker-compose.local-ai.yml` the
same way; see [AI assistant setup](AI_ASSISTANT_SETUP.md#4-run-a-local-ai-model-in-docker-no-api-key).

## Docker without Compose

```bash
docker network create requel-net

docker run -d --name requel-db --network requel-net \
  -e MYSQL_ROOT_PASSWORD=change-me -e MYSQL_DATABASE=requel \
  -v requel-db:/var/lib/mysql mysql:8.4

docker run -d --name requel --network requel-net -p 8080:8080 --restart unless-stopped \
  -e "SPRING_DATASOURCE_URL=jdbc:mysql://requel-db:3306/requel?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=change-me \
  -e "REQUEL_JWT_SECRET=$(openssl rand -base64 48)" \
  -e "_JAVA_OPTIONS=-Xmx2g" \
  rreganjr/requel:2.0.0
```

MySQL takes a few seconds to accept connections on first start. If Requel starts before it does,
it exits ("Communications link failure"), and `--restart unless-stopped` brings it back until
the database is ready.

Generate the secret once and reuse it when you recreate the container. Then follow steps 3 and
4 above (`docker logs -f requel`).

## The jar

1. Download `requel-app-2.0.0.jar` from the
   [2.0.0 release](https://github.com/rreganjr/Requel/releases/tag/v2.0.0).
2. Create an empty database and a user for it in your MySQL 8.4:

   ```sql
   CREATE DATABASE requel CHARACTER SET utf8mb4;
   CREATE USER 'requel'@'%' IDENTIFIED BY 'change-me';
   GRANT ALL ON requel.* TO 'requel'@'%';
   ```

3. Run it (macOS and Linux; on Windows set the same variables with `set` or `$env:`):

   ```bash
   export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/requel?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC'
   export SPRING_DATASOURCE_USERNAME=requel
   export SPRING_DATASOURCE_PASSWORD=change-me
   export REQUEL_JWT_SECRET='<the output of: openssl rand -base64 48>'
   java -Xmx2g -jar requel-app-2.0.0.jar
   ```

4. Wait for `Started Application`, then log in as in step 4 of the Compose section.

Every setting can also be passed as a `--name=value` argument (`--server.port=8081`). Don't
add `--spring.profiles.active=dev`: it is for developing Requel and opens unauthenticated
reset endpoints.

## Upgrading

**Back up the database first** (`mysqldump --single-transaction requel > requel-backup.sql`).

- **From 2.0.x:** replace the image or jar and start it. Flyway applies any new migrations.
- **From 1.2:** start 2.0 on the same database (MySQL 8.4). 1.2 already has Flyway history,
  so 2.0 migrates it in place on first start; logins and projects carry over.
- **From 1.0 or 1.1:** these databases have no Flyway history, and 2.0 refuses to start on
  one (it leaves it untouched). Move the projects instead:
  1. In the old version, open each project and use **Export** to save its XML.
  2. Start 2.0 on a new, empty database as above.
  3. Import each file (**Projects → Import**). The project keeps its name. Users named in the
     project (stakeholders, authors) are created from the file; create any other accounts
     again.

  Requel 1.0.x runs on MySQL 5.x only, so the new database is also your move to MySQL 8.4.
  An in-place upgrade is [#380](https://github.com/rreganjr/Requel/issues/380).

## Configuration reference

Set these as environment variables (shown) or as `--property=value` arguments.

| Setting | Default | What it is |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/requel?...` | JDBC URL of the database |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `root` / `password` | Database login |
| `REQUEL_JWT_SECRET` | none: required | Signs login tokens; at least 32 characters; keep it stable |
| `SERVER_PORT` | `8080` | HTTP port |
| `_JAVA_OPTIONS` | JVM default | Memory, e.g. `-Xmx2g` |
| `REQUEL_AI_*`, `SPRING_PROFILES_ACTIVE=ai-…` | assistant off | AI review provider; see [AI assistant setup](AI_ASSISTANT_SETUP.md) |
| `REQUEL_OAUTH_ISSUER` | derived per request | Public URL when Requel is behind a proxy, for MCP clients; see [MCP remote connection](mcp_remote_connection.md) |

## Before you expose it

- `REQUEL_JWT_SECRET` is set to a random value and kept somewhere safe. Requel refuses to start
  without one (except with the `dev` profile).
- The **admin** password is no longer `admin`.
- The `dev` profile is **not** active.
- Requel sits behind HTTPS (a reverse proxy such as nginx or Caddy terminating TLS).
- The database is backed up on a schedule.

## Troubleshooting

- **Nothing answers on port 8080 for a few minutes after the first start.** The dictionary is
  loading. Watch the log for `Database initialization complete.`
- **`REQUEL_JWT_SECRET is not set` or `... needs at least 32` at startup.** Set the secret as
  above. With Compose, check `.env` is next to `docker-compose.yml`.
- **`Database ... has N tables but no Flyway history`.** It is a Requel 1.0/1.1 database (or
  not a Requel database). See [Upgrading](#upgrading).
- **`Communications link failure`.** Requel can't reach MySQL: check the URL's host and port,
  and that the database container is healthy (`docker compose ps`).
- **Port already in use.** Change the published port (`"8081:8080"` in the compose file) or
  `SERVER_PORT` for the jar.
