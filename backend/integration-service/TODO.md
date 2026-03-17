# Integration Service TODO

## Current Status: Compilation Fixes In Progress

### 1. [COMPLETE ✅] Fix compilation errors
   - [x] Update pom.xml with missing dependencies (lombok, eureka-client)
   - [x] Fix missing imports in Java files (EnableDiscoveryClient, EnableScheduling, List, LocalDateTime, WebClient)
   - [x] Verify all repos/services imports
   - [x] Run `mvn clean compile` successfully

### 2. [PENDING] Post-compilation improvements
   - [ ] Add @Scheduled implementation in IntegrationService for menu polling
   - [ ] Implement real iiko API endpoints (menu fetch, order send, table status)
   - [ ] Implement real R-Keeper API endpoints
   - [ ] Add Kafka producers for menu updates to menu-service
   - [ ] Add DLQ handling for failed syncs (Kafka dead-letter topic)
   - [ ] Add integration tests for connectors
   - [ ] Add OpenAPI docs for /api/v1/integration endpoints
   - [ ] Docker build and test

### 3. [FUTURE] Features
   - Table occupancy sync
   - Real-time order status polling
   - Multi-tenant support
   - Webhook support from POS systems

**Priority: Get compilation working first**

