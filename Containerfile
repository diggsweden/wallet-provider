# SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
# SPDX-License-Identifier: EUPL-1.2

# Stage 1: Build stage
FROM docker.io/library/eclipse-temurin:25-jdk-alpine-3.23@sha256:416bd6ae24de242440633e6e449c533df9e902b3f0da8f9049227c06ccb254a4 AS builder

LABEL maintainer="Digg - Agency for Digital Government"
LABEL description="Build stage for Wallet Provider"

# Install dependencies needed for building
# hadolint ignore=DL3018
RUN apk add --no-cache curl

# Create app directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable and download dependencies (cached if pom.xml doesn't change)
RUN chmod +x ./mvnw && \
    ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip checkstyle in Docker build)
RUN ./mvnw clean package -DskipTests -Dcheckstyle.skip -Dformatter.skip -B && \
    java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime stage
FROM cgr.dev/chainguard/jre:latest@sha256:650b440197c70d9a9f2505b601f812e10c24607e59810e9b1415ec74337d313d AS runtime

LABEL maintainer="Digg - Agency for Digital Government"
LABEL description="Wallet Provider"

WORKDIR /app

# Copy Spring Boot layers from builder stage (nonroot user: 65532)
COPY --from=builder --chown=65532:65532 /app/dependencies/ ./
COPY --from=builder --chown=65532:65532 /app/spring-boot-loader/ ./
COPY --from=builder --chown=65532:65532 /app/snapshot-dependencies/ ./
COPY --from=builder --chown=65532:65532 /app/application/ ./

EXPOSE 8080

# JVM options are set directly in ENTRYPOINT since distroless has no shell
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "org.springframework.boot.loader.launch.JarLauncher"]
