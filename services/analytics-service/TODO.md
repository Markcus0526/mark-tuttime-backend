# Analytics Service TODO - Created by BLACKBOXAI

## Critical (for compilation) - COMPLETED

1. [x] Add lombok dependency to pom.xml
2. [x] Run `mvn clean compile` from backend/analytics-service/ to verify

## Functional Improvements

3. [ ] Fix AnalyticsService.handleEvent(): set event.timestamp = LocalDateTime.now()
4. [ ] Implement real logic in AnalyticsController.getOccupancy() using EventRepository
5. [ ] Ensure ObjectMapper bean is properly configured
6. [ ] Add validation (@Valid) and error handling
7. [ ] Add more endpoints (e.g., /metrics/{restaurantId}, /forecast)

## Testing & Polish

8. [ ] Add unit/integration tests
9. [ ] Add Swagger docs annotations
10. [ ] Test Kafka event consumption end-to-end
11. [ ] Add metrics/forecasting logic (e.g., avg prep time from events)

