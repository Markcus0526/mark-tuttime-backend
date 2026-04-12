# TutTime Restaurant Service

## Description
Manages restaurants, branches, zones, dishTables, media.

## Running the Service
```bash
cd services/restaurant-service
mvn spring-boot:run
```

## Testing
```bash
mvn test
```

Reports in `target/surefire-reports/`.

## Coverage
```bash
mvn jacoco:report
```
Open `target/site/jacoco/index.html`.

## API Docs
Open http://localhost:8080/swagger-ui.html
