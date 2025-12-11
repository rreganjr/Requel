## Requel

Requel is a Web-based requirements management system that supports collaboration among all stakeholders and provides (_very limited_) automated assistance to validate requirements and suggest improvements. It supports requirements as goals, stories, and use-cases.

See the [User Guide](https://github.com/rreganjr/Requel/raw/master/doc/UserGuide.pdf) for more details on using Requel. **NOTE** Chapter 5, _Requel Setup_ is no longer relevant with the executable Jar file.

For more information about the motivation for this project see the [Thesis Document](https://github.com/rreganjr/Requel/raw/master/doc/ThesisFinalColor.pdf).

See the example [Requel.xml](https://raw.githubusercontent.com/rreganjr/Requel/v1.0.1-beta/doc/samples/Requel.xml) project file  that can be imported.

### New Executable Jar for Easy Running

This release replicates the functionality of the original release from 2009, but as an executable jar file that is easier to configure and run. It targets **Java 17** and ships with Spring Boot 3 / Hibernate 6. Make sure your `JAVA_HOME` points to a JDK 17 install before launching. The app embeds Tomcat, so you only need to have a MySQL database running. Pass database settings and a port to listen on if 8080 is not available:

Pass in the database setting parameters like this:

* **jdbc url** --spring.datasource.url=\<url\>
* **username** --spring.datasource.username=\<username\>
* **password** --spring.datasource.password=\<password\>

If you already have an existing Requel database (created before Flyway was introduced), run once with these Flyway baseline flags so the schema is registered and the identity fixes apply:

* `-e SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`
* `-e SPRING_FLYWAY_BASELINE_VERSION=1`

Remove these after the first successful start; subsequent runs should omit them.
 
Optionally pass the service port like this:

* **port** --server.port=\<portnumber\>

### Example command

Note for zsh users: quote the JDBC URL (because of the `?`) or prefix the command with `noglob`. Ensure the command runs on Java 17 (set `JAVA_HOME` or use a 17-enabled shell) before launching the jar.

```bash
# macOS (uses /usr/libexec/java_home)
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
java -jar ./target/Requel-1.2.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Linux (set JAVA_HOME manually)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -jar ./target/Requel-1.2.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Windows PowerShell
$env:JAVA_HOME="C:\\Program Files\\Java\\jdk-17"
$env:Path="${env:JAVA_HOME}\\bin;${env:Path}"
java -jar .\target\Requel-1.2.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Windows Command Prompt
set JAVA_HOME=C:\\Program Files\\Java\\jdk-17
set PATH=%JAVA_HOME%\\bin;%PATH%
java -jar .\target\Requel-1.2.0.jar "--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" --spring.datasource.username=root --spring.datasource.password=password --server.port=8081
```

Then access the app http://localhost:8081/

log in to the application as **admin** user with password **admin**.

### If You Use Docker
check out  https://hub.docker.com/r/rreganjr/requel/

```
docker network create requel-net || true

# MySQL 8.4 container (maps host port 3307)
docker run --name requelDB --net=requel-net -p3307:3306 -e MYSQL_ROOT_PASSWORD=pa33w0rd -d mysql:8.4

# Requel 1.2.0 image (Java 17 base), connecting to MySQL 8
docker run --name requel --net=requel-net -p8181:8080 -d \
  rreganjr/requel:1.2.0 \
  --spring.datasource.url=jdbc:mysql://requelDB:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
  --spring.datasource.username=root \
  --spring.datasource.password=pa33w0rd
```
Then access the app http://localhost:8181/ (host MySQL exposed on port 3307)

### Database initialization and upgrades

- **Fresh database**: nothing extra to pass. Flyway runs `V1__init.sql` (now emits proper `AUTO_INCREMENT` PKs and no `*_seq` tables). `V2__identity_cleanup.sql` is a no-op on a clean schema.
- **Existing database** (pre-Flyway or older schema with sequence tables):
  - If `flyway_schema_history` already exists, just start the new image; Flyway will run `V2__identity_cleanup.sql` to drop legacy `*_seq` tables and set PKs to `AUTO_INCREMENT`.
  - If there is no `flyway_schema_history`, start once with:
    ```
    -e SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
    -e SPRING_FLYWAY_BASELINE_VERSION=1
    ```
    This baselines your current schema as version 1, then applies `V2__identity_cleanup.sql`. Remove these env vars after the first successful start.
  - Example first start against an existing DB:
    ```bash
    docker run --name requel --net=requel-net -p8181:8080 -d \
      -e SPRING_FLYWAY_BASELINE_ON_MIGRATE=true \
      -e SPRING_FLYWAY_BASELINE_VERSION=1 \
      rreganjr/requel:1.2.0 \
      --spring.datasource.url=jdbc:mysql://requelDB:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
      --spring.datasource.username=root \
      --spring.datasource.password=pa33w0rd
    ```
    On subsequent starts, omit the two `SPRING_FLYWAY_*` env vars.
