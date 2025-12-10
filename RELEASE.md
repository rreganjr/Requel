# Release Checklist (v1.1.0)

1. **Sync master**
   ```bash
   git checkout master
   git pull
   ```

2. **Full build & tests (once with Echo transform)**
   ```bash
   mvn -pl modules/requel-app -am clean verify -DskipTests=false
   ```

3. **Tag**
   ```bash
   git tag -a v1.1.0 -m "UI modularization and cleanup"
   git push origin v1.0.0
   ```

4. **Fast iterative build (skips echo transform/tests)**
   ```bash
   mvn -pl modules/requel-app -am package -DskipEchoTransform=true -DskipTests=true
   ```

5. **Run locally**
   ```bash
   java -jar modules/requel-app/target/requel-app-1.1.0.jar
   # then visit http://localhost:8080/
   ```

6. **GitHub Packages (Maven)**
   - In `~/.m2/settings.xml`:
     ```xml
     <servers>
       <server>
         <id>github</id>
         <username>YOUR_GH_USERNAME</username>
         <password>${env.GITHUB_TOKEN}</password> <!-- token with write:packages -->
       </server>
     </servers>
     ```
   - Ensure root `pom.xml` has:
     ```xml
     <distributionManagement>
       <repository>
         <id>github</id>
         <name>GitHub Packages</name>
         <url>https://maven.pkg.github.com/rreganjr/Requel</url>
       </repository>
     </distributionManagement>
     ```
   - Deploy:
     ```bash
     mvn -pl modules/requel-app -am deploy -DskipTests=true -DskipEchoTransform=true
     ```

7. **Docker image**
   ```bash
   mvn -pl modules/requel-app -am package -Pdocker-image -DskipTests=true -DskipEchoTransform=true
   # push if logged in: docker push rreganjr/requel:latest
   ```
   Manual alternative:
   ```bash
   docker build -t rreganjr/requel:1.1.0 .
   docker run -p 8080:8080 rreganjr/requel:1.1.0
   ```

8. **Smoke checks**
   - Login, UI assets load
   - Basic navigation works

