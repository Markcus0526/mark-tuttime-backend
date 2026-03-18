#!/bin/bash
# Build and Push Docker Images - Linux Bash Script
# Usage: ./build-images.sh [service-name|gateway-name|all|main] [--no-cache] [--dev]
# Examples:
#   ./build-images.sh user-service
#   ./build-images.sh admin-service
#   ./build-images.sh api-gateway
#   ./build-images.sh admin-gateway
#   ./build-images.sh all
#   ./build-images.sh main
#   ./build-images.sh user-service --no-cache
#   ./build-images.sh all --no-cache
#   ./build-images.sh user-service --dev
#   ./build-images.sh all --dev
#
# Available services: admin-service, booking-integration-service, content-service, conversion-service,
#                     messaging-service, mining-service, notification-service, rewards-service, social-service,
#                     staking-service, token-service, user-service
# Available gateways: admin-gateway, api-gateway
# Special options: all (builds all services and gateways), main (builds main services: user-service, rewards-service, notification-service, mining-service, content-service, social-service, booking-integration-service, messaging-service, api-gateway, admin-service, admin-gateway)
# Options: --no-cache (builds without using Docker cache), --dev (builds for k3s dev cluster with dev image tags)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$(cd "$SCRIPT_DIR/.." && pwd)/.env"

REGISTRY=""
USERNAME=""
PASSWORD=""

if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        if [[ $line =~ ^DOCKER_REGISTRY=(.+)$ ]]; then
            REGISTRY="${BASH_REMATCH[1]}"
            REGISTRY=$(echo "$REGISTRY" | xargs)
        fi
        if [[ $line =~ ^DOCKER_USERNAME=(.+)$ ]]; then
            USERNAME="${BASH_REMATCH[1]}"
            USERNAME=$(echo "$USERNAME" | xargs)
        fi
        if [[ $line =~ ^DOCKER_PASSWORD=***REMOVED*** ]]; then
            PASSWORD="${BASH_REMATCH[1]}"
            PASSWORD=$(echo "$PASSWORD" | xargs)
        fi
    done < "$ENV_FILE"
fi

if [ -z "$REGISTRY" ] || [ -z "$USERNAME" ] || [ -z "$PASSWORD" ]; then
    echo -e "\033[0;31mError: Missing Docker registry credentials in scripts/.env\033[0m"
    echo -e "\033[0;33mRequired: DOCKER_REGISTRY, DOCKER_USERNAME, DOCKER_PASSWORD\033[0m"
    exit 1
fi

# Parse command line arguments
NO_CACHE=false
DEV=false
SERVICE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        --dev)
            DEV=true
            shift
            ;;
        *)
            if [ -z "$SERVICE" ]; then
                SERVICE="$1"
            fi
            shift
            ;;
    esac
done

# Enable BuildKit
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo -e "\033[0;36m========================================\033[0m"
echo -e "\033[0;36mDocker Image Build Script (Linux)\033[0m"
echo -e "\033[0;36m========================================\033[0m"
if [ "$DEV" = true ]; then
    echo -e "\033[0;33mMode: DEV (k3s dev cluster)\033[0m"
else
    echo -e "\033[0;32mMode: PROD (k8s prod cluster)\033[0m"
fi
echo ""

echo -e "\033[0;33mLogging in to GitHub Container Registry...\033[0m"
echo "$PASSWORD" | docker login "$REGISTRY" -u "$USERNAME" --password-stdin
if [ $? -ne 0 ]; then
    echo -e "\033[0;31mLogin failed!\033[0m"
    exit 1
fi
echo -e "\033[0;32mLogin successful!\033[0m"
echo ""

get_image_name() {
    local BASE_IMAGE_NAME="$1"
    if [ "$DEV" = true ]; then
        echo "${BASE_IMAGE_NAME//:latest/-dev:latest}"
    else
        echo "$BASE_IMAGE_NAME"
    fi
}

build_service() {
    local SERVICE_NAME="$1"
    local SERVICE_DIR="$2"
    local IMAGE_NAME="$3"
    local COMPOSE_SERVICE_NAME="$4"
    
    echo -e "\033[0;36mBuilding $SERVICE_NAME...\033[0m"
    echo -e "\033[0;90m  Service directory: $SERVICE_DIR\033[0m"
    echo -e "\033[0;90m  Image: $IMAGE_NAME\033[0m"
    
    local COMPOSE_FILE="$SERVICE_DIR/docker-compose.yml"
    local ENV_DOCKER_FILE="$SERVICE_DIR/.env.docker"
    
    if [ ! -f "$COMPOSE_FILE" ]; then
        echo -e "\033[0;31mError: docker-compose.yml not found at $COMPOSE_FILE\033[0m"
        return 1
    fi
    
    (
        cd "$SERVICE_DIR"
        BUILD_ARGS=("$COMPOSE_SERVICE_NAME")
        if [ "$NO_CACHE" = true ]; then
            BUILD_ARGS+=("--no-cache")
        fi
        
        if [ -f "$ENV_DOCKER_FILE" ]; then
            docker compose --env-file .env.docker build "${BUILD_ARGS[@]}"
        else
            docker compose build "${BUILD_ARGS[@]}"
        fi
        
        if [ $? -ne 0 ]; then
            echo -e "\033[0;31mBuild failed for $SERVICE_NAME!\033[0m"
            return 1
        fi
        
        local COMPOSE_PROJECT_NAME=$(basename "$SERVICE_DIR")
        local LOCAL_IMAGE_NAME="${COMPOSE_PROJECT_NAME}-${COMPOSE_SERVICE_NAME}:latest"
        docker tag "$LOCAL_IMAGE_NAME" "$IMAGE_NAME"
        
        if [ $? -ne 0 ]; then
            echo -e "\033[0;33mTag failed for $SERVICE_NAME! Trying alternative name...\033[0m"
            local ALT_IMAGE_NAME="${COMPOSE_SERVICE_NAME}-${COMPOSE_SERVICE_NAME}:latest"
            docker tag "$ALT_IMAGE_NAME" "$IMAGE_NAME"
            if [ $? -ne 0 ]; then
                echo -e "\033[0;31mTag failed for $SERVICE_NAME!\033[0m"
                return 1
            fi
        fi
        
        echo -e "\033[0;32mBuild successful!\033[0m"
        
        echo -e "\033[0;33mPushing $SERVICE_NAME...\033[0m"
        docker push "$IMAGE_NAME"
        
        if [ $? -ne 0 ]; then
            echo -e "\033[0;31mPush failed for $SERVICE_NAME!\033[0m"
            return 1
        fi
        
        echo -e "\033[0;32mPush successful!\033[0m"
        echo ""
        return 0
    )
}

# Build services based on parameter
SERVICE="${SERVICE:-all}"
SUCCESS=true

case "${SERVICE,,}" in
    # Services
    "admin-service")
        SERVICE_DIR="$ROOT_DIR/services/admin-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-admin-service:latest")
        build_service \
            "admin-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "admin-service" || SUCCESS=false
        ;;
    "booking-integration-service")
        SERVICE_DIR="$ROOT_DIR/services/booking-integration-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-booking-integration-service:latest")
        build_service \
            "booking-integration-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "booking-integration-service" || SUCCESS=false
        ;;
    "content-service")
        SERVICE_DIR="$ROOT_DIR/services/content-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-content-service:latest")
        build_service \
            "content-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "content-service" || SUCCESS=false
        ;;
    "conversion-service")
        SERVICE_DIR="$ROOT_DIR/services/conversion-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-conversion-service:latest")
        build_service \
            "conversion-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "conversion-service" || SUCCESS=false
        ;;
    "mining-service")
        SERVICE_DIR="$ROOT_DIR/services/mining-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-mining-service:latest")
        build_service \
            "mining-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "mining-service" || SUCCESS=false
        ;;
    "messaging-service")
        SERVICE_DIR="$ROOT_DIR/services/messaging-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-messaging-service:latest")
        build_service \
            "messaging-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "messaging-service" || SUCCESS=false
        ;;
    "notification-service")
        SERVICE_DIR="$ROOT_DIR/services/notification-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-notification-service:latest")
        build_service \
            "notification-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "notification-service" || SUCCESS=false
        ;;
    "rewards-service")
        SERVICE_DIR="$ROOT_DIR/services/rewards-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-rewards-service:latest")
        build_service \
            "rewards-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "rewards-service" || SUCCESS=false
        ;;
    "social-service")
        SERVICE_DIR="$ROOT_DIR/services/social-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-social-service:latest")
        build_service \
            "social-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "social-service" || SUCCESS=false
        ;;
    "staking-service")
        SERVICE_DIR="$ROOT_DIR/services/staking-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-staking-service:latest")
        build_service \
            "staking-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "staking-service" || SUCCESS=false
        ;;
    "token-service")
        SERVICE_DIR="$ROOT_DIR/services/token-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-token-service:latest")
        build_service \
            "token-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "token-service" || SUCCESS=false
        ;;
    "user-service")
        SERVICE_DIR="$ROOT_DIR/services/user-service"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-user-service:latest")
        build_service \
            "user-service" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "user-service" || SUCCESS=false
        ;;
    # Gateways
    "admin-gateway")
        SERVICE_DIR="$ROOT_DIR/admin-gateway"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/tuttime-admin-gateway:latest")
        build_service \
            "admin-gateway" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "admin-gateway" || SUCCESS=false
        ;;
    "api-gateway")
        SERVICE_DIR="$ROOT_DIR/api-gateway"
        IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/apart-token-api-gateway:latest")
        build_service \
            "api-gateway" \
            "$SERVICE_DIR" \
            "$IMAGE_NAME" \
            "api-gateway" || SUCCESS=false
        ;;
    "main")
        echo -e "\033[0;36mBuilding main services and gateways...\033[0m"
        echo ""
        
        # Build main services
        SERVICES=(
            "user-service:services/user-service:apart-token-user-service"
            "rewards-service:services/rewards-service:apart-token-rewards-service"
            "notification-service:services/notification-service:apart-token-notification-service"
            "mining-service:services/mining-service:apart-token-mining-service"
            "content-service:services/content-service:apart-token-content-service"
            "social-service:services/social-service:apart-token-social-service"
            "booking-integration-service:services/booking-integration-service:apart-token-booking-integration-service"
            "messaging-service:services/messaging-service:apart-token-messaging-service"
            "admin-service:services/admin-service:apart-token-admin-service"
        )
        
        # Build main gateways
        GATEWAYS=(
            "api-gateway:api-gateway:apart-token-api-gateway"
            "admin-gateway:admin-gateway:apart-token-admin-gateway"
        )
        
        for svc in "${SERVICES[@]}"; do
            if [ "$SUCCESS" = false ]; then break; fi
            IFS=':' read -r name dir image <<< "$svc"
            SERVICE_DIR="$ROOT_DIR/$dir"
            IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/$image:latest")
            build_service \
                "$name" \
                "$SERVICE_DIR" \
                "$IMAGE_NAME" \
                "$name" || SUCCESS=false
        done
        
        for gw in "${GATEWAYS[@]}"; do
            if [ "$SUCCESS" = false ]; then break; fi
            IFS=':' read -r name dir image <<< "$gw"
            SERVICE_DIR="$ROOT_DIR/$dir"
            IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/$image:latest")
            build_service \
                "$name" \
                "$SERVICE_DIR" \
                "$IMAGE_NAME" \
                "$name" || SUCCESS=false
        done
        ;;
    "all")
        echo -e "\033[0;36mBuilding all services and gateways...\033[0m"
        echo ""
        
        # Build all services
        SERVICES=(
            "user-service:services/user-service:apart-token-user-service"
            "notification-service:services/notification-service:apart-token-notification-service"
            "token-service:services/token-service:apart-token-token-service"
            "staking-service:services/staking-service:apart-token-staking-service"
            "rewards-service:services/rewards-service:apart-token-rewards-service"
            "booking-integration-service:services/booking-integration-service:apart-token-booking-integration-service"
            "mining-service:services/mining-service:apart-token-mining-service"
            "conversion-service:services/conversion-service:apart-token-conversion-service"
            "content-service:services/content-service:apart-token-content-service"
            "social-service:services/social-service:apart-token-social-service"
            "messaging-service:services/messaging-service:apart-token-messaging-service"
            "admin-service:services/admin-service:apart-token-admin-service"
        )
        
        # Build all gateways
        GATEWAYS=(
            "admin-gateway:admin-gateway:apart-token-admin-gateway"
            "api-gateway:api-gateway:apart-token-api-gateway"
        )
        
        for svc in "${SERVICES[@]}"; do
            if [ "$SUCCESS" = false ]; then break; fi
            IFS=':' read -r name dir image <<< "$svc"
            SERVICE_DIR="$ROOT_DIR/$dir"
            IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/$image:latest")
            build_service \
                "$name" \
                "$SERVICE_DIR" \
                "$IMAGE_NAME" \
                "$name" || SUCCESS=false
        done
        
        for gw in "${GATEWAYS[@]}"; do
            if [ "$SUCCESS" = false ]; then break; fi
            IFS=':' read -r name dir image <<< "$gw"
            SERVICE_DIR="$ROOT_DIR/$dir"
            IMAGE_NAME=$(get_image_name "$REGISTRY/$USERNAME/$image:latest")
            build_service \
                "$name" \
                "$SERVICE_DIR" \
                "$IMAGE_NAME" \
                "$name" || SUCCESS=false
        done
        ;;
    *)
        echo -e "\033[0;31mUnknown service: $SERVICE\033[0m"
        echo -e "\033[0;33mAvailable services: admin-service, booking-integration-service, content-service, conversion-service, messaging-service, mining-service, notification-service, rewards-service, social-service, staking-service, token-service, user-service\033[0m"
        echo -e "\033[0;33mAvailable gateways: admin-gateway, api-gateway\033[0m"
        echo -e "\033[0;33mSpecial options: all (builds all services and gateways), main (builds main services: user-service, rewards-service, notification-service, mining-service, content-service, social-service, booking-integration-service, messaging-service, api-gateway, admin-service, admin-gateway)\033[0m"
        echo -e "\033[0;33mOptions: --no-cache (builds without using Docker cache), --dev (builds for k3s dev cluster with dev image tags)\033[0m"
        echo -e "\033[0;33mUsage: ./build-images.sh [service-name|gateway-name|all|main] [--no-cache] [--dev]\033[0m"
        exit 1
        ;;
esac

if [ "$SUCCESS" = true ]; then
    echo -e "\033[0;32m========================================\033[0m"
    echo -e "\033[0;32mAll builds completed successfully!\033[0m"
    echo -e "\033[0;32m========================================\033[0m"
    exit 0
else
    echo -e "\033[0;31m========================================\033[0m"
    echo -e "\033[0;31mBuild failed!\033[0m"
    echo -e "\033[0;31m========================================\033[0m"
    exit 1
fi
