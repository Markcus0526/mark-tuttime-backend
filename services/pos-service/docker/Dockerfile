# syntax=docker/dockerfile:1.4
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copy Skeleton POMs to satisfy Maven Reactor
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

# 2. Build Shared Libraries
RUN --mount=type=cache,target=/root/.m2 mvn install -N -DskipTests
COPY common-libs/common/src ./common-libs/common/src
RUN --mount=type=cache,target=/root/.m2 mvn clean install -pl common-libs/common -am -DskipTests

COPY common-libs/common-event/src ./common-libs/common-event/src
RUN --mount=type=cache,target=/root/.m2 cd common-libs/common-event && \
    mvn protobuf:compile -DskipTests || true && mvn clean install -DskipTests

COPY common-libs/common-auth/src ./common-libs/common-auth/src
RUN --mount=type=cache,target=/root/.m2 mvn clean install -pl common-libs/common-auth -am -DskipTests

# 3. Build Pos Service
COPY services/pos-service/src ./services/pos-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl services/pos-service -am -DskipTests

# --- Runner Stage ---
FROM eclipse-temurin:21-jre-jammy AS runner
RUN useradd -ms /bin/bash tuttime && mkdir -p /app/uploads/medias && chown tuttime:tuttime /app/uploads/medias
USER tuttime
WORKDIR /app
COPY --from=builder /app/services/pos-service/target/pos-service-*.jar app.jar

USER root
ENTRYPOINT ["sh", "-c", "chown -R tuttime:tuttime /app/uploads && runuser -u tuttime -- java $JAVA_OPTS -jar app.jar"]

ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0 -Duser.timezone=UTC"
EXPOSE 28007 29007
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
