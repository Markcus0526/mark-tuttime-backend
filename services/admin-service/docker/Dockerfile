# syntax=docker/dockerfile:1.4

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

RUN rm -rf ~/.m2

COPY pom.xml ./pom.xml

# Placeholder POMs for modules not being built
RUN mkdir -p services/user-service services/token-service services/mining-service services/staking-service \
    services/content-service services/booking-integration-service services/notification-service \
    services/conversion-service services/rewards-service services/cdn-proxy-service \
    services/messaging-service services/social-service \
    api-gateway admin-gateway && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>user-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/user-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>token-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/token-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>mining-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/mining-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>staking-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/staking-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>content-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/content-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>booking-integration-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/booking-integration-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>notification-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/notification-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>conversion-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/conversion-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>rewards-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/rewards-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>cdn-proxy-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/cdn-proxy-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>messaging-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/messaging-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>social-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/social-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>api-gateway</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > api-gateway/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.aparttoken</groupId><artifactId>admin-gateway</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > admin-gateway/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn install -N -DskipTests -T 1C

# Build common-libs
COPY common-libs/commons-exception/pom.xml ./common-libs/commons-exception/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl common-libs/commons-exception -am -T 1C || true
COPY common-libs/commons-exception/src ./common-libs/commons-exception/src
RUN --mount=type=cache,target=/root/.m2 \
    cd common-libs/commons-exception && mvn clean install -DskipTests -T 1C

COPY common-libs/commons-events/pom.xml ./common-libs/commons-events/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl common-libs/commons-events -am -T 1C || true
COPY common-libs/commons-events/src ./common-libs/commons-events/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn protobuf:compile protobuf:compile-custom -pl common-libs/commons-events -DskipTests -T 1C 2>&1 | tee /tmp/protobuf.log || true && \
    if [ -d common-libs/commons-events/target/protoc-plugins ]; then \
        find common-libs/commons-events/target/protoc-plugins -type f -name "*.exe" 2>/dev/null | while read f; do \
            linuxf="${f%.exe}"; \
            if [ ! -f "$linuxf" ]; then \
                cp "$f" "$linuxf" && chmod +x "$linuxf" && echo "Fixed: $linuxf"; \
            fi; \
        done; \
    fi && \
    cd common-libs/commons-events && mvn clean install -DskipTests -T 1C

COPY common-libs/commons-auth/pom.xml ./common-libs/commons-auth/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl common-libs/commons-auth -am -T 1C || true
COPY common-libs/commons-auth/src ./common-libs/commons-auth/src
RUN --mount=type=cache,target=/root/.m2 \
    cd common-libs/commons-auth && mvn clean install -DskipTests -T 1C

# Build admin-service
COPY services/admin-service/pom.xml ./services/admin-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd services/admin-service && mvn dependency:go-offline -T 1C || true
COPY services/admin-service/src ./services/admin-service/src
RUN --mount=type=cache,target=/root/.m2 \
    cd services/admin-service && mvn clean package -DskipTests -T 1C

FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

COPY --from=builder /app/services/admin-service/target/admin-service-*.jar app.jar

EXPOSE 28090

ENTRYPOINT ["java", "-jar", "app.jar"]
