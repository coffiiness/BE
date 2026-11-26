# The Java 17 Alpine images do not publish ARM64 variants. Build the portable
# bootJar on the builder's native architecture, then select the target JRE below.
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties lint.gradle ./
COPY core core
COPY storage storage
COPY support support
COPY clients clients
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon :core:core-api:bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Jammy uses apt, not apk. curl is used by the Oracle Compose healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/core/core-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
