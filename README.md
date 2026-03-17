# tuttime-restaurant-preorder-booking-platform

## 🚀 The Idea
TutTime is a mobile platform for **table reservations + menu pre-orders + dynamic off-peak discounts**.
Integrates with POS systems (**iiko**, **R-Keeper**).

**For guests**:
- Guaranteed seating, pre-set table
- Discounts 30-70% on dead hours
- Pre-order meals/drinks

**For restaurants**:
- Fill 30-50% under-occupancy
- Predictable revenue, no empty tables
- Reduce no-shows via prepayment

## 💡 Market Problem
- Restaurants: 30-50% empty tables in off-peak, no-shows 20-40%
- Guests: long waits, poor discounts, no pre-order option
- Market: $10B+ (delivery + dine-in bookings), growing 25%/year

## 🔥 Solution
**Yield management** like airlines/hotels:
- Dynamic pricing/discounts for 'dead hours'
- Prepayment + pre-order reduces no-shows 80%
- POS integration: real-time menu/tables/orders

## 📊 Business Model
- **Commission**: 5-12% from prepayments/pre-orders
- **Subscription**: 5-15k₽/month per restaurant
- **Promotions**: featured placements

**Unit economics**:
- Reservation: 1000₽ check → 50₽ commission
- Monthly: 20 restaurants × 100 res × 50₽ = 100k₽ MRR

## 🧠 Competitive Advantage
- Not just booking/discounts – **full demand management**
- Pre-order + prepay + POS sync
- AI forecasting (future)

## 🎯 Vision
1. **Year 1**: MVP Moscow restaurants
2. **Year 2**: 1000 restaurants, POS integrations
3. **Year 3+**: Loyalty, white-label, AI forecasting

## 💰 Investment Thesis
High-margin **SaaS + marketplace**:
- LTV/CAC > 5x
- 70% margins
- Scalable (no logistics/cold chain)

---

## UI/UX Screens
**[Detailed wireframes/specs in docs/requires.txt]**

**Client App** (iOS/Android):
1. Restaurant card → Discounts → Menu
2. Cart → Select table/time → Pay deposit
3. My reservations → Status → Arrived

**Restaurant Web**:
1. Dashboard: Occupancy, orders, revenue
2. Orders → Kitchen print → Status update
3. Seating map → Menu editor → Analytics

**Admin Panel**:
1. Moderation (disputes, bans)
2. Analytics, payments

**Design Rules**:
- Flow < 60 seconds
- Highlight: discount %, readiness, cancel policy
- Visual: table map, kitchen status

---

## Technical Specifications for Developers
### Architecture
| Component | Details |
|-----------|---------|
| **Clients** | iOS/Android (Flutter/React Native), Restaurant web (React/Vue), Admin panel |
| **Backend** | **Microservices**: API Gateway + 10 services (Spring Boot/Java 17, Maven, Eureka, Kafka, Postgres/Redis) |
| **Storage** | PostgreSQL (entities), Redis (locks/caches), S3 (images), Kafka (events) |

**Key Entities**:
- User, Restaurant, Branch, Zone/Table, Schedule, DiscountRule
- MenuCategory/Item/Modifier/StopList
- Booking, Order, Payment/Refund, Review

### Backend Services Overview
| Service | Port | Description | Gateway Path Prefix |
|---------|------|-------------|---------------------|
| api-gateway | 8080 | Spring Cloud Gateway | / (all) |
| restaurant-service | 8081 | Restaurants, branches, tables | /restaurants/** |
| menu-service | 8082 | Menus, items | /menus/** |
| booking-service | 8083 | Reservations | /bookings/** |
| auth-service | 8084 | JWT auth | /auth/** |
| order-service | 8085 | Pre-orders | /orders/** |
| payment-service | 8086 | Payments | /payments/** |
| integration-service | 8087 | POS sync | /integrations/** |
| review-service | 8088 | Reviews | /reviews/** |
| analytics-service | 8089 | Metrics | /analytics/** |

### Full OpenAPI / Swagger UI Docs
✅ **SpringDoc OpenAPI enabled on ALL services** v2.6.0.

**Access interactive docs via API Gateway** (http://localhost:8080):
- Restaurant API: http://localhost:8080/restaurants/swagger-ui.html
- Menu API: http://localhost:8080/menus/swagger-ui.html
- Auth API: http://localhost:8080/auth/swagger-ui.html
- Booking API: http://localhost:8080/bookings/swagger-ui.html
- Orders API: http://localhost:8080/orders/swagger-ui.html
- Payments API: http://localhost:8080/payments/swagger-ui.html
- Reviews API: http://localhost:8080/reviews/swagger-ui.html
- Analytics API: http://localhost:8080/analytics/swagger-ui.html
- Integration API: http://localhost:8080/integrations/swagger-ui.html

**OpenAPI JSON**:
- http://localhost:8080/<service>/v3/api-docs (e.g., /restaurants/v3/api-docs)

**Direct service access** (for debugging):
- http://localhost:8081/swagger-ui.html (restaurant-service, etc.)

Gateway proxies **all paths** including Swagger endpoints.

### Detailed API Endpoints (Parsed from Controllers)
**All via gateway: http://localhost:8080/{service}/{endpoint}**

| Service | Endpoint | Method | Description |
|---------|----------|--------|-------------|
| restaurant | restaurants/api/v1/tables/available | GET | Available tables |
| menu | menus/api/v1/items | POST | Create item |
| booking | bookings/api/v1/avail/{branchId} | GET | Availability |
| auth | auth/api/v1/register | POST | Register |
| auth | auth/api/v1/authenticate | POST | Login |

**Note**: Controllers need @Operation annotations for rich docs. Current shows endpoints/actuator.

## Developer Guide
### Project Structure
```
.
├── backend/                    # Spring Boot Microservices
│   ├── pom.xml                 # Parent
│   ├── api-gateway/            # 8080
│   ├── auth-service/           # etc.
│   └── docker-compose.yml
├── admin-panel/
├── client-app/
├── rest-web/
├── docs/
├── README.md
└── TODO.md
```

### Prerequisites
Java 17+, Maven, Docker Compose, Postgres, Redis

### Installation & Run
```bash
cd backend
mvn clean install -DskipTests
docker-compose up -d  # Eureka(8761), services(8080-89)
```

**Verify Swagger**:
```bash
curl -I http://localhost:8080/restaurants/swagger-ui.html  # 200 OK
```

**Eureka dashboard**: http://localhost:8761

## Roadmap / TODO.md
Backend flows, POS, frontends.

**Contributions welcome!** 🚀
