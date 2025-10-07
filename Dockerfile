FROM eclipse-temurin:17-jre-jammy
LABEL maintainer=rreganjr@users.sourceforge.net
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
