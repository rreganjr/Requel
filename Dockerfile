FROM openjdk:8-jdk-alpine
LABEL maintainer=rreganjr@users.sourceforge.net
VOLUME /tmp
#ARG JAR_FILE
#ADD ${JAR_FILE} /app.jar
ADD target/Requel-1.0.2.jar /app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
# "--spring.datasource.url=jdbc:mysql://localhost:3306/requeldb?createDatabaseIfNotExist=true", "--spring.datasource.username=root", "--spring.datasource.password=password"
