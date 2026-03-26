# syntax=docker/dockerfile:1.4

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./pom.xml

# Placeholder POMs for modules not being built
RUN mkdir -p services/admin-service services/analytics-service services/booking-service services/pos-service \
    services/menu-service services/order-service services/payment-service \
    services/restaurant-service services/review-service services/user-service \
    admin-gateway && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>admin-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/admin-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>analytics-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/analytics-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>booking-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/booking-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>pos-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/pos-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>menu-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/menu-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>order-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/order-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>payment-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/payment-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>restaurant-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/restaurant-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>review-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/review-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>user-service</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > services/user-service/pom.xml && \
    echo '<?xml version="1.0"?><project><modelVersion>4.0.0</modelVersion><groupId>com.tuttime</groupId><artifactId>api-gateway</artifactId><version>1.0-SNAPSHOT</version><packaging>pom</packaging></project>' > admin-gateway/pom.xml

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
    cd common-libs/commons-events && \
    mvn protobuf:compile protobuf:compile-custom -DskipTests -T 1C 2>&1 | tee /tmp/protobuf.log || true && \
    if [ -d target/protoc-plugins ]; then \
        find target/protoc-plugins -type f -name "*.exe" 2>/dev/null | while read f; do \
            linuxf="${f%.exe}"; \
            if [ ! -f "$linuxf" ]; then \
                cp "$f" "$linuxf" && chmod +x "$linuxf" && echo "Fixed: $linuxf"; \
            fi; \
        done; \
    fi && \
    mvn clean install -DskipTests -T 1C

COPY common-libs/commons-auth/pom.xml ./common-libs/commons-auth/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl common-libs/commons-auth -am -T 1C || true
COPY common-libs/commons-auth/src ./common-libs/commons-auth/src
RUN --mount=type=cache,target=/root/.m2 \
    cd common-libs/commons-auth && mvn clean install -DskipTests -T 1C

# Build api-gateway
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl api-gateway -am -T 1C || true
COPY api-gateway/src ./api-gateway/src
RUN --mount=type=cache,target=/root/.m2 \
    cd api-gateway && mvn clean package -DskipTests -T 1C

FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

EXPOSE 29000

ENTRYPOINT ["java", "-jar", "app.jar"]

