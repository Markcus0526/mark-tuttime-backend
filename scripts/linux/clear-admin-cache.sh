#!/bin/bash

# Bash script to clear all admin service caches via API
# This script authenticates with the admin API and clears all caches
#
# Usage:
#   ./scripts/linux/clear-admin-cache.sh
#   ./scripts/linux/clear-admin-cache.sh --username admin --password YOUR_PASSWORD
#   ./scripts/linux/clear-admin-cache.sh --base-url http://localhost:28080
#   ./scripts/linux/clear-admin-cache.sh --cache-name analytics  # Clear specific cache

set -e

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$(cd "$SCRIPT_DIR/.." && pwd)/.env"

# Load environment variables from .env file
if [ -f "$ENV_FILE" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        
        # Remove BOM and leading/trailing whitespace
        line=$(echo "$line" | sed 's/^\xEF\xBB\xBF//' | sed 's/^[[:space:]]*//' | sed 's/[[:space:]]*$//')
        
        # Skip if line is empty after trimming
        [[ -z "$line" ]] && continue
        
        # Parse key=value
        if [[ $line =~ ^([^=]+)=(.*)$ ]]; then
            key="${BASH_REMATCH[1]}"
            value="${BASH_REMATCH[2]}"
            
            # Trim key and value
            key=$(echo "$key" | sed 's/^[[:space:]]*//' | sed 's/[[:space:]]*$//')
            value=$(echo "$value" | sed 's/^[[:space:]]*//' | sed 's/[[:space:]]*$//')
            
            # Remove quotes if present
            if [[ $value =~ ^[\"'](.*)[\"']$ ]]; then
                value="${BASH_REMATCH[1]}"
            fi
            
            # Export the variable
            export "$key=$value"
        fi
    done < "$ENV_FILE"
fi

# Default values (no hardcoded passwords)
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:***REMOVED***}"
BASE_URL="${ADMIN_BASE_URL:-http://localhost:28080}"
ADMIN_PATH="/api/admin"
CACHE_NAME=""
VERBOSE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --username|-u)
            ADMIN_USERNAME="$2"
            shift 2
            ;;
        --password|-p)
            ADMIN_PASSWORD="$2"
            shift 2
            ;;
        --base-url|-b)
            BASE_URL="$2"
            shift 2
            ;;
        --cache-name|-c)
            CACHE_NAME="$2"
            shift 2
            ;;
        --verbose|-v)
            VERBOSE=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -u, --username USERNAME    Admin username (default: admin)"
            echo "  -p, --password PASSWORD    Admin password (from .env or environment)"
            echo "  -b, --base-url URL        Base URL for admin API (default: http://localhost:28080)"
            echo "  -c, --cache-name NAME     Clear specific cache (e.g., 'analytics', 'userDetails')"
            echo "  -v, --verbose             Show verbose output"
            echo "  -h, --help               Show this help message"
            echo ""
            echo "Environment variables (or from scripts/.env):"
            echo "  ADMIN_USERNAME           Admin username"
            echo "  ADMIN_PASSWORD           Admin password (required)"
            echo "  ADMIN_BASE_URL           Base URL for admin API"
            echo ""
            echo "Examples:"
            echo "  $0"
            echo "  $0 --username admin --password mypass"
            echo "  $0 --cache-name analytics"
            echo "  ADMIN_USERNAME=admin ADMIN_PASSWORD=***REMOVED*** $0"
            echo ""
            echo "Note: Create scripts/.env file with ADMIN_PASSWORD (see .env.example)"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Check if curl is available
if ! command -v curl >/dev/null 2>&1; then
    echo -e "${RED}ERROR: curl is not installed${NC}"
    echo "Please install curl to use this script"
    exit 1
fi

# Check if jq is available (optional, for pretty JSON output)
HAS_JQ=false
if command -v jq >/dev/null 2>&1; then
    HAS_JQ=true
fi

echo "========================================"
echo "Clear Admin Service Cache"
echo "========================================"
echo ""
echo "Configuration:"
echo "  Base URL: $BASE_URL"
echo "  Username: $ADMIN_USERNAME"
if [ "$VERBOSE" = true ]; then
    echo "  Password: [REDACTED]"
fi
if [ -n "$CACHE_NAME" ]; then
    echo "  Cache Name: $CACHE_NAME"
fi
echo ""

# Function to print verbose output
verbose() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[VERBOSE]${NC} $1"
    fi
}

# Function to print error
error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Function to print success
success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to print warning
warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Validate required credentials
if [ -z "$ADMIN_PASSWORD" ]; then
    error "ADMIN_PASSWORD is required but not set"
    echo ""
    echo "Please set ADMIN_PASSWORD in one of the following ways:"
    echo "  1. Create scripts/.env file with ADMIN_PASSWORD=your_password"
    echo "  2. Pass via command line: $0 --password YOUR_PASSWORD"
    echo "  3. Set environment variable: export ADMIN_PASSWORD=your_password"
    exit 1
fi

# Step 1: Login to get JWT token
verbose "Step 1: Authenticating with admin API..."
LOGIN_URL="${BASE_URL}${ADMIN_PATH}/auth/login"

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$LOGIN_URL" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"$ADMIN_USERNAME\",
        \"password\": \"$ADMIN_PASSWORD\"
    }" 2>&1)

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

verbose "Login HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" != "200" ]; then
    error "Login failed with HTTP status $HTTP_CODE"
    if [ "$HAS_JQ" = true ]; then
        echo "$LOGIN_BODY" | jq '.' 2>/dev/null || echo "$LOGIN_BODY"
    else
        echo "$LOGIN_BODY"
    fi
    
    if [ "$HTTP_CODE" = "400" ]; then
        echo ""
        warning "Invalid credentials. Please check your username and password."
        echo ""
        echo "To use credentials:"
        echo "  1. Create scripts/.env file with ADMIN_USERNAME and ADMIN_PASSWORD"
        echo "  2. Or pass via command line: $0 --username YOUR_USERNAME --password YOUR_PASSWORD"
        echo "  3. Or set environment variables:"
        echo "     export ADMIN_USERNAME=your_username"
        echo "     export ADMIN_PASSWORD=your_password"
    fi
    exit 1
fi

# Extract token from response
if [ "$HAS_JQ" = true ]; then
    TOKEN=$(echo "$LOGIN_BODY" | jq -r '.data.token // empty' 2>/dev/null)
    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        error "Token not found in response"
        echo "$LOGIN_BODY" | jq '.'
        exit 1
    fi
else
    # Fallback: use grep/sed to extract token (less reliable)
    TOKEN=$(echo "$LOGIN_BODY" | grep -o '"token"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/' | head -1)
    if [ -z "$TOKEN" ]; then
        error "Token not found in response (jq not available, using basic parsing)"
        echo "$LOGIN_BODY"
        exit 1
    fi
fi

verbose "Token retrieved successfully (length: ${#TOKEN})"
success "Authentication successful"

# Step 2: Clear cache
echo ""
if [ -n "$CACHE_NAME" ]; then
    verbose "Step 2: Clearing specific cache: $CACHE_NAME"
    CACHE_URL="${BASE_URL}${ADMIN_PATH}/system/cache/clear/$CACHE_NAME"
else
    verbose "Step 2: Clearing all admin caches"
    CACHE_URL="${BASE_URL}${ADMIN_PATH}/system/cache/clear"
fi

CACHE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$CACHE_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" 2>&1)

CACHE_HTTP_CODE=$(echo "$CACHE_RESPONSE" | tail -n1)
CACHE_BODY=$(echo "$CACHE_RESPONSE" | sed '$d')

verbose "Cache clear HTTP Status: $CACHE_HTTP_CODE"

if [ "$CACHE_HTTP_CODE" = "200" ]; then
    if [ "$HAS_JQ" = true ]; then
        MESSAGE=$(echo "$CACHE_BODY" | jq -r '.message // .data // "Cache cleared successfully"' 2>/dev/null)
        success "$MESSAGE"
        if [ "$VERBOSE" = true ]; then
            echo ""
            echo "Full response:"
            echo "$CACHE_BODY" | jq '.'
        fi
    else
        success "Cache cleared successfully"
        if [ "$VERBOSE" = true ]; then
            echo "Response: $CACHE_BODY"
        fi
    fi
elif [ "$CACHE_HTTP_CODE" = "401" ]; then
    error "Authentication failed (401 Unauthorized)"
    echo "The token may have expired or is invalid"
    exit 1
elif [ "$CACHE_HTTP_CODE" = "403" ]; then
    error "Permission denied (403 Forbidden)"
    echo "Your account may not have permission to clear caches"
    if [ "$HAS_JQ" = true ]; then
        echo "$CACHE_BODY" | jq '.' 2>/dev/null || echo "$CACHE_BODY"
    else
        echo "$CACHE_BODY"
    fi
    exit 1
else
    error "Cache clear failed with HTTP status $CACHE_HTTP_CODE"
    if [ "$HAS_JQ" = true ]; then
        echo "$CACHE_BODY" | jq '.' 2>/dev/null || echo "$CACHE_BODY"
    else
        echo "$CACHE_BODY"
    fi
    exit 1
fi

# Step 3: Logout (optional, but good practice)
verbose "Step 3: Logging out..."
LOGOUT_URL="${BASE_URL}${ADMIN_PATH}/auth/logout"
curl -s -X POST "$LOGOUT_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" >/dev/null 2>&1 || true

verbose "Logged out"

echo ""
echo "========================================"
echo "Operation completed successfully!"
echo "========================================"
echo ""

