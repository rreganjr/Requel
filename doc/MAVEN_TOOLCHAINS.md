# Maven Toolchains Configuration

This repository relies on Maven Toolchains to pin specific JDK versions per build without changing your global `JAVA_HOME`. The toolchains file lets developers keep multiple JDKs installed (Java 11, 17, 24, etc.) and have Maven automatically select the correct one based on project requirements.

## Why we use toolchains
- Spring Boot 3 migration requires Java 17, but some developers still build other projects with Java 11.
- The toolchain keeps `mvn` invocations consistent regardless of which JDK your shell or IDE currently points at.
- CI can define its own `toolchains.xml` with the same schema, ensuring the same JDK matrix across environments.

## Example `~/.m2/toolchains.xml`

```xml
<toolchains>
    <toolchain>
        <type>jdk</type>
        <provides>
            <version>11</version>
            <vendor>any</vendor>
        </provides>
        <configuration>
            <jdkHome>/opt/homebrew/Cellar/openjdk@11/11.0.24/libexec/openjdk.jdk/Contents/Home</jdkHome>
        </configuration>
    </toolchain>

    <toolchain>
        <type>jdk</type>
        <provides>
            <version>17</version>
            <vendor>any</vendor>
        </provides>
        <configuration>
            <jdkHome>/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home</jdkHome>
        </configuration>
    </toolchain>

    <toolchain>
        <type>jdk</type>
        <provides>
            <version>24</version>
            <vendor>any</vendor>
        </provides>
        <configuration>
            <jdkHome>/opt/homebrew/Cellar/openjdk/24.0.2/libexec/openjdk.jdk/Contents/Home</jdkHome>
        </configuration>
    </toolchain>
</toolchains>
```

## Creating or editing the file
- Path: `~/.m2/toolchains.xml` (hidden by default; use `⌘⇧.` in macOS file dialogs to reveal).
- The `<jdkHome>` paths above match Homebrew installs; adjust them if you use SDKMAN, asdf, or vendor-provided packages.
- Add or remove `<toolchain>` entries as needed—Maven matches on `<version>` and `<vendor>` when executing the `maven-toolchains-plugin` goal in `pom.xml`.

## Verification
1. Ensure the specified directories contain a `bin/java` executable (e.g. run `/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home/bin/java -version`).
2. Run `mvn --version`; the output should still reflect your default JDK. This is expected.
3. Run `mvn -DskipTests=true validate` inside the project. The plugin logs the toolchain it selects:
   
   ```
   [INFO] Required toolchain: jdk [ vendor='any' version='17' ]
   [INFO] Found matching toolchain for type jdk: JDK[/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home]
   ```
4. If Maven cannot find a matching toolchain, it fails fast so you can fix the path or add an entry.

Keeping this file synchronized across local machines and CI ensures that future upgrades (e.g., Java 21 LTS) are just a matter of adding another block and updating the version requested in `pom.xml`.
