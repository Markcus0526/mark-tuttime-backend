#!/bin/bash
set -e

POSTGRES_USER="${POSTGRES_USER:-postgres}"

echo "Creating review_service_db database..."

# Wait for PostgreSQL to be ready
until psql -U "$POSTGRES_USER" -c '\q' 2>/dev/null; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 1
done

# Check if database exists, create if it doesn't
DB_EXISTS=$(psql -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='review_service_db'" 2>/dev/null || echo "")

if [ "$DB_EXISTS" != "1" ]; then
    echo "Database does not exist. Creating..."
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE review_service_db;
        ALTER DATABASE review_service_db SET search_path TO review_service, public;
EOSQL
    echo "Database created successfully."
else
    echo "Database already exists. Skipping creation."
fi

# Set default search_path for the database
echo "Setting database search_path..."
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d review_service_db -c "ALTER DATABASE review_service_db SET search_path TO review_service, public;" 2>/dev/null || true

echo "Database setup complete. PostgreSQL will automatically run init-db.sql for schema initialization."