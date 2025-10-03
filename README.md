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
 
Optionally pass the service port like this:

* **port** --server.port=\<portnumber\>

### Example command

Note for zsh users: quote the JDBC URL (because of the `?`) or prefix the command with `noglob`. Ensure the command runs on Java 17 (set `JAVA_HOME` or use a 17-enabled shell) before launching the jar.

```bash
# macOS (uses /usr/libexec/java_home)
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Linux (set JAVA_HOME manually)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Windows PowerShell
$env:JAVA_HOME="C:\\Program Files\\Java\\jdk-17"
$env:Path="${env:JAVA_HOME}\\bin;${env:Path}"
java -jar .\target\Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.datasource.username=root --spring.datasource.password='password' --server.port=8081

# Windows Command Prompt
set JAVA_HOME=C:\\Program Files\\Java\\jdk-17
set PATH=%JAVA_HOME%\\bin;%PATH%
java -jar .\target\Requel-1.1.0.jar "--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" --spring.datasource.username=root --spring.datasource.password=password --server.port=8081
```

Then access the app http://localhost:8081/

One‑time schema init (if starting from an empty DB):

```bash
# macOS one-time schema init
JAVA_HOME=$(/usr/libexec/java_home -v 17) PATH="$JAVA_HOME/bin:$PATH" \
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Linux
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
java -jar ./target/Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Windows PowerShell
$env:JAVA_HOME="C:\\Program Files\\Java\\jdk-17"
$env:Path="${env:JAVA_HOME}\\bin;${env:Path}"
java -jar .\target\Requel-1.1.0.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' --spring.jpa.hibernate.ddl-auto=create --server.port=8081

# Windows Command Prompt
set JAVA_HOME=C:\\Program Files\\Java\\jdk-17
set PATH=%JAVA_HOME%\\bin;%PATH%
java -jar .\target\Requel-1.1.0.jar "--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" --spring.jpa.hibernate.ddl-auto=create --server.port=8081
```
Then access the app http://localhost:8081/

log in to the application as **admin** user with password **admin**.

### If You Use Docker
check out  https://hub.docker.com/r/rreganjr/requel/

```
docker network create requel-net || true

# MySQL 8.4 container (maps host port 3307)
docker run --name requelDB --net=requel-net -p3307:3306 -e MYSQL_ROOT_PASSWORD=pa33w0rd -d mysql:8.4

# Requel 1.1.0 image (Java 17 base), connecting to MySQL 8
docker run --name requel --net=requel-net -p8181:8080 -d \
  rreganjr/requel:1.1.0 \
  --spring.datasource.url=jdbc:mysql://requelDB:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
  --spring.datasource.username=root \
  --spring.datasource.password=pa33w0rd
```
Then access the app http://localhost:8181/ (host MySQL exposed on port 3307)
