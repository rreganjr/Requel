# Deployment Guide

## 1. Build and Test Locally

```bash
# ensure you are on the project root and using JDK 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

mvn -version   # should report Java 17
mvn clean verify
```

## 2. Package the Jar to Use in Docker

```bash
# still on the project root
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

mvn -DskipTests clean package
ls -lh target/Requel-1.1.0.jar
```

## 3. Build the Docker Image

```bash
# from the project root where Dockerfile lives
docker build --build-arg JAR_FILE=target/Requel-1.1.0.jar -t rreganjr/requel:1.1.0 .
```

## 4. Verify the Image Locally

```bash
# quick smoke test against local DB
docker run --rm -p8181:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3307/requel?createDatabaseIfNotExist=true" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=pa33w0rd \
  rreganjr/requel:1.1.0

# optional: copy the jar out of the image and inspect it
CONTAINER_ID=$(docker create rreganjr/requel:1.1.0)
docker cp "$CONTAINER_ID:/app.jar" ./requel-1.1.0-from-image.jar
docker rm "$CONTAINER_ID"
jar tf requel-1.1.0-from-image.jar | head
jar xf requel-1.1.0-from-image.jar BOOT-INF/classes/com/rreganjr/requel/Application.class
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

# push the new tag
docker push rreganjr/requel:1.1.0
```

## 7. Tag the Release in Git

```bash
git checkout master
git merge update-spring-boot-3
git tag v1.1.0
git push origin master --tags
```

## 8. Publish GitHub Release

1. Draft a release on GitHub using tag `v1.1.0`.
2. Attach `target/Requel-1.1.0.jar` built earlier.
3. Highlight key notes (Java 17 requirement, Spring Boot 3 upgrade, Docker tag).

## 9. Optional: Run One-off Schema Init Locally

```bash
# macOS
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Windows PowerShell
$env:JAVA_HOME="C:\\Program Files\\Java\\jdk-17"
$env:Path="${env:JAVA_HOME}\\bin;${env:Path}"
java -jar .\target\Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Windows Command Prompt
set JAVA_HOME=C:\\Program Files\\Java\\jdk-17
set PATH=%JAVA_HOME%\\bin;%PATH%
java -jar .\target\Requel-1.1.0.jar "--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" --spring.jpa.hibernate.ddl-auto=create --server.port=8081
```

## 10. Optional: Verify Docker Hub Deployment After Push

```bash
# pull & run the pushed image
Docker pull rreganjr/requel:1.1.0
docker run --rm -p8080:8080 rreganjr/requel:1.1.0
```
