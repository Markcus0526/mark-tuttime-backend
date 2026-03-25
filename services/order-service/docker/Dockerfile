# syntax=docker/dockerfile:1.4

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./pom.xml

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

# Build order-service
# Re-install parent POM to ensure it's in local repository (required for order-service's empty relativePath)
RUN --mount=type=cache,target=/root/.m2 \
    mvn install -N -DskipTests -T 1C && \
    ls -la /root/.m2/repository/com/tuttime/tuttime-be/1.0-SNAPSHOT/ || echo "Parent POM install verification failed"
COPY services/order-service/pom.xml ./services/order-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl services/order-service -am -T 1C || true
COPY services/order-service/src ./services/order-service/src
# Build from the module directory - parent POM should be in local repo due to empty relativePath
RUN --mount=type=cache,target=/root/.m2 \
    cd services/order-service && mvn clean package -DskipTests -T 1C

FROM eclipse-temurin:21-jre-jammy AS runner

WORKDIR /app

COPY --from=builder /app/services/order-service/target/order-service-*.jar app.jar

EXPOSE 8001 29191

ENTRYPOINT ["java", "-jar", "app.jar"]

