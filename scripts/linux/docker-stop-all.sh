#!/bin/bash
set -e

# Get the directory where this script resides
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Project root is two levels up (script/linux -> script -> root)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  TutTime - Stopping All Services${NC}"
echo -e "${GREEN}========================================${NC}"

INFRA_DIR="${PROJECT_ROOT}/infrastructure"
API_GATEWAY_DIR="${PROJECT_ROOT}/api-gateway"
SERVICES_DIR="${PROJECT_ROOT}/services"

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