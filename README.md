docker compose --env-file .env.docker up xx-service-db

- Healthy check
docker inspect tuttime-api-gateway --format='{{range .State.Health.Log}}{{.Output}}{{end}}'

docker inspect tuttime-api-gateway --format='{{json .State.Health}}'

docker compose config

docker compose up -d --build menu-service

docker compose up -d --force-recreate booking-service

docker compose up -d --build

docker exec tuttime-booking-service printenv | grep ORDER_GRPC_SERVICE

# k3s configuration

# Install K3s on your VPS (fresh install):
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644 --flannel-backend none

sudo cat /etc/rancher/k3s/k3s.yaml

# Copy that to your local ~/.kube/config and change server: https://127.0.0.1:6443 to your VPS public IP.