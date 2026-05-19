#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

CONTAINER_NAME="swagger-ui"
IMAGE="swaggerapi/swagger-ui"
HOST_PORT=18080

# Use localhost because we're on the same machine as the gateway
GATEWAY_URL="http://localhost:8080"

URLS_JSON='[
    {"name": "User Service", "url": "'"$GATEWAY_URL"'/user-docs/v3/api-docs"},
    {"name": "Restaurant Service", "url": "'"$GATEWAY_URL"'/restaurant-docs/v3/api-docs"},
    {"name": "Menu Service", "url": "'"$GATEWAY_URL"'/menu-docs/v3/api-docs"},
    {"name": "Booking Service", "url": "'"$GATEWAY_URL"'/booking-docs/v3/api-docs"},
    {"name": "Payment Service", "url": "'"$GATEWAY_URL"'/payment-docs/v3/api-docs"},
    {"name": "Order Service", "url": "'"$GATEWAY_URL"'/order-docs/v3/api-docs"},
    {"name": "POS Service", "url": "'"$GATEWAY_URL"'/pos-docs/v3/api-docs"},
    {"name": "Review Service", "url": "'"$GATEWAY_URL"'/review-docs/v3/api-docs"},
    {"name": "Analytics Service", "url": "'"$GATEWAY_URL"'/analytics-docs/v3/api-docs"},
    {"name": "Notification Service", "url": "'"$GATEWAY_URL"'/notification-docs/v3/api-docs"}
]'

if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${YELLOW}Removing existing container...${NC}"
    docker stop "${CONTAINER_NAME}" >/dev/null 2>&1
    docker rm "${CONTAINER_NAME}" >/dev/null 2>&1
fi

echo -e "${GREEN}Starting Swagger UI container on port ${HOST_PORT}...${NC}"
docker run -d \
    --name "${CONTAINER_NAME}" \
    -p "${HOST_PORT}:8080" \
    -e URLS="${URLS_JSON}" \
    --restart unless-stopped \
    "${IMAGE}"

echo -e "${GREEN}Swagger UI running at http://localhost:${HOST_PORT}${NC}"
echo -e "${YELLOW}IMPORTANT: If you still see 'api-gateway' in API call URLs, you need to update each microservice's OpenApiConfig to set the server URL to '${GATEWAY_URL}'. Otherwise, manually change the server dropdown in Swagger UI.${NC}"