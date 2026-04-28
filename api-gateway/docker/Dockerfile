# syntax=docker/dockerfile:1.4
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 1. Copy ONLY the POM files first (The "Skeleton")
# This allows Maven to resolve the project structure without the source code
COPY pom.xml ./
COPY common-libs/common/pom.xml ./common-libs/common/
COPY common-libs/common-event/pom.xml ./common-libs/common-event/
COPY common-libs/common-auth/pom.xml ./common-libs/common-auth/

COPY api-gateway/pom.xml ./api-gateway/

# Copy service poms (Even if not building them, they must exist for the Root POM to be valid)
COPY services/user-service/pom.xml ./services/user-service/
COPY services/restaurant-service/pom.xml ./services/restaurant-service/
COPY services/menu-service/pom.xml ./services/menu-service/
COPY services/booking-service/pom.xml ./services/booking-service/
COPY services/payment-service/pom.xml ./services/payment-service/
COPY services/order-service/pom.xml ./services/order-service/
COPY services/pos-service/pom.xml ./services/pos-service/
COPY services/review-service/pom.xml ./services/review-service/
COPY services/analytics-service/pom.xml ./services/analytics-service/
COPY services/notification-service/pom.xml ./services/notification-service/

# 2. Build the root project
RUN --mount=type=cache,target=/root/.m2 \
    mvn install -N -DskipTests

# 3. Build common-libs/common (Source + Build)
COPY common-libs/common/src ./common-libs/common/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -pl common-libs/common -DskipTests -T 1C

# 4. Build common-libs/common-event (Source + Protobuf + Build)
COPY common-libs/common-event/src ./common-libs/common-event/src
RUN --mount=type=cache,target=/root/.m2 \
    cd common-libs/common-event && \
    mvn protobuf:compile protobuf:compile-custom -DskipTests -T 1C || true && \
    if [ -d target/protoc-plugins ]; then \
        find target/protoc-plugins -type f -name "*.exe" | while read f; do \
            [ ! -f "${f%.exe}" ] && cp "$f" "${f%.exe}" && chmod +x "${f%.exe}"; \
        done; \
    fi && \
    mvn clean install -DskipTests -T 1C

# 5. Build common-auth (Source + Build)
COPY common-libs/common-auth/src ./common-libs/common-auth/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean install -pl common-libs/common-auth -DskipTests -T 1C

# 6. Build api-gateway (Source + Final Package)
COPY api-gateway/src ./api-gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl api-gateway -am -DskipTests -T 1C

# --- Final Runner Stage ---
FROM eclipse-temurin:21-jre-jammy AS runner
RUN useradd -ms /bin/bash tuttime
USER tuttime
WORKDIR /app
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
