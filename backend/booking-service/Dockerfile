# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copy parent POM from parent dir
COPY ../pom.xml pom.xml

# 2. STRIP MODULES: This prevents Maven from looking for missing folders
RUN sed -i '/<modules>/,/<\/modules>/d' pom.xml

# 3. Copy only the specific service folder
COPY booking-service/ booking-service/

# 4. Build only this project
RUN mvn -f booking-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Note the path change here to match where Maven puts the jar
COPY --from=build /app/booking-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
