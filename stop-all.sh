#!/bin/bash
INFRA_DIR="infrastructure"
API_GATEWAY_DIR="api-gateway"
SERVICES_DIR="services"

SERVICE_SUBDIRS=(
    "user-service"
    "restaurant-service"
    "menu-service"
    "booking-service"
    "payment-service"
    "order-service"
    "pos-service"
    "review-service"
    "analytics-service"
    "notification-service"
)

stop_compose() {
    local dir=$1
    if [ -f "$dir/docker-compose.yml" ] || [ -f "$dir/docker-compose.yaml" ]; then
        echo "Stopping services in $dir"
        (cd "$dir" && docker compose down)
    fi
}

stop_compose "$INFRA_DIR"
stop_compose "$API_GATEWAY_DIR"

for subdir in "${SERVICE_SUBDIRS[@]}"; do
    service_path="$SERVICES_DIR/$subdir"
    if [ -d "$service_path" ]; then
        stop_compose "$service_path"
    fi
done

echo "All services stopped."