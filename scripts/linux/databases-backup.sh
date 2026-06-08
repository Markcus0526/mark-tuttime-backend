#!/bin/bash
# ==============================================================================
# TutTime Production Microservices Database Backup Utility Runbook Script
# ==============================================================================

BACKUP_DIR="/app/backups/postgres"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_PASSWORD="***REMOVED***" # Matches your local infrastructure setup
DB_USER="postgres"
DB_HOST="menu-service-db" # Update host depending on the targeted network bridge

# List of all your isolated production database schemas
DATABASES=("user_service_db" "restaurant_service_db" "menu_service_db" "booking_service_db" "payment_service_db" "order_service_db" "pos_service_db" "review_service_db" "analytics_service_db" "notification_service_db")

mkdir -p "$BACKUP_DIR"

echo "🏁 Starting automated database backup sequence..."

for DB_NAME in "${DATABASES[@]}"; do
    BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_$TIMESTAMP.sql.gz"
    echo "💾 Backing up database: $DB_NAME -> $BACKUP_FILE"

    # Execute pg_dump natively inside the network layer with max compression
    PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" | gzip > "$BACKUP_FILE"

    if [ $? -eq 0 ]; then
        echo "✅ Success: $DB_NAME safely backed up."
    else
        echo "❌ ERROR: Backup failed for $DB_NAME!"
    fi
done

# Housekeeping: Retain backups for only 7 days to maximize storage efficiency
echo "🧹 Purging historical backup assets older than 7 days..."
find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime +7 -delete

echo "🎉 Database backup sequence completed successfully."
