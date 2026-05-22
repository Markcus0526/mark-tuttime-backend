#!/bin/bash
# remove-all-docker-images.sh
# Lists all Docker images, then deletes them after confirmation.

echo "========================================="
echo "Current Docker images on this system:"
echo "========================================="
docker images

echo ""
echo "WARNING: This will delete ALL Docker images listed above."
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 1
fi

# Stop all running containers (optional, but prevents conflicts)
if [ "$(docker ps -q)" ]; then
    echo "Stopping all running containers..."
    docker stop $(docker ps -q)
fi

# Remove all containers (optional)
if [ "$(docker ps -aq)" ]; then
    echo "Removing all containers..."
    docker rm $(docker ps -aq)
fi

# Remove all images
echo "Removing all Docker images..."
docker rmi -f $(docker images -q) 2>/dev/null

echo "All Docker images have been removed."