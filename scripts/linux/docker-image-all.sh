#!/bin/bash
set -e

# Determine the script directory and the project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  TutTime - Build All Docker Images${NC}"
echo -e "${GREEN}========================================${NC}"

# List of services and their expected image tags
SERVICES=(
    "api-gateway"
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

# Function to find Dockerfile in a service directory (using absolute paths)
find_dockerfile() {
    local service=$1
    local base_path

    if [ "$service" = "api-gateway" ]; then
        base_path="${PROJECT_ROOT}/api-gateway"
    else
        base_path="${PROJECT_ROOT}/services/$service"
    fi

    if [ -f "$base_path/Dockerfile" ]; then
        echo "$base_path/Dockerfile"
    elif [ -f "$base_path/docker/Dockerfile" ]; then
        echo "$base_path/docker/Dockerfile"
    else
        echo ""
    fi
}

for svc in "${SERVICES[@]}"; do
    dockerfile=$(find_dockerfile "$svc")
    if [ -z "$dockerfile" ]; then
        echo -e "${RED}No Dockerfile found for $svc (searched ${PROJECT_ROOT}/services/$svc/Dockerfile and ${PROJECT_ROOT}/services/$svc/docker/Dockerfile). Skipping.${NC}"
        continue
    fi

    echo -e "${YELLOW}Building tuttime/${svc}:v1 from $dockerfile (context: ${PROJECT_ROOT})${NC}"
    # Use the project root as the build context so that relative paths inside Dockerfile (e.g., copying common files) work
    docker build -f "$dockerfile" -t "tuttime/${svc}:v1" "${PROJECT_ROOT}"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully built tuttime/${svc}:v1${NC}"
    else
        echo -e "${RED}Failed to build tuttime/${svc}:v1${NC}"
        exit 1
    fi
done

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All images built successfully!${NC}"