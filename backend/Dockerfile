# syntax=docker/dockerfile:1

FROM gradle:8.10.2-jdk17-alpine AS builder
WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY backend backend

RUN chmod +x gradlew
RUN ./gradlew :backend:clean :backend:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && apk add --no-cache wget

COPY --from=builder /workspace/backend/build/libs/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

