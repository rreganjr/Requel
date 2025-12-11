# Release Guide

## 1. Build and Test Locally

```bash
# ensure you are on the project root and using JDK 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

mvn -version   # should report Java 17
mvn clean verify
```

> Note: Flyway runs before Hibernate; the DB must be clean or repaired. Hibernate DDL is disabled by default (`spring.jpa.hibernate.ddl-auto=none`)—Flyway owns schema creation.
> Flyway migrations now consist of a clean `V1__init.sql` (AUTO_INCREMENT PKs, no `*_seq` tables) and `V2__identity_cleanup.sql` (drops legacy sequences, enforces AUTO_INCREMENT, annotation discriminator fixes).

## 1.1 Update The POM Versions as needed
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -q versions:set -DnewVersion=1.2.0 -DgenerateBackupPoms=false
```
- update the jar versions in this file to the new version

## 1.2 Update The README.md
- add any new setup notes
  - update the versions of jars to the latest version
- describe new features

## 2. Package the Jar to Use in Docker

```bash
# still on the project root
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

mvn -DskipTests clean package -DskipEchoTransform=true
ls -lh modules/requel-app/target/requel-app-1.2.0.jar
```

## 3. Build the Docker Image

**important: update the jar file name below to the current version being deployed before building**
```bash
# from the project root where Dockerfile lives
# this creates a tag for both the version and "latest"
docker build --build-arg JAR_FILE=modules/requel-app/target/requel-app-1.2.0.jar -t rreganjr/requel:1.2.0 -t rreganjr/requel:latest .
```

## 4. Verify the Image Locally

```bash
# quick smoke test against local DB
docker run --rm -p8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/requel?createDatabaseIfNotExist=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  rreganjr/requel:1.2.0

# optional: copy the jar out of the image and inspect it
CONTAINER_ID=$(docker create rreganjr/requel:1.2.0)
docker cp "$CONTAINER_ID:/app.jar" ./requel-1.2.0-from-image.jar
docker rm "$CONTAINER_ID"
jar tf requel-1.2.0-from-image.jar | head
jar xf requel-1.2.0-from-image.jar BOOT-INF/classes/com/rreganjr/requel/Application.class
javap -v BOOT-INF/classes/com/rreganjr/requel/Application.class | grep 'major version'
rm -r BOOT-INF
```

## 5. Run the Stack with docker-compose

```bash
# from the project root
docker compose up -d --build
# verify the app at http://localhost:8080 (or the mapped port)
# MySQL is available on host port 3307
```

## 6. Push the Image to Docker Hub

```bash
# login with publisher credentials
docker logout
docker login --username rreganjr

# push the new tags
docker push rreganjr/requel:1.2.0
docker push rreganjr/requel:latest
```

## 7. Optional: Verify Docker Hub Deployment After Push

Clear out local docker images for requel (optional, `docker rmi rreganjr/requel:latest rreganjr/requel:1.2.0`).

Create a new yml file outside the repo, like in /tmp or Desktop, using the `latest` tag so you know the pull comes from Docker Hub:
```yml
services:
  db:
    image: mysql:8.4
    ports:
      - "3307:3306"
    networks:
      - requel-net
    environment:
      - "MYSQL_ROOT_PASSWORD=pa33w0rd"
      - "MYSQL_DATABASE=requel"
      - "MYSQL_ROOT_HOST=%"
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p\"$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s
    restart: unless-stopped

  web:
    depends_on:
      db:
        condition: service_healthy
    image: rreganjr/requel:latest
    pull_policy: always
    ports:
      - "8080:8080"
    networks:
      - requel-net
    environment:
      - "_JAVA_OPTIONS=-Xms2g -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
      - "SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/requel?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
      - "SPRING_DATASOURCE_USERNAME=root"
      - "SPRING_DATASOURCE_PASSWORD=pa33w0rd"
      # Uncomment the next two lines ONLY for the first start against an existing pre-Flyway database:
      # - "SPRING_FLYWAY_BASELINE_ON_MIGRATE=true"
      # - "SPRING_FLYWAY_BASELINE_VERSION=1"
    restart: unless-stopped

networks:
  requel-net:
    driver: bridge
```

```bash
# run the compose file
docker compose up
```

## 8. Tag the Release in Git

```bash
# Set a temporary repo-local identity so the tag is authored as rreganjr
git config user.name "rreganjr"
git config user.email "11188410+rreganjr@users.noreply.github.com"

git checkout master
git merge update-spring-boot-3
git tag v1.2.0
git push origin master --tags

# Optional: remove the temporary identity
git config --unset user.name
git config --unset user.email
```

## 9. Publish GitHub Release

1. Draft a release on GitHub using tag `v1.2.0`.
2. Attach `target/Requel-1.2.0.jar` built earlier.
3. Highlight key notes (Java 17 requirement, Spring Boot 3 upgrade, Docker tag).
