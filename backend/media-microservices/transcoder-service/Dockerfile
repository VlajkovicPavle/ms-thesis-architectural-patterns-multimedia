FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY messaging-contract/pom.xml messaging-contract/pom.xml
COPY media-service/pom.xml media-service/pom.xml
COPY transcoder-service/pom.xml transcoder-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY messaging-contract/src messaging-contract/src
COPY transcoder-service/src transcoder-service/src
RUN mvn -pl transcoder-service -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/transcoder-service/target/transcoder-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
