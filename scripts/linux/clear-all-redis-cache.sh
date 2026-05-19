#!/bin/bash

# Bash script to clear all Redis cache
# This clears all application caches including verification codes, rate limits, tokens, and Spring cache
#
# Usage:
#   ./scripts/linux/clear-all-redis-cache.sh                    # Clear all application caches (safe)
#   ./scripts/linux/clear-all-redis-cache.sh --flush-db         # Clear entire Redis database (WARNING: removes everything)
#   ./scripts/linux/clear-all-redis-cache.sh --flush-all        # Clear all Redis databases (DANGER: removes everything from all DBs)
#   ./scripts/linux/clear-all-redis-cache.sh --host localhost --port 6379 --password mypass

# Don't exit on error - we handle errors manually
set +e

# Default values
FLUSH_DB=false
FLUSH_ALL=false
REDIS_HOST="localhost"
REDIS_PORT=6379
REDIS_PASSWORD=""
USE_DOCKER=false
REDIS_CONTAINER=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --flush-db|--flushdb)
            FLUSH_DB=true
            shift
            ;;
        --flush-all|--flushall)
            FLUSH_ALL=true
            shift
            ;;
        --host)
            REDIS_HOST="$2"
            shift 2
            ;;
        --port)
            REDIS_PORT="$2"
            shift 2
            ;;
        --password)
            REDIS_PASSWORD="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--flush-db] [--flush-all] [--host HOST] [--port PORT] [--password PASSWORD]"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "Clear All Redis Cache"
echo "========================================"
echo ""

# Check if redis-cli is available
REDIS_CLI_CMD=""
if command -v redis-cli >/dev/null 2>&1; then
    REDIS_CLI_CMD="redis-cli"
    echo "Using local redis-cli"
elif command -v docker >/dev/null 2>&1; then
    # Try to find Redis container
    echo "Checking for Redis Docker container..."
    REDIS_CONTAINER=$(docker ps --format "{{.Names}}" 2>/dev/null | grep -i redis | head -1)
    if [ -n "$REDIS_CONTAINER" ]; then
        echo "Found Redis container: $REDIS_CONTAINER"
        REDIS_CLI_CMD="docker exec $REDIS_CONTAINER redis-cli"
        USE_DOCKER=true
    fi
fi

if [ -z "$REDIS_CLI_CMD" ]; then
    echo "ERROR: redis-cli not found and no Redis Docker container detected."
    echo ""
    echo "Options:"
    echo "1. Install Redis and add redis-cli to PATH"
    echo "2. Run Redis in Docker and ensure container name contains 'redis'"
    echo ""
    echo "Or manually run:"
    echo "   docker exec -it <redis-container-name> redis-cli FLUSHDB"
    exit 1
fi

# Build Redis connection arguments
REDIS_ARGS=()
if [ "$USE_DOCKER" = false ]; then
    # Only add connection args if not using Docker (Docker exec connects directly to container)
    if [ "$REDIS_HOST" != "localhost" ]; then
        REDIS_ARGS+=("-h" "$REDIS_HOST")
    fi
    if [ "$REDIS_PORT" != "6379" ]; then
        REDIS_ARGS+=("-p" "$REDIS_PORT")
    fi
fi
if [ -n "$REDIS_PASSWORD" ]; then
    export REDISCLI_AUTH="$REDIS_PASSWORD"
    REDIS_ARGS+=("-a" "$REDIS_PASSWORD")
fi

# Define all cache key patterns to clear
declare -a CACHE_PATTERNS=(
    # SMS and Email verification
    "sms:verification:*"
    "sms:signup:*"
    "sms:attempts:*"
    "email:verification:*"
    "email:attempts:*"
    
    # Tokens
    "signup:token:*"
    "password-reset:token:*"
    "whitelist:token:*"
    "whitelist:user:*:tokens"
    "whitelist:device:*:tokens"
    "blacklist:token:*"
    
    # Rate limiting and security
    "ratelimit:*"
    "bruteforce:*"
    
    # Online status
    "presence:user:*"
    "online:users"
    
    # Spring Cache (application caches)
    "users"
    "balances"
    "ap-balances"
    "status"
    "status-bonus"
    "premium-mining-eligibility"
    "staking-pools"
    "referral-codes"
    "video-content"
    "ads"
    "map-queries"
)

# Function to execute redis-cli command
execute_redis() {
    local cmd_args=("$@")
    local result
    if [ "$USE_DOCKER" = true ]; then
        result=$(docker exec "$REDIS_CONTAINER" redis-cli "${cmd_args[@]}" 2>&1)
    else
        result=$(redis-cli "${REDIS_ARGS[@]}" "${cmd_args[@]}" 2>&1)
    fi
    local exit_code=$?
    if [ $exit_code -eq 0 ]; then
        echo "$result"
        return 0
    else
        echo "$result" >&2
        return $exit_code
    fi
}

# Function to check if key exists
key_exists() {
    local key="$1"
    local result
    result=$(execute_redis EXISTS "$key" 2>/dev/null)
    if [ $? -eq 0 ] && [ "$result" = "1" ]; then
        return 0
    else
        return 1
    fi
}

# Function to delete key
delete_key() {
    local key="$1"
    execute_redis DEL "$key" >/dev/null 2>&1
    return $?
}

# Function to get keys matching pattern
get_keys() {
    local pattern="$1"
    local result
    result=$(execute_redis KEYS "$pattern" 2>/dev/null)
    if [ $? -eq 0 ] && [ -n "$result" ]; then
        echo "$result" | grep -v "^$"
    fi
}

# Main execution
if [ "$FLUSH_ALL" = true ]; then
    # DANGER: Clear all databases
    echo "WARNING: This will clear ALL Redis databases!"
    echo "Press Ctrl+C to cancel, or wait 5 seconds to continue..."
    sleep 5
    
    echo "Flushing ALL Redis databases..."
    if execute_redis FLUSHALL >/dev/null 2>&1; then
        echo "All Redis databases cleared successfully"
    else
        echo "Failed to flush all databases"
        exit 1
    fi
elif [ "$FLUSH_DB" = true ]; then
    # WARNING: Clear entire current database
    echo "WARNING: This will clear the entire current Redis database!"
    echo "Press Ctrl+C to cancel, or wait 3 seconds to continue..."
    sleep 3
    
    echo "Flushing current Redis database..."
    if execute_redis FLUSHDB >/dev/null 2>&1; then
        echo "Current Redis database cleared successfully"
    else
        echo "Failed to flush database"
        exit 1
    fi
else
    # Safe: Clear only application cache patterns
    echo "Clearing application caches (safe mode)..."
    echo ""
    
    total_deleted=0
    patterns_cleared=0
    
    for pattern in "${CACHE_PATTERNS[@]}"; do
        printf "  Checking pattern: %-40s" "$pattern"
        
        # For exact keys (no wildcard), use EXISTS first to check existence, then DEL
        if [[ "$pattern" != *"*"* ]]; then
            if key_exists "$pattern"; then
                if delete_key "$pattern"; then
                    ((total_deleted++))
                    ((patterns_cleared++))
                    echo " - Cleared"
                else
                    echo " - Failed"
                fi
            else
                echo " - Not found"
            fi
        else
            # For patterns with wildcards, use KEYS then DEL
            # Note: KEYS command can be slow on large datasets
            keys=$(get_keys "$pattern")
            
            if [ -n "$keys" ]; then
                # Count keys
                key_count=$(echo "$keys" | wc -l)
                printf "\r  Checking pattern: %-40s - Found %d key(s), deleting..." "$pattern" "$key_count"
                
                deleted_count=0
                processed=0
                while IFS= read -r key; do
                    if [ -n "$key" ]; then
                        if delete_key "$key"; then
                            ((deleted_count++))
                            ((total_deleted++))
                        fi
                        ((processed++))
                        # Show progress for large sets
                        if [ "$key_count" -gt 100 ] && [ $((processed % 50)) -eq 0 ]; then
                            printf "\r  Checking pattern: %-40s - Deleting... (%d/%d)" "$pattern" "$processed" "$key_count"
                        fi
                    fi
                done <<< "$keys"
                
                printf "\r  Checking pattern: %-40s - Cleared %d key(s)\n" "$pattern" "$deleted_count"
                if [ "$deleted_count" -gt 0 ]; then
                    ((patterns_cleared++))
                fi
            else
                echo " - No keys found"
            fi
        fi
    done
    
    echo ""
    echo "Cleared $total_deleted key(s) from $patterns_cleared pattern(s)"
fi

echo ""
echo "========================================"
echo "Operation completed successfully!"
echo "========================================"
echo ""

# Cleanup
unset REDISCLI_AUTH

