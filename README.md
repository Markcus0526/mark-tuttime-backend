# TutTime Backend Microservices

## Architecture
Microservices per requirements:
- **api-gateway** (8080): Spring Cloud Gateway, routes to services
- **restaurant-service** (8081): Restaurants, branches, zones, tables
- **menu-service** (8082): Menus, categories, items, modifiers, stop lists
- **booking-service** (8083): Reservations, table locks (Redis), slots
- **order-service** (8084): Pre-orders, statuses, kitchen sync
- **payment-service** (8085): Payments, refunds, deposits
- **pos-service** (8086): iiko/R-Keeper connectors
- **review-service** (8087): Reviews, ratings
- **analytics-service** (8088): Events, metrics, occupancy forecasting

## Prerequisites
- Java 17+, Maven
- Docker & Docker Compose (infra)

## Quick Start
```bash
# 1. Start infra (DB, Redis, Kafka, Eureka)
docker-compose up -d

# 2. Build & run services
cd backend/{service-name}
mvn spring-boot:run
# Or all with scripts/docker

# 3. Test via Gateway: http://localhost:8080/restaurants/, /menus/, etc.
```

## Full Setup
See root README.md. Run `docker-compose up -d` first.

**Note**: Package mismatch errors in VSCode? Refresh workspace or fix dir structure: move Java files to src/main/java/com/tuttime/{service}/ (create dirs manually if needed).

