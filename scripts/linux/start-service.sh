#!/bin/bash

# Bash script to start a single service with proper environment variables
# Usage: ./scripts/linux/start-service.sh <service-name> [--clean]
#        pm2 start ./scripts/linux/start-service.sh -- <service-name> [--clean]
#
# Example: ./scripts/linux/start-service.sh mining-service
# Example: ./scripts/linux/start-service.sh mining-service --clean
# Example: pm2 start ./scripts/linux/start-service.sh -- mining-service --clean
#
# Options:
#   --clean    Clean build cache before building (removes target directory and runs mvn clean)
#
# Available services:
#   - user-service
#   - token-service
#   - mining-service
#   - rewards-service
#   - staking-service
#   - content-service
#   - notification-service
#   - conversion-service
#   - booking-integration-service
#   - admin-service
#   - api-gateway
#   - admin-gateway

# Don't exit on error during build phase, but exit if JAR execution fails
set +e

# Check if service name is provided
if [ -z "$1" ]; then
    echo "ERROR: Service name is required"
    echo ""
    echo "Usage: $0 <service-name>"
    echo ""
    echo "Available services:"
    echo "  - admin-service"
    echo "  - token-service"
    echo "  - mining-service"
    echo "  - rewards-service"
    echo "  - staking-service"
    echo "  - content-service"
    echo "  - notification-service"
    echo "  - conversion-service"
    echo "  - booking-integration-service"
    echo "  - user-service"
    echo "  - api-gateway"
    echo "  - admin-gateway"
    exit 1
fi

SERVICE_NAME="$1"

# Get project root (parent of scripts directory)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# Service configuration
declare -A SERVICE_PATH
declare -A SERVICE_DB_URL
declare -A SERVICE_PORT

SERVICE_PATH["user-service"]="services/user-service"
SERVICE_DB_URL["user-service"]="jdbc:postgresql://localhost:5432/user_service_db?currentSchema=user_service&stringtype=unspecified&prepareThreshold=0"
SERVICE_PORT["user-service"]="28081"

SERVICE_PATH["token-service"]="services/token-service"
SERVICE_DB_URL["token-service"]="jdbc:postgresql://localhost:5432/token_service_db?currentSchema=token_service&stringtype=unspecified&prepareThreshold=0&TimeZone=UTC"
SERVICE_PORT["token-service"]="28082"

SERVICE_PATH["mining-service"]="services/mining-service"
SERVICE_DB_URL["mining-service"]="jdbc:postgresql://localhost:5432/mining_service_db?currentSchema=mining_service&TimeZone=UTC"
SERVICE_PORT["mining-service"]="28083"

SERVICE_PATH["rewards-service"]="services/rewards-service"
SERVICE_DB_URL["rewards-service"]="jdbc:postgresql://localhost:5432/rewards_service_db?currentSchema=rewards_service&TimeZone=UTC"
SERVICE_PORT["rewards-service"]="28085"

SERVICE_PATH["staking-service"]="services/staking-service"
SERVICE_DB_URL["staking-service"]="jdbc:postgresql://localhost:5432/staking_service_db?currentSchema=staking_service&TimeZone=UTC"
SERVICE_PORT["staking-service"]="28084"

SERVICE_PATH["content-service"]="services/content-service"
SERVICE_DB_URL["content-service"]="jdbc:postgresql://localhost:5432/content_service_db?currentSchema=content_service&TimeZone=UTC"
SERVICE_PORT["content-service"]="28086"

SERVICE_PATH["notification-service"]="services/notification-service"
SERVICE_DB_URL["notification-service"]="jdbc:postgresql://localhost:5432/notification_service_db?currentSchema=notification_service&stringtype=unspecified&prepareThreshold=0&TimeZone=UTC"
SERVICE_PORT["notification-service"]="28088"

SERVICE_PATH["conversion-service"]="services/conversion-service"
SERVICE_DB_URL["conversion-service"]="jdbc:postgresql://localhost:5432/conversion_service_db?currentSchema=conversion_service&stringtype=unspecified&prepareThreshold=0&TimeZone=UTC"
SERVICE_PORT["conversion-service"]="28087"

SERVICE_PATH["booking-integration-service"]="services/booking-integration-service"
SERVICE_DB_URL["booking-integration-service"]="jdbc:postgresql://localhost:5432/booking_integration_service_db?currentSchema=booking_integration_service&TimeZone=UTC"
SERVICE_PORT["booking-integration-service"]="28089"

SERVICE_PATH["admin-service"]="services/admin-service"
SERVICE_DB_URL["admin-service"]="jdbc:postgresql://localhost:5432/admin_service_db?currentSchema=admin_service&stringtype=unspecified&prepareThreshold=0&TimeZone=UTC"
SERVICE_PORT["admin-service"]="28090"

SERVICE_PATH["api-gateway"]="api-gateway"
SERVICE_DB_URL["api-gateway"]=""
SERVICE_PORT["api-gateway"]="29000"

SERVICE_PATH["admin-gateway"]="admin-gateway"
SERVICE_DB_URL["admin-gateway"]=""
SERVICE_PORT["admin-gateway"]="28080"

# Check if service exists
if [ -z "${SERVICE_PATH[$SERVICE_NAME]}" ]; then
    echo "ERROR: Unknown service: $SERVICE_NAME"
    echo ""
    echo "Available services:"
    for key in "${!SERVICE_PATH[@]}"; do
        echo "  - $key"
    done | sort
    exit 1
fi

SERVICE_DIR="${SERVICE_PATH[$SERVICE_NAME]}"
SERVICE_FULL_PATH="$PROJECT_ROOT/$SERVICE_DIR"

if [ ! -d "$SERVICE_FULL_PATH" ]; then
    echo "ERROR: Service path not found: $SERVICE_FULL_PATH"
    exit 1
fi

# Function to load environment variables from .env.local file
load_env_file() {
    local env_file_path="$1"
    
    if [ -f "$env_file_path" ]; then
        echo "Loading environment variables from .env.local..."
        # Read the file line by line
        while IFS= read -r line || [ -n "$line" ]; do
            # Skip empty lines and comments
            line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            if [ -z "$line" ] || [[ "$line" =~ ^# ]]; then
                continue
            fi
            
            # Split on = to separate key and value
            if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
                key="${BASH_REMATCH[1]}"
                value="${BASH_REMATCH[2]}"
                
                # Remove inline comments (everything after # that's not inside quotes)
                value=$(echo "$value" | sed 's/[[:space:]]*#.*$//')
                
                # Remove surrounding quotes if present
                value=$(echo "$value" | sed -E "s/^['\"](.*)['\"]$/\1/")
                
                # Trim whitespace
                key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                
                # Export the variable
                export "$key=$value"
            fi
        done < "$env_file_path"
        echo "Environment variables loaded successfully"
    else
        echo "Warning: .env.local file not found at $env_file_path"
        echo "Using default environment variables"
        echo "Tip: Create .env.local file based on .env.local.example (if available)"
    fi
}

# Function to check if a port is accessible
check_port() {
    local host=$1
    local port=$2
    local service_name=$3
    
    # Try to connect using timeout (if available) or use a simple check
    if command -v timeout >/dev/null 2>&1; then
        if timeout 1 bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null; then
            return 0
        fi
    elif command -v nc >/dev/null 2>&1; then
        if nc -z "$host" "$port" 2>/dev/null; then
            return 0
        fi
    else
        # Fallback: try to open a TCP connection
        if (echo >/dev/tcp/$host/$port) 2>/dev/null; then
            return 0
        fi
    fi
    
    echo "⚠️  WARNING: $service_name is not accessible at $host:$port"
    return 1
}

# Function to check infrastructure services
check_infrastructure() {
    local missing_services=()
    
    echo "Checking infrastructure services..."
    
    # Check Redis (most services need it)
    # Note: Always check localhost for infrastructure services when running locally
    # The .env.local might have Docker hostnames, but we're running on the host
    local redis_check_host="localhost"
    local redis_check_port="${REDIS_PORT:-6379}"
    if ! check_port "$redis_check_host" "$redis_check_port" "Redis"; then
        missing_services+=("Redis ($redis_check_host:$redis_check_port)")
    fi
    
    # Check PostgreSQL (extract from DATABASE_URL or use defaults)
    # Note: Always check localhost for infrastructure services when running locally
    local pg_host="localhost"
    local pg_port="5432"
    if [ -n "$DATABASE_URL" ]; then
        # Try to extract port from JDBC URL, but always use localhost as host
        if [[ "$DATABASE_URL" =~ jdbc:postgresql://([^:/]+)(:([0-9]+))? ]]; then
            pg_port="${BASH_REMATCH[3]:-5432}"
        fi
    fi
    if ! check_port "$pg_host" "$pg_port" "PostgreSQL"; then
        missing_services+=("PostgreSQL ($pg_host:$pg_port)")
    fi
    
    # Check Kafka if enabled
    # Note: Always check localhost for infrastructure services when running locally
    if [ "${KAFKA_ENABLED:-true}" = "true" ]; then
        local kafka_host_port="${KAFKA_BOOTSTRAP_SERVERS:-localhost:29092}"
        local kafka_port="${kafka_host_port##*:}"
        # Always use localhost for checking, even if KAFKA_BOOTSTRAP_SERVERS has a different host
        local kafka_check_host="localhost"
        if ! check_port "$kafka_check_host" "$kafka_port" "Kafka"; then
            missing_services+=("Kafka ($kafka_check_host:$kafka_port)")
        fi
    fi
    
    if [ ${#missing_services[@]} -gt 0 ]; then
        echo ""
        echo "⚠️  WARNING: Some infrastructure services are not running!"
        echo ""
        echo "Missing services:"
        for service in "${missing_services[@]}"; do
            echo "  - $service"
        done
        echo ""
        echo "Please start infrastructure services first:"
        echo "  cd infrastructure"
        echo "  docker-compose up -d"
        echo ""
        # Only prompt if running interactively (not from PM2)
        if [ -t 0 ]; then
            echo "Or start individual services as needed."
            echo ""
            read -p "Continue anyway? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
        else
            echo "Continuing anyway (non-interactive mode)..."
        fi
    else
        echo "✅ All infrastructure services are accessible"
    fi
    echo ""
}

# Load environment variables from scripts/.env file first
SCRIPTS_ENV_FILE="$PROJECT_ROOT/scripts/.env"
if [ -f "$SCRIPTS_ENV_FILE" ]; then
    echo "Loading environment variables from scripts/.env..."
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        if [ -z "$line" ] || [[ "$line" =~ ^# ]]; then
            continue
        fi
        
        # Split on = to separate key and value
        if [[ "$line" =~ ^([^=]+)=(.*)$ ]]; then
            key="${BASH_REMATCH[1]}"
            value="${BASH_REMATCH[2]}"
            
            # Remove inline comments (everything after # that's not inside quotes)
            value=$(echo "$value" | sed 's/[[:space:]]*#.*$//')
            
            # Remove surrounding quotes if present
            value=$(echo "$value" | sed -E "s/^['\"](.*)['\"]$/\1/")
            
            # Trim whitespace
            key=$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            value=$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            
            # Export the variable
            export "$key=$value"
        fi
    done < "$SCRIPTS_ENV_FILE"
    echo "Environment variables from scripts/.env loaded"
    echo ""
fi

# Change to service directory first (required before loading .env.local)
cd "$SERVICE_FULL_PATH"

# Load environment variables from service's .env.local file
ENV_FILE_PATH="$SERVICE_FULL_PATH/.env.local"
load_env_file "$ENV_FILE_PATH"

# Set default values if not provided in .env.local or scripts/.env (no hardcoded passwords)
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
export DATABASE_USERNAME="${DATABASE_USERNAME:-postgres}"
export DATABASE_PASSWORD="${DATABASE_PASSWORD:***REMOVED***}"
export JWT_SECRET="${JWT_SECRET:***REMOVED***}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:***REMOVED***}"
export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:29092}"
export EUREKA_SERVER_URL="${EUREKA_SERVER_URL:-http://localhost:8761/eureka/}"
export ZIPKIN_URL="${ZIPKIN_URL:-http://localhost:9411/api/v2/spans}"
export KAFKA_ENABLED="${KAFKA_ENABLED:-true}"

# Validate required credentials
if [ -z "$DATABASE_PASSWORD" ]; then
    echo "ERROR: DATABASE_PASSWORD is required but not set"
    echo ""
    echo "Please set DATABASE_PASSWORD in one of the following ways:"
    echo "  1. Create scripts/.env file with DATABASE_PASSWORD=your_password"
    echo "  2. Set in service's .env.local file: DATABASE_PASSWORD=your_password"
    echo "  3. Set environment variable: export DATABASE_PASSWORD=your_password"
    exit 1
fi

# Set DATABASE_URL if service requires it
if [ -n "${SERVICE_DB_URL[$SERVICE_NAME]}" ] && [ -z "$DATABASE_URL" ]; then
    export DATABASE_URL="${SERVICE_DB_URL[$SERVICE_NAME]}"
fi

echo "========================================"
echo "Starting $SERVICE_NAME"
echo "========================================"
echo ""
echo "Service Path: $SERVICE_FULL_PATH"
echo "HTTP Port: ${SERVICE_PORT[$SERVICE_NAME]}"
echo ""

# Check infrastructure services
check_infrastructure

# Install parent POM first (required for multi-module projects)
echo "Installing parent POM..."
cd "$PROJECT_ROOT"
if ! mvn install -N -DskipTests; then
    echo "ERROR: Failed to install parent POM!"
    exit 1
fi
echo "Parent POM installed successfully"
echo ""

# Build common-libs modules first (required dependencies for services)
echo "Building common-libs modules..."
if ! mvn install -pl common-libs/commons-exception,common-libs/commons-events,common-libs/commons-auth -am -DskipTests; then
    echo "ERROR: Failed to build common-libs modules!"
    exit 1
fi
echo "Common-libs modules built successfully"
echo ""

# Change to service directory
cd "$SERVICE_FULL_PATH"

# Check if clean build is requested (via CLEAN_BUILD environment variable or --clean flag)
CLEAN_BUILD=false
if [ "$2" = "--clean" ] || [ "${CLEAN_BUILD:-false}" = "true" ]; then
    CLEAN_BUILD=true
fi

# Clean build cache if requested
if [ "$CLEAN_BUILD" = "true" ]; then
    echo "Cleaning build cache for $SERVICE_NAME..."
    # Remove target directory
    if [ -d "$SERVICE_FULL_PATH/target" ]; then
        rm -rf "$SERVICE_FULL_PATH/target"
        echo "Removed target directory"
    fi
    # Clean Maven cache for this project
    cd "$SERVICE_FULL_PATH"
    mvn clean > /dev/null 2>&1 || true
    echo "Maven clean completed"
    echo ""
fi

# Build the service (skip if JAR already exists and is recent, unless clean build)
JAR_FILE=$(find "$SERVICE_FULL_PATH/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -not -name "*.original" 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ] || [ "$CLEAN_BUILD" = "true" ]; then
    echo "Building $SERVICE_NAME..."
    if ! mvn clean install -DskipTests; then
        echo "ERROR: Build failed!"
        exit 1
    fi
    echo "Build successful!"
    echo ""
    
    # Find the JAR file after build
    JAR_FILE=$(find "$SERVICE_FULL_PATH/target" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -not -name "*.original" 2>/dev/null | head -1)
else
    echo "Using existing JAR: $(basename "$JAR_FILE")"
    echo ""
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found in $SERVICE_FULL_PATH/target"
    exit 1
fi

# Run the service with Java
echo "Starting $SERVICE_NAME..."
echo "Service will be available at: http://localhost:${SERVICE_PORT[$SERVICE_NAME]}"
echo "JAR: $JAR_FILE"
echo ""

# Set error handling for JAR execution
set -e

# Execute the JAR with all environment variables
exec java -Dspring.profiles.active=local -jar "$JAR_FILE"

