#!/bin/bash
# ==============================================================================
# TutTime Production Microservices Database Restore / Disaster Recovery Script
# ==============================================================================

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "❌ Usage Error: Missing arguments."
    echo "💡 Syntax: ./restore-database.sh <DATABASE_NAME> <PATH_TO_BACKUP_FILE.sql.gz>"
    echo "👉 Example: ./restore-database.sh menu_service_db /app/backups/postgres/menu_service_db_20260608.sql.gz"
    exit 1
fi

TARGET_DB="$1"
BACKUP_FILE="$2"
DB_USER="postgres"
DB_PASSWORD="password"
DB_HOST="menu-service-db"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ ERROR: Targeted backup file does not exist at path: $BACKUP_FILE"
    exit 1
fi

echo "🚨 WARNING: You are about to overwrite all data inside target database: $TARGET_DB"
read -p "🤔 Are you sure you want to proceed with disaster recovery? (y/N): " confirm

if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "🛑 Restore aborted by operator."
    exit 0
fi

echo "🔄 Initializing data re-hydration sequence..."

# 1. Terminate any active application socket connections to prevent deadlocks
echo "🔌 Terminating active connection channels to $TARGET_DB..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "$TARGET_DB" -c \
"SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TARGET_DB' AND pid <> pg_backend_pid();"

# 2. Drop and reconstruct a pristine database space
echo "💥 Dropping and reconstructing target database shell..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "postgres" -c "DROP DATABASE IF EXISTS $TARGET_DB;"
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "postgres" -c "CREATE DATABASE $TARGET_DB;"

# 3. Stream and extract compressed SQL binary data directly back into PostgreSQL
echo "🚀 Streaming raw backup data matrix directly into database context..."
gunzip -c "$BACKUP_FILE" | PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -U "$DB_USER" -d "$TARGET_DB"

if [ $? -eq 0 ]; then
    echo "🎉 SUCCESS: Database $TARGET_DB has been fully re-hydrated and restored to operational health!"
else
    echo "❌ CRITICAL ERROR: Disaster recovery execution failed."
    exit 1
fi
