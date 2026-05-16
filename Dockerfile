FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="11188410+rreganjr@users.noreply.github.com"
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
