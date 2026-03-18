#!/bin/bash
# Build and Push Docker Images - Mac (Multi-Architecture)
# Usage: ./build-images-mac.sh [service-name]
# Examples:
#   ./build-images-mac.sh user-service
#   ./build-images-mac.sh api-gateway
#   ./build-images-mac.sh all

set -e

SERVICE="${1:-all}"

# Configuration
REGISTRY="${DOCKER_REGISTRY:-}"
PROJECT_PATH="${DOCKER_PROJECT_PATH:-}"
USERNAME="${DOCKER_USERNAME:-}"
PASSWORD="${DOCKER_PASSWORD:***REMOVED***}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PLATFORM="linux/amd64"  # Kubernetes needs Linux AMD64

# Validate required environment variables
if [ -z "$REGISTRY" ]; then
    echo "Error: DOCKER_REGISTRY environment variable is not set"
    echo "Please set: export DOCKER_REGISTRY='registry.apart.local'"
    exit 1
fi
if [ -z "$PROJECT_PATH" ]; then
    echo "Error: DOCKER_PROJECT_PATH environment variable is not set"
    echo "Please set: export DOCKER_PROJECT_PATH='tuttime/tuttime-services'"
    exit 1
fi
if [ -z "$USERNAME" ]; then
    echo "Error: DOCKER_USERNAME environment variable is not set"
    echo "Please set: export DOCKER_USERNAME='your-username'"
    exit 1
fi
if [ -z "$PASSWORD" ]; then
    echo "Error: DOCKER_PASSWORD environment variable is not set"
    echo "Please set: export DOCKER_PASSWORD='your-password'"
    exit 1
fi

# Enable BuildKit
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

echo "========================================"
echo "Docker Image Build Script (Mac - Multi-Arch)"
echo "========================================"
echo ""

# Check if buildx is available
if ! docker buildx version &>/dev/null; then
    echo "Error: docker buildx not available"
    echo "Please update Docker Desktop or install buildx"
    exit 1
fi

# Create buildx builder if it doesn't exist
if ! docker buildx ls | grep -q "multiarch"; then
    echo "Creating buildx builder..."
    docker buildx create --name multiarch --use
else
    echo "Using existing buildx builder..."
    docker buildx use multiarch
fi

# Login to GitLab Container Registry
echo "Logging in to GitLab Container Registry..."
echo "$PASSWORD" | docker login "$REGISTRY" -u "$USERNAME" --password-stdin
echo "Login successful!"
echo ""

# Function to build and push a service (with buildx)
build_service() {
    local service_name=$1
    local dockerfile_path=$2
    local image_name=$3
    
    echo "Building $service_name for $PLATFORM..."
    echo "  Dockerfile: $dockerfile_path"
    echo "  Image: $image_name"
    echo "  Platform: $PLATFORM"
    
    # Build and push directly (no local load for Kubernetes)
    docker buildx build \
        --platform "$PLATFORM" \
        --progress=plain \
        -f "$dockerfile_path" \
        -t "$image_name" \
        --push \
        "$ROOT_DIR"
    
    echo "Build and push successful!"
    echo ""
}

# Build services based on parameter
case "$SERVICE" in
    user-service)
        build_service \
            "user-service" \
            "services/user-service/docker/Dockerfile" \
            "$REGISTRY/$PROJECT_PATH/tuttime-user-service:latest"
        ;;
    api-gateway)
        build_service \
            "api-gateway" \
            "api-gateway/Dockerfile" \
            "$REGISTRY/$PROJECT_PATH/tuttime-api-gateway:latest"
        ;;
    all)
        echo "Building all services..."
        echo ""
        
        build_service \
            "user-service" \
            "services/user-service/docker/Dockerfile" \
            "$REGISTRY/$PROJECT_PATH/tuttime-user-service:latest"
        
        build_service \
            "api-gateway" \
            "api-gateway/Dockerfile" \
            "$REGISTRY/$PROJECT_PATH/tuttime-api-gateway:latest"
        ;;
    *)
        echo "Unknown service: $SERVICE"
        echo "Usage: ./build-images-mac.sh [user-service|api-gateway|all]"
        exit 1
        ;;
esac

echo "========================================"
echo "All builds completed successfully!"
echo "========================================"

