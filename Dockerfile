# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Resolve dependencies first (cached until pom.xml changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Build and extract layered JAR (most-stable layers first)
COPY src/ src/
RUN ./mvnw package -DskipTests -q && \
    java -Djarmode=layertools -jar target/*.jar extract --destination layers

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S store && adduser -S store -G store && mkdir -p /app && chown store:store /app

WORKDIR /app
USER store

# Copy layers in stability order — only 'application' changes on code updates
COPY --from=builder --chown=store:store /app/layers/dependencies/          ./
COPY --from=builder --chown=store:store /app/layers/spring-boot-loader/    ./
COPY --from=builder --chown=store:store /app/layers/snapshot-dependencies/ ./
COPY --from=builder --chown=store:store /app/layers/application/           ./

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher", \
  "--spring.profiles.active=prod"]
