#!/bin/bash
# Restart Pods Script - Mac (Optimized)
# Restarts Kubernetes deployments to pull latest images
# Optimized for Mac with better output formatting and colors
# Usage: ./pull-and-restart-mac.sh [service-name|all]
# Examples:
#   ./pull-and-restart-mac.sh user-service
#   ./pull-and-restart-mac.sh api-gateway
#   ./pull-and-restart-mac.sh all

set -e

SERVICE="${1:-all}"
NAMESPACE="tuttime"

# Colors for Mac terminal (ANSI escape codes)
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Restart Pods Script (Mac)${NC}"
echo -e "${CYAN}========================================${NC}"
echo -e "${GRAY}Note: Kubernetes will automatically pull latest images${NC}"
echo -e "${GRAY}      (imagePullPolicy: Always is configured)${NC}"
echo ""

# Function to restart deployment
restart_deployment() {
    local service_name=$1
    
    echo -e "${YELLOW}Restarting ${BLUE}$service_name${NC}..."
    
    # Restart deployment
    if kubectl rollout restart "deployment/$service_name" -n "$NAMESPACE" 2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} Deployment restart initiated"
        
        # Wait for rollout to complete
        echo -e "  ${GRAY}Waiting for rollout to complete...${NC}"
        if kubectl rollout status "deployment/$service_name" -n "$NAMESPACE" --timeout=300s 2>/dev/null; then
            echo -e "  ${GREEN}✓${NC} Rollout completed successfully"
        else
            echo -e "  ${YELLOW}⚠${NC} Rollout status check timed out or failed"
        fi
    else
        echo -e "  ${RED}✗${NC} Failed to restart deployment: $service_name"
        return 1
    fi
    
    echo ""
}

# Process services based on parameter
case "$SERVICE" in
    user-service)
        restart_deployment "user-service"
        ;;
    api-gateway)
        restart_deployment "api-gateway"
        ;;
    all)
        echo -e "${CYAN}Restarting all services...${NC}"
        echo ""
        
        restart_deployment "user-service"
        restart_deployment "api-gateway"
        ;;
    *)
        echo -e "${RED}Unknown service: $SERVICE${NC}"
        echo "Usage: ./pull-and-restart-mac.sh [user-service|api-gateway|all]"
        exit 1
        ;;
esac

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}All operations completed!${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""
echo -e "${CYAN}Current pod status:${NC}"
kubectl get pods -n "$NAMESPACE" --no-headers | awk '{printf "  %s - %s - Age: %s\n", $1, $3, $5}'
