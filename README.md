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

Recommended Order (from earliest to latest)
Order	Service	Reason / Dependencies
1	user-service	No dependency on other app services. Provides authentication and user data.
2	restaurant-service	No dependency on other app services. Provides restaurant, branch, table data.
3	menu-service	Depends on restaurant-service (to know which restaurant/branch).
4	review-service	Depends on user-service and restaurant-service (reviews need users and restaurants).
5	booking-service	Depends on user-service, restaurant-service, menu-service (for pre‑orders).
6	payment-service	Depends on booking-service (payments are tied to a booking).
7	order-service	Depends on booking-service, menu-service, payment-service.
8	pos-service	Depends on order-service and menu-service (synchronises orders to POS).
9	notification-service	Consumes events from many services, but can start anytime. Placing it after core services ensures it can respond to events immediately.
10	analytics-service	Also consumes events; can start anytime. Start after core services.
11	api-gateway	Should be started last, after all backend services are ready to accept traffic.