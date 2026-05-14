#!/bin/bash
for img in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep tuttime); do
  echo "Importing $img"
  docker save "$img" -o /tmp/$(echo $img | tr '/' '_').tar
  sudo k3s ctr image import /tmp/$(echo $img | tr '/' '_').tar
done