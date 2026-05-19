#!/bin/bash

CONTAINER_NAME="swagger-ui"

echo "Stopping container $CONTAINER_NAME..."
docker stop $CONTAINER_NAME 2>/dev/null && echo "Container stopped." || echo "Container was not running or does not exist."

read -p "Do you want to remove the container as well? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker rm $CONTAINER_NAME 2>/dev/null && echo "Container removed." || echo "Container not found or already removed."
fi