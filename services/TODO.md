# Docker Compose Fix Progress

## Plan Steps (Approved)
- [x] Create TODO.md with steps
- [x] Step 1: Edit backend/docker-compose.yml - Update all microservice build contexts to service-specific (`context: ./<service>`)
- [x] Step 2: Update all 10 Dockerfiles - Generic template: COPY ../pom.xml, COPY ., mvn -am
- [ ] Step 3: docker compose down && docker compose up --build -d
- [ ] Step 4: docker compose ps && logs
- [ ] Complete
