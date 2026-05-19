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
echo -e "${GREEN}  TutTime - Starting All Services${NC}"
echo -e "${GREEN}========================================${NC}"

# ------------------------------------------------------------
# 1. Define directories
# ------------------------------------------------------------
INFRA_DIR="${PROJECT_ROOT}/infrastructure"
API_GATEWAY_DIR="${PROJECT_ROOT}/api-gateway"
SERVICES_DIR="${PROJECT_ROOT}/services"

# ------------------------------------------------------------
# 2. Helper: start docker-compose in a directory
# ------------------------------------------------------------
start_compose() {
    local dir=$1
    if [ -f "$dir/docker-compose.yml" ] || [ -f "$dir/docker-compose.yaml" ]; then
        echo -e "${YELLOW}Starting services in $dir...${NC}"
        (cd "$dir" && docker compose up -d --build)
    else
        echo -e "${RED}No docker-compose.yml found in $dir, skipping.${NC}"
    fi
}

# ------------------------------------------------------------
# 3. Helper: wait for a container to be ready (running & healthy)
# ------------------------------------------------------------
wait_for_container() {
    local name=$1
    local max_attempts=150          # 5 minutes (2s * 150)
    local attempt=1
    echo -n "Waiting for $name"
    while [ $attempt -le $max_attempts ]; do
        state=$(docker inspect --format='{{.State.Status}}' "$name" 2>/dev/null || echo "none")
        health=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "none")
        if [ "$state" = "running" ]; then
            if [ "$health" = "healthy" ] || [ "$health" = "none" ]; then
                echo -e " ${GREEN}✓ (ready)${NC}"
                return 0
            elif [ "$health" = "starting" ]; then
                echo -n "."
            else
                echo -n "."
            fi
        elif [ "$state" = "created" ] || [ "$state" = "restarting" ]; then
            echo -n "."
        else
            echo -n "."
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    echo -e " ${RED}✗ (timeout)${NC}"
    return 1   # don't abort the whole script
}

# ------------------------------------------------------------
# 4. Start infrastructure (Redis, Kafka, etc.)
# ------------------------------------------------------------
if [ -d "$INFRA_DIR" ]; then
    start_compose "$INFRA_DIR"
    echo -e "${YELLOW}Waiting for infrastructure containers...${NC}"
    INFRA_CONTAINERS=(
        "tuttime-redis"
        "tuttime-kafka"
        "tuttime-zipkin"
        "tuttime-mongodb"
        "tuttime-opensearch"
        "tuttime-graylog"
    )
    for container in "${INFRA_CONTAINERS[@]}"; do
        wait_for_container "$container" || echo -e "${YELLOW}Container $container not ready, continuing...${NC}"
    done
    echo -e "${YELLOW}Giving Kafka extra time to stabilise...${NC}"
    sleep 15
else
    echo -e "${RED}Infrastructure directory '$INFRA_DIR' not found. Exiting.${NC}"
    exit 1
fi

# ------------------------------------------------------------
# 5. Start API gateway
# ------------------------------------------------------------
if [ -d "$API_GATEWAY_DIR" ]; then
    start_compose "$API_GATEWAY_DIR"
    echo -n "Checking API gateway health endpoint..."
    max=30
    count=0
    while [ $count -lt $max ]; do
        if curl -s -f http://localhost:8080/actuator/health &>/dev/null; then
            echo -e " ${GREEN}✓${NC}"
            break
        fi
        echo -n "."
        sleep 2
        count=$((count+1))
    done
    if [ $count -eq $max ]; then echo -e " ${YELLOW}API gateway not responding, continuing${NC}"; fi
fi

# ------------------------------------------------------------
# 6. Start microservices sequentially with health checks
# ------------------------------------------------------------
if [ ! -d "$SERVICES_DIR" ]; then
    echo -e "${RED}Services directory '$SERVICES_DIR' not found. Exiting.${NC}"
    exit 1
fi

echo -e "${YELLOW}Starting microservices in dependency order, waiting for each to be ready...${NC}"

# Map service subdirectory -> expected container name
declare -A SERVICE_CONTAINER_MAP=(
    ["user-service"]="tuttime-user-service"
    ["restaurant-service"]="tuttime-restaurant-service"
    ["menu-service"]="tuttime-menu-service"
    ["review-service"]="tuttime-review-service"
    ["booking-service"]="tuttime-booking-service"
    ["payment-service"]="tuttime-payment-service"
    ["order-service"]="tuttime-order-service"
    ["pos-service"]="tuttime-pos-service"
    ["notification-service"]="tuttime-notification-service"
    ["analytics-service"]="tuttime-analytics-service"
)

SERVICE_ORDER=(
    "user-service"
    "restaurant-service"
    "menu-service"
    "review-service"
    "booking-service"
    "payment-service"
    "order-service"
    "pos-service"
    "notification-service"
    "analytics-service"
)

for subdir in "${SERVICE_ORDER[@]}"; do
    service_path="$SERVICES_DIR/$subdir"
    container_name="${SERVICE_CONTAINER_MAP[$subdir]}"

    if [ -d "$service_path" ]; then
        start_compose "$service_path"

        if [ -n "$container_name" ]; then
            wait_for_container "$container_name" || echo -e "${YELLOW}Service $subdir not fully ready, continuing anyway...${NC}"
        else
            echo -e "${YELLOW}No container name defined for $subdir, waiting 10 seconds instead...${NC}"
            sleep 10
        fi
        # Small delay to allow service to settle after container becomes ready
        sleep 3
    else
        echo -e "${RED}Service directory $service_path not found, skipping.${NC}"
    fi
done

# ------------------------------------------------------------
# 7. Final status
# ------------------------------------------------------------
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All services started!${NC}"
echo ""
echo "To view logs: cd <service-dir> && docker compose logs -f"
echo "To stop all: ./stop-all.sh"