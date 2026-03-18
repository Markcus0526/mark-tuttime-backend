# TutTime Backend Development Master TODO
Generated from approved workflow plan. Updated after each completed step.

## Phase 1: Compilation Fixes [IN PROGRESS]
### 1.1 booking-service (10 steps - Current: Starting edits...)
- [ ] 1. Update pom.xml: add openfeign dependency
- [ ] 2. Update BookingRepository.java: add countByBranchIdAndBookingTimeBetween query
- [ ] 3. Fix BookingController.java: add imports, @RequestBody, complete getAvailability
- [ ] 4. Fix AvailabilityService.java: add missing imports
- [ ] 5. Fix BookingService.java: add missing imports (LocalDateTime, Duration, Map)
- [ ] 6. Fix AutoCancelScheduler.java: add BookingRepository import
- [ ] 7. Fix BookingResponse.java: add LocalDateTime import
- [ ] 8. Fix entities (Booking.java, Order.java): add @Builder.Default
- [ ] 9. mvn clean compile
- [ ] 10. mvn spring-boot:run (test startup)

### 1.2 restaurant-service (Current: Starting Step 1)
- [ ] 1. Update pom.xml with maven-compiler-plugin for Lombok annotation processing
- [ ] 2. Fix RestaurantService.java - add missing Table import
- [ ] 3. Fix entity files: add @Builder.Default / Lombok annotations
- [ ] 4. Clean minor cast in RestaurantController.java
- [ ] 5. cd services/restaurant-service && mvn clean compile
- [ ] 6. mvn clean package

### 1.3 All services verification
- [ ] Root: mvn clean compile (multi-module)
- [ ] payment-service: Create missing src/ structure

## Phase 2: Core Functionality & Integration [PENDING]
- [ ] Verify auth-service JWT
- [ ] Complete restaurant table availability
- [ ] Implement booking availability/locks/scheduler
- [ ] menu-service CRUD
- [ ] docker-compose up E2E test

## Phase 3: Advanced Features [PENDING]
- [ ] integration-service: POS connectors/scheduling
- [ ] analytics-service: Kafka metrics
- [ ] review/payment completion
- [ ] Gateway routes/auth

## Phase 4: Testing & Polish [PENDING]
- [ ] Unit/integration tests
- [ ] Kafka E2E
- [ ] OpenAPI

## Phase 5: Production [PENDING]
- [ ] Monitoring/CI/CD

**Next Action: Start booking-service pom.xml update (Phase 1.1 Step 1)**
