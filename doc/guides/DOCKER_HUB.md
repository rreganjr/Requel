# Requel

Web-based requirements management: goals, stories, actors, scenarios and use cases, with an
issue/position/argument discussion layer and automated NLP and AI review.

- Source and full docs: https://github.com/rreganjr/Requel
- Install guide (Compose, Docker, jar, upgrading, settings): https://github.com/rreganjr/Requel/blob/v2.0.2/doc/guides/INSTALL.md

## Tags

| Tag | What it is |
|---|---|
| `latest`, `2.0.2` | Current release. Multi-platform: `linux/amd64` and `linux/arm64` |
| `2.0.1`, `2.0.0` | Previous 2.0 releases |
| `1.2.0`, `1.1.0` | 1.x line (arm64 only); see Upgrading before moving off them |
| `1.0.2`, `1.0.1` | Original release; MySQL 5.x only |

Pin a version tag rather than `latest` so an upgrade happens when you choose.

## Requirements

- Docker with Compose v2.
- About 3 GB of memory for Requel (the compose file gives the JVM a 2 GB heap) plus MySQL 8.4.
- The first start on an empty database loads the dictionary, which takes a few minutes. The
  web port opens when it is done.

## Quick start with Docker Compose

```bash
mkdir requel && cd requel
curl -fsSLO https://raw.githubusercontent.com/rreganjr/Requel/v2.0.2/docker-compose.yml
echo "REQUEL_JWT_SECRET=$(openssl rand -base64 48)" > .env
docker compose up -d
docker compose logs -f web     # Ctrl-C once "Started Application" appears
```

Open http://localhost:8080/ and log in as **admin** / **admin**. Requel sends you straight to
**Change your password**; nothing else works for that account until you have.

`REQUEL_JWT_SECRET` signs login tokens and is required. Keep the `.env` file: a new secret logs
everyone out. MySQL is published on host port 3307. `docker compose down` stops both containers
and keeps the data; `docker compose down --volumes` deletes it.

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
  rreganjr/requel:2.0.2

docker logs -f requel          # Ctrl-C once "Started Application" appears
```

If Requel starts before MySQL accepts connections it exits ("Communications link failure"), and
`--restart unless-stopped` brings it back until the database is ready. Generate the secret once
and reuse it when you recreate the container.

## Try the sample project

Download [Requel.xml](https://raw.githubusercontent.com/rreganjr/Requel/v2.0.2/doc/samples/Requel.xml)
and import it from **Projects → Import**.

## Settings

| Environment variable | Default | What it is |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/requel?...` | JDBC URL of the database |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `root` / `password` | Database login |
| `REQUEL_JWT_SECRET` | none: required | Signs login tokens; at least 32 characters; keep it stable |
| `SERVER_PORT` | `8080` | HTTP port inside the container |
| `_JAVA_OPTIONS` | JVM default | Memory, e.g. `-Xmx2g` |
| `REQUEL_AI_*`, `SPRING_PROFILES_ACTIVE=ai-…` | assistant off | AI review provider; see [AI assistant setup](https://github.com/rreganjr/Requel/blob/v2.0.2/doc/guides/AI_ASSISTANT_SETUP.md) |

The full list, including the OAuth settings for MCP clients and `requel-cli`, is in the
[install guide](https://github.com/rreganjr/Requel/blob/v2.0.2/doc/guides/INSTALL.md#configuration-reference).
Don't set `SPRING_PROFILES_ACTIVE=dev`: it is for developing Requel and opens unauthenticated
reset endpoints.

## Upgrading

Back up the database first (`mysqldump --single-transaction requel > requel-backup.sql`).

- **From 2.0.x:** change the image tag and recreate the container. Flyway applies any new migrations.
- **From 1.2:** start 2.0 on the same MySQL 8.4 database; it migrates in place on first start.
- **From 1.0 or 1.1:** 2.0 refuses to start on these databases (and leaves them untouched).
  Export each project's XML from the old version, start 2.0 on a new empty database, and import
  them. Details in the [install guide](https://github.com/rreganjr/Requel/blob/v2.0.2/doc/guides/INSTALL.md#upgrading).

## Before you expose it

- `REQUEL_JWT_SECRET` is a random value kept somewhere safe.
- The **admin** password is no longer `admin` (Requel makes you change it at the first sign-in),
  and the database password is not `change-me`.
- The `dev` profile is not active.
- Requel sits behind HTTPS (a reverse proxy such as nginx or Caddy terminating TLS).
- The database is backed up on a schedule.

## Troubleshooting

- **Nothing answers on port 8080 for a few minutes after the first start.** The dictionary is
  loading. Watch the log for `Database initialization complete.`
- **`REQUEL_JWT_SECRET is not set` or `... needs at least 32` at startup.** Set the secret as
  above. With Compose, check `.env` is next to `docker-compose.yml`.
- **`Database ... has N tables but no Flyway history`.** It is a Requel 1.0/1.1 database. See
  Upgrading.
- **`Communications link failure`.** Requel can't reach MySQL: check the URL's host and port,
  and that the database container is healthy (`docker compose ps`).
- **Port already in use.** Publish a different host port (`-p 8081:8080`, or `"8081:8080"` in
  the compose file).
