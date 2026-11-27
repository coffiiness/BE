# Official Temurin 17.0.21.1+2 Docker tags lack ARM64. Install the SAME official,
# SHA-256-pinned binaries on digest-pinned Ubuntu Jammy for both supported CPUs.
FROM --platform=$BUILDPLATFORM ubuntu:22.04@sha256:b000e69a50ff940390233025e7a337fb8469b509e2334fa4d018ac30f3e18cdb AS build
ARG BUILDARCH
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
COPY docker/install-temurin17.sh /tmp/install-temurin17.sh
COPY docker/verify-temurin17.sh /tmp/verify-temurin17.sh
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ca-certificates curl fontconfig p11-kit tzdata locales \
    && printf 'en_US.UTF-8 UTF-8\n' > /etc/locale.gen && locale-gen \
    && rm -rf /var/lib/apt/lists/* \
    && sed -i 's/\r$//' /tmp/install-temurin17.sh \
    && sh /tmp/install-temurin17.sh jdk "$BUILDARCH" \
    && sed -i 's/\r$//' /tmp/verify-temurin17.sh \
    && sh /tmp/verify-temurin17.sh "$BUILDARCH" \
    && rm /tmp/install-temurin17.sh /tmp/verify-temurin17.sh
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties lint.gradle quality.gradle ./
COPY core core
COPY storage storage
COPY support support
COPY clients clients
ENV SPRING_PROFILES_ACTIVE=local APP_EMAIL_ENABLED=false AWS_EC2_METADATA_DISABLED=true
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && find core storage support clients -type f -name '*.java' -exec sed -i 's/\r$//' {} + \
    && AWS_ACCESS_KEY_ID=validation-only AWS_SECRET_ACCESS_KEY=validation-only \
       ./gradlew --no-daemon --max-workers=1 qualityCheck :core:core-api:bootJar

# Reuse the official Jammy OS packages by immutable manifest. Remove its Java
# before the final rootfs copy, so no older JVM is included in the runtime image.
FROM eclipse-temurin@sha256:c6a5dd70fb24f4e07ef88cbda8efb613a5a649a1af071a41de8eaabd1ee6b5da AS runtime-os
FROM --platform=$BUILDPLATFORM ubuntu:22.04@sha256:b000e69a50ff940390233025e7a337fb8469b509e2334fa4d018ac30f3e18cdb AS prepare-runtime
ARG TARGETARCH
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=runtime-os / /runtime-root/
COPY docker/install-temurin17.sh /tmp/install-temurin17.sh
COPY docker/verify-temurin17.sh /runtime-root/tmp/verify-temurin17.sh
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* /runtime-root/opt/java/openjdk \
    && sed -i 's/\r$//' /tmp/install-temurin17.sh /runtime-root/tmp/verify-temurin17.sh \
    && sh /tmp/install-temurin17.sh jre "$TARGETARCH" /runtime-root/opt/java/openjdk

FROM scratch AS runtime-base
COPY --from=prepare-runtime /runtime-root/ /
ARG TARGETARCH
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
RUN sh /tmp/verify-temurin17.sh "$TARGETARCH" && rm /tmp/verify-temurin17.sh
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# Trusted artifact path: tools/deployment/build-arm64-runtime.ps1 runs the
# unchanged gate first, then supplies the verified JAR and its content hash.
FROM runtime-base AS runtime-from-jar
ARG APP_JAR_SHA256
LABEL com.calfit.java.runtime="17.0.21.1+2" com.calfit.jar.sha256="$APP_JAR_SHA256"
COPY app.jar /app/app.jar
RUN test -n "$APP_JAR_SHA256" \
    && printf '%s  /app/app.jar\n' "$APP_JAR_SHA256" | sha256sum --check --strict -

# Default source build always runs the full Gradle gate. No skip-test switch.
FROM runtime-base AS runtime
COPY --from=build /app/core/core-api/build/libs/*.jar /app/app.jar
