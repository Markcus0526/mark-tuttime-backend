#!/bin/bash
set -e

POSTGRES_USER="${POSTGRES_USER:-postgres}"

echo "Creating analytics_service_db database..."

# Wait for PostgreSQL to be ready
until psql -U "$POSTGRES_USER" -c '\q' 2>/dev/null; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done

# Check if database exists, create if it doesn't
DB_EXISTS=$(psql -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='analytics_service_db'" 2>/dev/null || echo "")

if [ "$DB_EXISTS" != "1" ]; then
    echo "Database does not exist. Creating..."
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE analytics_service_db;
        ALTER DATABASE analytics_service_db SET search_path TO analytics_service, public;
EOSQL
    echo "Database created successfully."
else
    echo "Database already exists. Skipping creation."
fi

# Set default search_path for the database
echo "Setting database search_path..."
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d pos_service_db <<-EOSQL
    CREATE SCHEMA IF NOT EXISTS pos_service;
    ALTER DATABASE pos_service_db SET search_path TO pos_service, public;
EOSQL

echo "Database setup complete. PostgreSQL will automatically run init-db.sql for schema initialization."