# syntax=docker/dockerfile:1.7

# ─── Stage 1: build con Maven ───────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache de dependencias: copiamos solo el pom primero
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests dependency:go-offline

# Codigo fuente
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests package && \
    cp target/*.jar app.jar

# ─── Stage 2: runtime con JRE 21 ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# curl para HEALTHCHECK
RUN apk add --no-cache curl

# Usuario no-root con su propio grupo (UID fijo 1001, NO en root)
RUN addgroup -S spring && adduser -S -G spring -u 1001 spring

COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar

USER spring
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

# `exec` hace que la JVM reemplace al shell -> SIGTERM llega a Java
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
