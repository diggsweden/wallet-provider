# SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
# SPDX-License-Identifier: EUPL-1.2

# Stage 1: Build stage
FROM docker.io/library/eclipse-temurin:21-jdk-alpine@sha256:c98f0d2e171c898bf896dc4166815d28a56d428e218190a1f35cdc7d82efd61f AS builder

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
RUN ./mvnw clean package -DskipTests -Dcheckstyle.skip=true -B && \
    java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Runtime stage
FROM cgr.dev/chainguard/jre:latest@sha256:c4e02258ca54fcf0e0ea8b3cc5b9f0bcfa95fb2947ca280c9b529c88e992f8aa AS runtime

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