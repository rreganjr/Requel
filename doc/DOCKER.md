# Docker Quick Start

This guide covers running Requel **1.1.0** (Java 17, Spring Boot 3 / Hibernate 6) with Docker using the published image on Docker Hub.

## Prerequisites
- Docker Engine 20.10+ (or Docker Desktop 4.x)
- Free ports (defaults shown below): `3307` for optional host DB access, `8181` for the web app

## 1. Create a Docker Network
```bash
docker network create requel-net || true
```
The shared network keeps the Requel container and MySQL container on the same bridge.

## 2. Launch MySQL 8.4
```bash
docker run --name requelDB --net=requel-net \
  -e MYSQL_ROOT_PASSWORD=pa33w0rd \
  -e MYSQL_DATABASE=requel \
  --restart unless-stopped \
  -d mysql:8.4
```
- Add `-p3307:3306` if you need to reach MySQL from the host.
- The database name `requel` matches the default JDBC URL shipped with the app.

## 3. Launch Requel 1.1.0
```bash
docker run --name requel --net=requel-net \
  -p8181:8080 \
  --restart unless-stopped \
  -d rreganjr/requel:1.1.0 \
  --spring.datasource.url=jdbc:mysql://requelDB:3306/requel?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC \
  --spring.datasource.username=root \
  --spring.datasource.password=pa33w0rd
```
- The container already runs on Java 17.
- Schema creation is automatic (`spring.jpa.hibernate.ddl-auto=update`); no separate bootstrap command is required.
- Adjust the `--spring.datasource.*` flags if you customised credentials in the MySQL step.

Once both containers are healthy, browse to http://localhost:8181/. If you are using Docker Toolbox, replace `localhost` with the VM IP (for example `http://192.168.99.100:8181/`).

## Optional: Compose File
Use a `docker-compose.yml` file to configure the database and server:
```yaml
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
    image: rreganjr/requel:1.1.0
    ports:
      - "8080:8080"
    networks:
      - requel-net
    environment:
      - "_JAVA_OPTIONS=-Xms2g -Xmx2g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"
      - "SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/requel?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
      - "SPRING_DATASOURCE_USERNAME=root"
      - "SPRING_DATASOURCE_PASSWORD=pa33w0rd"
    restart: unless-stopped

networks:
  requel-net:
    driver: bridge
```
Then run the docker command in the folder where you have the yml file.
```bash
docker compose up -d
```
Stop with `docker compose down` (add `--volumes` if you want to drop the MySQL data directory).

## First Login and Sample Project
1. Sign in with user `admin` / password `admin`.
2. Open the **Users** tab, click the `admin` account, check **ProjectUserRole** and **createProjects**, then save.
3. A **Projects** tab appears. Download the sample project (Requel.xml) from https://raw.githubusercontent.com/rreganjr/Requel/master/doc/samples/Requel.xml.
4. In **Projects**, choose **Import Project**, select the downloaded file, and upload. Leave **Enable analysis** checked if you want automated annotation.
5. After import, the Requel project should show up in the project tree.

## Known Limitations (1.1.0)
- File uploads for XSLT report generation and project import continue to use the legacy widget; depending on browser/runtime combos they may require a retry.
- Stanford Parser–based analysis components remain fragile on modern JDKs and may fail until upstream models are refreshed.

## Troubleshooting
- The first startup can take several minutes while Hibernate builds the schema. Tail the container log if the UI does not appear:
  ```bash
  docker logs -f requel
  ```
  Wait for a `Started Application` line before attempting to login.
- If the web container exits immediately, double-check the JDBC URL, username, and password match the MySQL container settings.
- Remove stale containers (including the MySQL volume) before re-running if you change credentials:
  ```bash
  docker rm -f requel requelDB
  docker volume prune
  ```
