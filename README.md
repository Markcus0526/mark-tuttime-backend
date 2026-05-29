# TutTime Backend

TutTime is a pre‑order and smart restaurant booking platform that combines online table reservations, menu pre‑orders, and dynamic discounts during off‑peak hours. It integrates with POS systems (iiko, R‑Keeper) and provides a yield management solution for restaurants.

---

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Development with Docker Compose](#development-with-docker-compose)
- [Production with K3s (Kubernetes)](#production-with-k3s-kubernetes)
  - [Install K3s](#install-k3s)
  - [Build and Import Images](#build-and-import-images)
  - [Deploy Manifests](#deploy-manifests)
  - [Expose API Gateway](#expose-api-gateway)
  - [Monitoring in K3s](#environment-variables--configuration)
- [Environment Variables & Configuration](#environment-variables--configuration)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

TutTime consists of 11 microservices plus an API Gateway:

| Service | Port (REST) | gRPC Port | Description |
|---------|-------------|-----------|-------------|
| api-gateway | 8080 | – | Spring Cloud Gateway, JWT auth, rate limiting |
| user-service | 28001 | 29001 | User management, authentication, OAuth2 |
| restaurant-service | 28002 | 29002 | Restaurants, branches, tables, schedules, PostGIS |
| menu-service | 28003 | 29003 | Menu categories, items, modifiers, discounts |
| booking-service | 28004 | 29004 | Table reservations, slot management, Redis locks |
| payment-service | 28006 | 29006 | YooKassa integration, payments, refunds |
| order-service | 28005 | 29005 | Pre‑orders, order status, kitchen sync |
| pos-service | 28007 | 29007 | iiko / R‑Keeper integration |
| review-service | 28008 | 29008 | Reviews, ratings, moderation |
| analytics-service | 28009 | 29009 | Event aggregation, reports, forecasting |
| notification-service | 28010 | 29010 | Email, SMS, push, WebSocket notifications |

Infrastructure: PostgreSQL (each service has its own DB), Redis (caching & rate limiting), Kafka (event bus), Zipkin (tracing), Graylog (logging), OpenSearch + MongoDB (Graylog backend).

---

## Tech Stack

- **Java 21** with Spring Boot 3.3.13
- **Spring Cloud Gateway** (WebFlux reactive gateway)
- **Spring Security** + JWT
- **Spring Data JPA** + Hibernate + PostGIS
- **gRPC** with `grpc-spring-boot-starter`
- **Apache Kafka** for event‑driven communication
- **Redis** (Lettuce) for caching & rate limiting
- **Resilience4j** for circuit breakers & retries
- **Micrometer** + Prometheus + Grafana (optional)
- **MapStruct** + Lombok
- **Maven** multi‑module project

---

## Project Structure

```markdown
tuttime-backend/
├── api-gateway/
├── common-libs/
│ ├── common/ # shared DTOs, utils
│ ├── common-event/ # gRPC stubs, Kafka events
│ └── common-auth/ # JWT & security utilities
├── services/
│ ├── user-service/
│ ├── restaurant-service/
│ ├── menu-service/
│ ├── booking-service/
│ ├── payment-service/
│ ├── order-service/
│ ├── pos-service/
│ ├── review-service/
│ ├── analytics-service/
│ └── notification-service/
├── infrastructure/ # Docker Compose for dev
├── k3s/ # Kubernetes manifests (Kustomize)
├── scripts/ # Helper scripts (build, import, deploy)
├── docker-compose.yml # root compose (optional)
└── pom.xml # parent Maven POM
```

---

## Prerequisites

- **Ubuntu 22.04 / 24.04** VPS (8+ CPU, 24+ GB RAM recommended)
- **Docker** & **Docker Compose** (for development)
- **K3s** (for production cluster)
- **kubectl** (configured for K3s)
- **Maven** 3.9+ with Java 21
- **Git**

---

## Development with Docker Compose

For local development or quick testing, use Docker Compose.

```bash
# Clone the repository
git clone https://github.com/your-org/tuttime-backend.git
cd tuttime-backend

# Start all services (infrastructure + microservices)
docker compose up -d --build

# Watch logs
docker compose logs -f
```
Services are exposed on ports 28001–28010 (REST) and 29001–29010 (gRPC). The API Gateway is on port 8080.
To stop:

```bash
docker compose down
```
---

## Production with K3s (Kubernetes)

Install K3s

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--flannel-backend=flannel --write-kubeconfig-mode 644" sh -
```

Configure kubectl for your user:

```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config
export KUBECONFIG=~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
```
---

## Build and Import Custom Images

First, build all JARs (outside Docker):

```bash
mvn clean install -DskipTests
```

Then build each service’s Docker image and import into K3s containerd. Use the helper script:

```bash
chmod +x scripts/linux/build-all-images.sh
./scripts/linux/build-all-images.sh
```

Or do it manually:

```bash
docker build -f services/user-service/docker/Dockerfile -t tuttime/user-service:v1 .
docker save tuttime/user-service:v1 | sudo k3s ctr image import -
```
---

## Deploy Manifests

All Kubernetes YAMLs are in k3s/. Apply in order:

```bash
kubectl create namespace tuttime
kubectl apply -f k8s/infra/ -n tuttime
kubectl apply -f k8s/api-gateway/ -n tuttime
kubectl apply -f k8s/user-service/ -n tuttime
# ... repeat for other services
```

Alternatively, use Kustomize:

```bash
kubectl apply -k k8s/
```
---

## Expose API Gateway

The gateway Service uses type: NodePort with nodePort: 30080. Access your API at:

```bash
http://<VPS-IP>:30080
```
---

## Monitoring in K3s

### Install the kube-prometheus-stack (includes Prometheus, Grafana, Alertmanager)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Create a namespace for monitoring
kubectl create namespace monitoring

# Install the stack
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set grafana.service.type=NodePort \
  --set grafana.service.nodePort=32000 \
  --set prometheus.service.type=NodePort \
  --set prometheus.service.nodePort=32001
```

> **Note:** The `NodePort` settings allow you to access Grafana and Prometheus directly from your VPS IP. Adjust ports if needed (must be in range 30000-32767).

### Access Grafana

After installation, get the Node IP and port:

```bash
kubectl get svc -n monitoring monitoring-grafana
```

Open your browser at `http://<VPS‑IP>:32000` (or the nodePort you set).

- **Username:** `admin`
- **Password:** To retrieve the auto‑generated password:

```bash
kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

> Change the password after first login.

### Default Dashboards

The stack comes with many pre‑configured dashboards for Kubernetes clusters. To view them:

- Log into Grafana.
- Click the **Dashboards** icon (four squares) → **Browse**.
- Look for dashboards like:
  - **Kubernetes / Compute Resources / Pod**
  - **Kubernetes / Networking / Pod**
  - **Spring Boot Statistics** (if you import a Spring Boot dashboard)

### Import a Spring Boot Dashboard

To see detailed JVM, HTTP request, and database metrics:

1. In Grafana, go to **Dashboards** → **Import**.
2. Enter dashboard ID **12900** (Spring Boot 2.1+ Statistics) or **4701** (Spring Boot 2.1+).
3. Select the Prometheus data source (named `Prometheus`).
4. Click **Import**.

Now you can see metrics from each of your microservices.

---

## Environment Variables & Configuration

Each microservice uses a ConfigMap (for non‑sensitive) and a Secret (for sensitive values). Key variables:

| Variable | Purpose                                             |
|----------|-----------------------------------------------------|
| `DATABASE_URL` | PostgreSQL JDBC URL (includes schema)               |
| `SPRING_PROFILES_ACTIVE` | `k8s` for Kubernetes, `graylog-enabled` for logging |
| `JWT_SECRET` | Secret key for JWT signing                          |
| `SPRING_DATA_REDIS_HOST` | Redis service name (`redis`)                        |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker (`kafka:9093`)                         |
| `YOOKASSA_SHOP_ID` / `YOOKASSA_SECRET_KEY` | Payment gateway credentials                         |
| `GOOGLE_CLIENT_ID` / `APPLE_CLIENT_ID` | Email Sender credentials                    |
---

## Useful Commands

### Build images (all services)

```bash
./scripts/linux/docker-image-build-all.sh
```

### Delete images (all services)

```bash
./scripts/linux/docker-image-remove-all.sh
```

### Start Plain Docker mode (all services)

```bash
./scripts/linux/docker-start-all.sh
```

### Stop all Docker (all services)

```bash
./scripts/linux/docker-stop-all.sh
```

### Cleanup and reinstall K3s

```bash
./scripts/linux/k3s-reinstall.sh
```

### Import images into K3s containerd

```bash
./scripts/linux/import-image-all.sh
```

### Deploy everything from scratch

```bash
./scripts/linux/k3s-deploy-all.sh
```

### Start and test API Endpoint in Docker mode

```bash
./scripts/linux/swagger-ui-run.sh
```

Swagger UI (aggregated) is available at http://<VPS-IP>:8080/swagger-ui.html if the gateway serves it, or per‑service at http://<VPS-IP>:28001/swagger-ui.html etc.
