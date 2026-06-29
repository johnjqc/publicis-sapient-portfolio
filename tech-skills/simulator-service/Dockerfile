# ==========================
# Build Stage
# ==========================
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY build.gradle settings.gradle ./

COPY src ./src

RUN gradle bootJar --no-daemon

# ==========================
# Runtime Stage
# ==========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=60s \
            --retries=3 \
            CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","app.jar"]