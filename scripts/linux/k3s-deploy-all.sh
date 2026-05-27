#!/bin/bash
# deploy-tuttime.sh
# Imports tuttime images into K3s containerd and deploys manifests in order:
# 1. Create namespace
# 2. Infrastructure (k3s/infra/)
# 3. API Gateway (k3s/api-gateway/)
# 4. All microservices (k3s/<service>/)

set -e  # exit on error

# Determine the absolute path of the project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K8S_DIR="${PROJECT_ROOT}/k3s"

# Parse optional -y flag
AUTO_YES=false
while getopts "y" opt; do
    case $opt in
        y) AUTO_YES=true ;;
        *) echo "Usage: $0 [-y]"; exit 1 ;;
    esac
done

confirm() {
    if [ "$AUTO_YES" = true ]; then
        return 0
    fi
    read -p "$1 (y/N): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

echo "========================================="
echo "TutTime Deployment Script"
echo "========================================="

# Check if kubectl can connect
if ! kubectl get namespace default &>/dev/null; then
    echo "Error: kubectl cannot connect to the cluster. Please check your K3s installation."
    exit 1
fi

# ----------------------------------------------------------------------
# 0. Create namespace if it doesn't exist
# ----------------------------------------------------------------------
echo "Step 0: Ensuring namespace 'tuttime' exists"
if kubectl get namespace tuttime &>/dev/null; then
    echo "Namespace 'tuttime' already exists."
else
    echo "Creating namespace 'tuttime'..."
    kubectl create namespace tuttime
fi

# ----------------------------------------------------------------------
# 1. Import custom images into containerd
# ----------------------------------------------------------------------
echo "Step 1: Importing tuttime Docker images into K3s containerd"
IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "^tuttime/" | sort -u)
if [ -z "$IMAGES" ]; then
    echo "No tuttime images found. Please build images first (e.g., using docker compose build)."
    exit 1
fi
echo "Found images:"
echo "$IMAGES"
echo ""

if confirm "Import these images into containerd?"; then
    for img in $IMAGES; do
        echo "Importing $img ..."
        docker save "$img" | sudo k3s ctr image import -
    done
    echo "✅ Image import complete."
else
    echo "Skipping image import."
fi

# ----------------------------------------------------------------------
# 2. Deploy Infrastructure (databases, Kafka, Redis, etc.)
# ----------------------------------------------------------------------
echo ""
echo "Step 2: Deploying infrastructure services"
INFRA_DIR="${K8S_DIR}/infra"
if [ ! -d "$INFRA_DIR" ]; then
    echo "Error: Infrastructure directory $INFRA_DIR not found. Exiting."
    exit 1
fi

if confirm "Apply $INFRA_DIR manifests?"; then
    kubectl apply -f "$INFRA_DIR" -n tuttime
    echo "Infrastructure deployed."
else
    echo "Skipping infrastructure deployment."
fi

# Optional: wait for critical infrastructure (Redis) to be ready
if confirm "Wait for Redis pod to be ready (recommended)?"; then
    echo "Waiting for Redis pod to be Running..."
    kubectl wait --for=condition=ready pod -l app=redis -n tuttime --timeout=120s || echo "Timeout waiting for Redis, continuing anyway."
fi

# ----------------------------------------------------------------------
# 3. Deploy API Gateway
# ----------------------------------------------------------------------
echo ""
echo "Step 3: Deploying API Gateway"
API_GATEWAY_DIR="${K8S_DIR}/api-gateway"
if [ ! -d "$API_GATEWAY_DIR" ]; then
    echo "Error: API Gateway directory $API_GATEWAY_DIR not found. Exiting."
    exit 1
fi

if confirm "Apply $API_GATEWAY_DIR manifests?"; then
    kubectl apply -f "$API_GATEWAY_DIR" -n tuttime
    echo "API Gateway deployed."
else
    echo "Skipping API Gateway deployment."
fi

# ----------------------------------------------------------------------
# 4. Deploy all microservices
# ----------------------------------------------------------------------
echo ""
echo "Step 4: Deploying microservices"
SERVICES=(
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

if confirm "Apply all microservices?"; then
    for svc in "${SERVICES[@]}"; do
        SERVICE_DIR="${K8S_DIR}/${svc}"
        if [ -d "$SERVICE_DIR" ]; then
            echo "Applying $svc..."
            kubectl apply -f "$SERVICE_DIR" -n tuttime
        else
            echo "Warning: $SERVICE_DIR not found, skipping."
        fi
    done
    echo "Microservices deployed."
else
    echo "Skipping microservices deployment."
fi

# ----------------------------------------------------------------------
# 5. Optional restart of deployments
# ----------------------------------------------------------------------
echo ""
if confirm "Restart all deployments in namespace tuttime to pick up new images?"; then
    kubectl rollout restart deployment -n tuttime
    echo "Deployments restarted."
fi

echo ""
echo "✅ Deployment process completed."
echo "Monitor with: kubectl get pods -n tuttime -w"