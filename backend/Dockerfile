# Stage 1: Build
FROM eclipse-temurin:22-jdk AS builder

# The Gradle wrapper script (gradlew) and wrapper JAR are gitignored, so
# install the Gradle version pinned in gradle/wrapper/gradle-wrapper.properties
# directly instead of relying on the wrapper being present in the build context.
ARG GRADLE_VERSION=8.14

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl unzip \
 && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip \
 && unzip -q /tmp/gradle.zip -d /opt \
 && rm /tmp/gradle.zip \
 && ln -s "/opt/gradle-${GRADLE_VERSION}" /opt/gradle

ENV GRADLE_HOME=/opt/gradle
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

WORKDIR /app

COPY . .

# Build the fat JAR (Ktor plugin task); --no-daemon keeps the build self-contained.
RUN gradle buildFatJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:22-jre

WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /app/data /app/downloads

COPY --from=builder /app/build/libs/app.jar app.jar

ENV DATA_DIR=/app/data
ENV DOWNLOADS_DIR=/app/downloads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
