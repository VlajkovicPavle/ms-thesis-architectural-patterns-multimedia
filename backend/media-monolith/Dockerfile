FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM media-base:21
WORKDIR /app
ENV APP_STORAGE_LOCAL_PATH=/var/lib/media
USER root
RUN mkdir -p /var/lib/media && chown -R app:app /var/lib/media /app
USER app
COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
