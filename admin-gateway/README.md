# Admin Gateway

## Overview

Admin Gateway is a thin Backend-for-Frontend (BFF) layer for the Admin Panel. It acts as a proxy between the Admin UI and the admin-service.

## Architecture

```
Admin UI → api-gateway → admin-gateway (BFF) → admin-service (business logic) → domain services
```

## Responsibilities

- ✅ Request routing to admin-service (all requests proxied)
- ✅ Simple data aggregation when needed
- ✅ CORS configuration
- ✅ Stateless operation (no database needed)

## What it does NOT do

- ❌ Business logic (stays in admin-service)
- ❌ Authentication (handled by admin-service)
- ❌ Data persistence (completely stateless)
- ❌ JWT validation (admin-service handles it)

## Configuration

### Environment Variables

- `ADMIN_GATEWAY_DB_URL`: PostgreSQL connection URL
- `ADMIN_GATEWAY_DB_USER`: Database username
- `ADMIN_GATEWAY_DB_PASSWORD`: Database password
- `ADMIN_SERVICE_URL`: URL of admin-service (default: http://admin-service)
- `ADMIN_JWT_SECRET`: JWT secret for admin tokens
- `ADMIN_JWT_EXPIRATION`: JWT expiration in milliseconds (default: 86400000 = 24 hours)
- `REDIS_HOST`: Redis host (for token blacklist)
- `REDIS_PORT`: Redis port
- `EUREKA_SERVER_URL`: Eureka server URL

## Running

```bash
mvn spring-boot:run
```

The service will start on port 28080 by default.

## Endpoints

All endpoints are proxied to admin-service:

- `POST /api/admin/auth/login` - Proxied to admin-service
- `POST /api/admin/auth/logout` - Proxied to admin-service
- `GET /api/admin/**` - Proxied to admin-service
- `POST /api/admin/**` - Proxied to admin-service
- `PUT /api/admin/**` - Proxied to admin-service
- `DELETE /api/admin/**` - Proxied to admin-service

## Architecture

The gateway is **completely stateless**:
- ✅ No database connection required
- ✅ No JPA/database dependencies
- ✅ All authentication handled by admin-service
- ✅ All business logic in admin-service
- ✅ Gateway just routes requests

