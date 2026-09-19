# Log4j Update Assessment

## Summary

Requel is still explicitly using Log4j 1.x today.

- The current repo is on Java 17 and Spring Boot `3.3.4` in the root parent POM, not `3.2`.
- Spring Boot already brings its default logging stack: SLF4J + Logback.
- Requel also includes both:
  - the real Log4j 1.x implementation: `log4j:log4j:1.2.17`
  - the Log4j 1.x compatibility bridge: `org.slf4j:log4j-over-slf4j`

That combination is the core problem. The old Log4j 1.x artifact is what triggers the Dependabot findings, and it also explains the runtime `log4j:WARN` messages.

## What I Found

### 1. Spring Boot already bundles logging

The root build inherits from Spring Boot:

- [pom.xml](./Requel/pom.xml:9) uses `spring-boot-starter-parent` `3.3.4`
- [pom.xml](./Requel/pom.xml:20) sets `java.version` to `17`

Spring Boot starters bring `spring-boot-starter-logging`, which brings Logback. Maven dependency tree confirmed `ch.qos.logback:logback-classic` is already on the app classpath.

### 2. Requel still directly pulls Log4j 1.x

There is one explicit dependency on the legacy implementation:

- [modules/dictionary-jpa/pom.xml](./Requel/modules/dictionary-jpa/pom.xml:50)

```xml
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
```

That dependency flows into the app through `dictionary-jpa` and then onward into `project-domain`, `annotation-jpa`, `nlp-jpa`, `service-api`, `service-impl`, and `requel-app`.

### 3. Requel also includes the bridge to Spring logging

These modules already depend on the bridge:

- [modules/platform-core/pom.xml](./Requel/modules/platform-core/pom.xml:24)
- [modules/requel-app/pom.xml](./Requel/modules/requel-app/pom.xml:442)

`log4j-over-slf4j` provides Log4j 1.x API classes that route logging into SLF4J/Logback.

This is good as a migration aid, but it should not coexist with the real `log4j:log4j` jar. Right now both are present.

### 4. Source code still uses the Log4j 1.x API heavily

I found:

- `94` Java files in the repo importing `org.apache.log4j`
- `52` of those are in modules that are part of the current Maven reactor build

Current reactor usage by module:

- `modules/nlp-jpa`: `26`
- `modules/platform-core`: `11`
- `modules/project-jpa`: `6`
- `modules/dictionary-jpa`: `4`
- `modules/requel-app`: `4`
- `modules/user-jpa`: `1`

Most usages are simple `Logger` calls and should migrate cleanly to SLF4J later. I found only one active reactor class importing `org.apache.log4j.Level`:

- [modules/nlp-jpa/src/main/java/com/rreganjr/nlp/impl/StanfordLexicalizedParser.java](./Requel/modules/nlp-jpa/src/main/java/com/rreganjr/nlp/impl/StanfordLexicalizedParser.java:33)

So the code migration is broad, but not especially complex.

### 5. The `log4j:WARN No appenders` message is consistent with the current setup

You reported:

```text
log4j:WARN No appenders could be found for logger (com.rreganjr.ResourceBundleHelper).
```

That logger is created in:

- [modules/platform-core/src/main/java/com/rreganjr/ResourceBundleHelper.java](./Requel/modules/platform-core/src/main/java/com/rreganjr/ResourceBundleHelper.java:87)

The warning strongly suggests the real Log4j 1.x implementation is being loaded at runtime and it does not see a usable Log4j 1.x configuration.

There is a legacy config file here:

- [conf/log4j.properties](./Requel/conf/log4j.properties:1)

But I found no build or runtime references to that file, and it is not under `src/main/resources`. It also contains old placeholder tokens like `@log4j.rootLogger.level@` and Tomcat-era paths like `${catalina.base}/logs/...`, which do not match the current Spring Boot packaging model.

## Answer To The Main Questions

### Are we explicitly using Log4j 1.x?

Yes.

- Direct dependency: yes, in `dictionary-jpa`
- Direct API usage in source: yes, across many classes
- Runtime presence in the app: yes, confirmed by Maven dependency tree and consistent with the console warnings

### Can we just use what is bundled in Spring?

Yes, that should be the target.

Spring Boot's default logging stack is already present and is the right default for this application. You do not need Log4j 1.x to keep logging working.

### Do we need to update a lot of Java to do that?

Not for the first step.

There are two separate tasks:

1. **Security/runtime cleanup**
   Remove the real `log4j:log4j` dependency and keep `log4j-over-slf4j`.

   This should eliminate the vulnerable Log4j 1.x implementation from the shipped app while allowing existing `org.apache.log4j.Logger` source code to continue compiling and route through Spring Boot's Logback backend.

2. **Source cleanup**
   Later, replace `org.apache.log4j.Logger` imports with `org.slf4j.Logger` and `org.slf4j.LoggerFactory`, then remove `log4j-over-slf4j` once no code depends on the old API.

The first step should be low risk. The second step touches many files, but the changes are mostly mechanical.

## Recommended Path

### Recommended now

1. Remove `log4j:log4j:1.2.17` from [modules/dictionary-jpa/pom.xml](./Requel/modules/dictionary-jpa/pom.xml:50).
2. Keep `log4j-over-slf4j` for now.
3. Verify the app starts without the `log4j:WARN No appenders` messages.
4. Add Spring Boot logging configuration only if you want custom levels or file output.

This path uses the logging stack already bundled with Spring Boot and should address the Dependabot findings tied to Log4j 1.x.

### Recommended later

Migrate source imports from Log4j 1.x to SLF4J in the active reactor modules first:

- `platform-core`
- `user-jpa`
- `dictionary-jpa`
- `project-jpa`
- `nlp-jpa`
- `requel-app`

After that, remove `log4j-over-slf4j` as well.

## Risk / Effort Assessment

### Option A: Remove only the real Log4j 1.x artifact now

- Risk: low
- Effort: low
- Code churn: minimal
- Benefit: removes the vulnerable legacy implementation from the app classpath

This is the best immediate move.

### Option B: Fully migrate source to SLF4J now

- Risk: low to moderate
- Effort: moderate
- Code churn: moderate because of file count, not because of hard logic changes
- Benefit: removes both the legacy implementation and the legacy API dependency

This is worthwhile, but it does not need to block the immediate cleanup.

## Bottom Line

Requel is explicitly using Log4j 1.x today, and the current app classpath contains both the real Log4j 1.x jar and Spring Boot's normal Logback stack.

The safest near-term fix is:

- remove `log4j:log4j:1.2.17`
- keep `log4j-over-slf4j`
- rely on Spring Boot's bundled logging

That should not require a large Java rewrite up front. The larger Java change is only needed if you want to remove the old Log4j 1.x API imports entirely.
