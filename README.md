# tuttime-restaurant-preorder-booking-platform

Structure and Key Sections:

Introduction & Core Idea (🚀 The Idea):

Describes TutTime as a mobile platform for table reservations + menu pre-orders + dynamic off-peak discounts.
Integrates with POS systems (iiko, R-Keeper).
Value for guests: Guaranteed seating, pre-set table, discounts.
Value for restaurants: Fill dead hours, predictable revenue, reduce no-shows.
Market Problem & Solution (💡 Market Problem, 🔥 Solution):

Problems: Under-occupancy (30-50%), no-shows, wait times, poor discount integration.
Solution: Yield management like airlines – dynamic pricing/discounts for dead hours, prepayment.
Business Model & Scalability (📊 Business Model, 📈 Why it's scalable):

Revenue: 5-12% commission, subscriptions, promotions.
Unit economics example: 250-400₽ per reservation.
Scalable due to SaaS, no logistics, POS integrations.
Competitive Advantage & Vision (🧠 Competitive advantage, 🎯 Vision):

Not just booking/discounts – full demand management with pre-order/prepay.
Long-term: AI forecasting, loyalty, white-label.
Investment Thesis (💰 Investment thesis): High-margin SaaS + marketplace.

UI/UX Screens (Implicit section starting [3/17/2026 7:53 AM]):

Client app: Restaurant card, menu, cart, booking flow, my reservations.
Restaurant web: Dashboard, orders, seating, menu, analytics.
Admin panel: Moderation, disputes, etc.
Design rules: Fast flow (60s), highlight discounts/readiness/cancellation.
Technical Specifications (3) Technical Specifications for Developers):

Architecture:
Component	Details
Clients	iOS/Android (Flutter/React Native/native), Restaurant web (React/Vue), Admin panel
Backend Services	API Gateway, Auth, Restaurant, Menu, Booking, Order, Payment, Integration, Review, Analytics
Storage	PostgreSQL, Redis, S3, Kafka/RabbitMQ
Entities: User, Restaurant, Branch, Zone, Table, Schedule, DiscountRule, MenuCategory, MenuItem, Modifier, Booking, Order, Payment, Refund, Review, etc.
Booking Logic: Table-based or slot-based; soft locks in Redis; time rules (lead/prep/grace periods).
Order Statuses: CREATED → PAID → SENT_TO_RESTAURANT → ... → REFUNDED.
Payments: Apple Pay/cards, full/deposit options, configurable refunds.
POS Integrations (iiko/R-Keeper): Menu sync, orders, table mapping, webhooks/polling.
Admin/Moderation: Disputes, bans.
Insights & Relevance to Project:

This is the product requirements document (PRD) for the entire TutTime platform.
Current project structure (from env): Backend is Spring Boot Java (pom.xml, entities like MenuItem.java, Restaurant.java, Booking.java, TuttimeApplication.java), aligning with entities here (e.g., MenuItem, Restaurant, Booking).
Gaps/Implementation Status: Spec suggests full-stack (mobile/web/admin), but project has backend + admin-panel/client-app/rest-web dirs. Backend entities match spec partially.
Actionable: Use as blueprint for features like dynamic discounts, POS integrations, booking flow.
Potential Next Steps: Compare against backend entities/pom.xml to check implementation coverage. Actionable: Use as blueprint for features like dynamic discounts, POS integrations, booking flow.

## Developer Guide

Updated based on analysis of `docs/requires.txt` (full PRD).

### Project Structure
```
.
├── backend/          # Spring Boot Java API (core services: Restaurant, Menu, Booking, etc.)
├── admin-panel/      # Admin dashboard for moderation/disputes
├── client-app/       # Mobile app (iOS/Android - Flutter/React Native)
├── rest-web/         # Restaurant web dashboard
├── docs/             # PRD, specs (requires.txt)
├── README.md         # This file
└── TODO.md           # Task tracking
```

### Tech Stack (from requires.txt + project)
| Layer | Technologies |
|-------|--------------|
| Backend | Microservices: API Gateway, Restaurant service, Menu service, Booking service, Order service, Payment service, Integration service, Review service, Analytics/Event service (all Spring Boot/Java 17+, Maven) |
| Database | PostgreSQL (master data/entities), Redis (caches/locks) |
| Messaging | Kafka, RabbitMQ (async events, order statuses, POS sync) |
| Storage | S3-compatible (images) |
| Frontend | React/Vue (rest-web), Flutter/React Native (client-app) |
| Integrations | iiko/R-Keeper APIs (menu/orders/tables), Payment gateways (cards/Apple Pay) |

**Key Entities** (per requires.txt, implement across services): User, Restaurant, Branch, Zone/Table, Schedule, DiscountRule, MenuCategory/Item/Modifier, Booking, Order, Payment/Refund, Review.

### Prerequisites & Installation
1. **Java 17+**, Maven 3.6+, Node.js (for frontends), PostgreSQL 15+, Redis 7+.
2. Clone: `git clone <repo> && cd tuttime-restaurant-preorder-booking-platform`
3. Microservices (docker-compose recommended):
   ```
   # Build all services
   mvn clean install -DskipTests
   
   # Or per service, e.g.
   cd backend/api-gateway && mvn clean install
   cd ../restaurant-service && mvn clean install
   # ... (menu-service, booking-service, etc.)
   ```
4. Database:
   - Create DB: `createdb tuttime`
   - Update `backend/src/main/resources/application.properties`: DB URL, Redis host.
   - Run migrations: `mvn flyway:migrate` (if setup).
5. Frontends:
   ```
   cd admin-panel && npm install
   cd ../client-app && npm install  # or flutter pub get
   cd ../rest-web && npm install
   ```

### Running Locally
1. **Microservices** (with docker-compose up or individually):
   ```
   # API Gateway: cd backend/api-gateway && mvn spring-boot:run (e.g. :8080)
   # Restaurant service: cd backend/restaurant-service && mvn spring-boot:run (e.g. :8081)
   # Menu service: cd backend/menu-service && mvn spring-boot:run (e.g. :8082)
   # ... adjust ports per service
   ```
2. **Admin Panel**: `cd admin-panel && npm start` (http://localhost:3001)
3. **Client App**: 
   - Web preview: `cd client-app && npm start`
   - Native: Flutter `flutter run` or React Native `npx react-native start`
4. **Restaurant Web**: `cd rest-web && npm start` (http://localhost:3000)
5. Test API: Use Postman/Swagger (`/swagger-ui.html`).

**Notes**: Seed DB with test data. Redis required for booking locks. POS mocks needed for integrations.

### Key Implementation Notes (from requires.txt)
- **Booking Logic**: Table soft-lock (Redis, 5min), slot capacity, time rules (30-60min lead time).
- **Order Flow**: CREATED → PAID → SENT_TO_RESTAURANT → READY → SERVED.
- **Discounts**: Dynamic per "dead hours", configurable %/tables/dishes.
- **POS Sync**: Webhooks/polling for menu/orders/tables.
- **Payments**: Prepay/deposit, refunds (full/partial based on cancel time).

**TODOs/Gaps**:
- Implement full services (Payment, Integration).
- Mobile app completion.
- AI forecasting (future).

### Development Roadmap
1. Backend: Complete entities/services per requires.txt.
2. POS Connectors: iiko/R-Keeper modules.
3. Frontend: Full flows (cart, payment, dashboard).
4. Deploy: Docker/K8s, CI/CD.
5. Analytics/Reviews.

See `docs/requires.txt` for full specs. Contributions welcome!
