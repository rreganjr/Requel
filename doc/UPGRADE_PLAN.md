# Requel Migration Plan: Java 17 & Spring Boot 3.x

## Goals
- Adopt Java 17 as the baseline runtime and language level.
- Migrate all Java EE (`javax.*`) usage to Jakarta (`jakarta.*`) namespaces.
- Upgrade Spring Boot from 2.7.18 to the 3.x LTS line (target 3.3.x).
- Preserve functional parity while taking the opportunity to modernize security, build, and deployment configuration.

## JDK Management Strategy
- Install JDK 17 alongside existing JDK 11; no need to replace the global default.
  - Homebrew: `brew install openjdk@17` and use `JAVA_HOME=` paths per shell or IntelliJ.
  - SDKMAN/asdf: `sdk install java 17-tem` (or vendor of choice) and switch per project.
  - IntelliJ IDEA: add JDK 17 under *Project Structure → SDKs* and assign it only to the Requel module.
- For command-line builds, prefer Maven Toolchains so Maven can select JDK 17 without altering other shells.
  1. Create or edit `~/.m2/toolchains.xml` with an entry that points to your JDK 17 installation, for example:

     ```xml
     <toolchains>
       <toolchain>
         <type>jdk</type>
         <provides>
           <version>17</version>
           <vendor>any</vendor>
         </provides>
         <configuration>
           <jdkHome>/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home</jdkHome>
         </configuration>
       </toolchain>
     </toolchains>
     ```

  2. Wire the `maven-toolchains-plugin` into `pom.xml` (typically in the `<build><plugins>` section) so Maven respects the toolchain. When you run `mvn` for this project it will pick JDK 17 automatically, while other projects without a toolchain entry continue using Java 11.

## Phase 1 – Prepare on Spring Boot 2.7
1. **Baseline sanity checks**
   - Capture current build (`mvn -DskipTests=false clean verify`) and smoke-test results.
   - Note any integration or manual QA scripts that must be rerun post-migration.
2. **Move project to Java 17 while staying on Boot 2.7**
   - Update `pom.xml` properties (`<java.version>17</java.version>`) and compiler plugin configuration.
   - Ensure the build runs on JDK 17 using the Maven Toolchain or IntelliJ Project SDK.
   - Fix Java 17 compilation warnings (records, sealed types not expected; focus on reflection, illegal access, module path issues).
3. **Modernize Spring Security configuration**
   - Replace the `WebSecurityConfigurerAdapter` subclass with a `SecurityFilterChain` bean and supporting `AuthenticationManager`/`PasswordEncoder` configuration so the code matches Spring Security 6 expectations.
4. **Library hygiene while still on 2.7**
   - Swap deprecated libraries that will not survive Jakarta (e.g., ensure Apache HttpClient 4.x usage compiles, verify logging bridges).
   - Identify any `javax.*` dependencies already offering Jakarta artifacts and add conditional notes for Phase 2.
5. **Regression testing**
   - Run the full test suite on JDK 17.
   - Execute smoke tests and manual flows in IntelliJ using the JDK 17 runtime.

## Phase 2 – Jakarta Namespace Migration
1. **Inventory `javax.*` usage**
   - Use `rg 'javax\.' src/main/java` (already started) to produce a checklist of packages: `javax.servlet`, `javax.persistence`, `javax.validation`, `javax.xml.bind`, etc.
   - Current usage (2025-09-24):
     - Application/server: `javax.servlet.*`, `javax.servlet.annotation.WebListener`, `javax.servlet.http.*`.
     - Persistence/JPA: `javax.persistence.*` across domain entities, repositories, and helper classes.
     - Bean validation: `javax.validation.constraints.*` on DTO/domain fields.
     - JAXB / XML binding: `javax.xml.bind.*` plus annotations (XmlElement/ID/Type) and adapters in annotation/domain packages.
     - Miscellaneous: `javax.annotation.Resource`, `javax.crypto.*`, `javax.xml.transform.*`, `javax.xml.parsers.*` (standard JDK modules – migrate only if required by downstream libs).
2. **Update dependencies to Jakarta releases**
   - Replace key coordinates: `jakarta.persistence:jakarta.persistence-api`, `jakarta.servlet:jakarta.servlet-api`, `jakarta.validation:jakarta.validation-api`, `jakarta.xml.bind:jakarta.xml.bind-api`, etc.
   - Introduce `org.glassfish.jaxb:jaxb-runtime` for Jakarta JAXB runtime needs.
   - Dependencies currently binding to `javax` APIs:
     - `javax.xml.bind:jaxb-api`, `com.sun.xml.bind:jaxb-impl`, `com.sun.xml.bind:jaxb-core` → swap for Jakarta (`jakarta.xml.bind:jakarta.xml.bind-api`, `org.glassfish.jaxb:jaxb-runtime`).
     - Legacy Echo/Echopoint artifacts and custom `echopm` jars are compiled against `javax.servlet` and likely have no Jakarta build – evaluate upgrade/replacement strategy.
     - Hibernate/JPA stack via Spring Boot 2.7 still depends on `javax.persistence`; final migration will come with Boot 3 + Hibernate 6.
   - Short-term bridge: Maven now runs the Eclipse Jakarta Transformer CLI (via `scripts/java17-transform.sh`) during the `initialize` phase to rewrite the Echo/Echopoint/EchoPM jars to `jakarta.*` namespaces and installs those transformed artifacts under the original coordinates, keeping the existing UI working while the backend upgrades.
3. **Refactor imports and annotations**
   - Migrate code imports to `jakarta.*` equivalents and adjust package names in XML, configuration classes, and reflection usage.
   - Validate serialization/deserialization logic that depends on JAXB.
4. **Third-party library audit**
   - Echo2/Echopoint/EchoPM artifacts: confirm if Jakarta-compatible forks exist; otherwise plan shims or consider repackaging/rewriting the UI layer.
   - Any vendor library still using `javax.*` must be upgraded or replaced; document blockers.
5. **Build and test on Boot 2.7 + Jakarta APIs**
   - Ensure the app still runs on Tomcat 9 with the Jakarta artifacts (back-compat works via the servlet API bridge bundled in Boot 2.7).
   - Run automated and manual tests again.

## Phase 3 – Upgrade to Spring Boot 3.x
1. **Bump Spring Boot parent**
   - Update `pom.xml` parent to `spring-boot-starter-parent` 3.3.x (latest patch).
   - Review/refresh plugin versions if the new parent exposes newer defaults.
2. **Align configuration and properties**
   - Update renamed configuration keys (see Boot 3 migration guide).
   - Validate actuator endpoints, management port bindings, and metrics exports.
3. **Verify dependency compatibility**
   - Remove any legacy starters replaced or removed in Boot 3 (e.g., separate validation starter no longer needed).
   - Ensure Hibernate/JPA version aligns (Boot 3 pulls Hibernate 6 with Jakarta persistence).
4. **Re-test security and authentication**
   - Confirm the new `SecurityFilterChain` works with Spring Security 6; update password encoders and test login flows.
5. **Final build and regression**
   - `mvn -DskipTests=false clean verify` on JDK 17.
   - Run integration tests, execute manual QA, and validate Docker image build.
   - Update deployment scripts if they expect Java 8/11.

## Post-upgrade Considerations
- Monitor performance and memory characteristics under the new JDK.
- Review logging configuration (Logback vs Log4j bridges) for compatibility with the latest dependencies.
- Plan follow-up work to modernize remaining third-party libraries (commons-lang3 3.1, commons-io 2.7, etc.).
- Document any manual steps required for team members (IDE setup, local toolchains).
