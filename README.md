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
- [Testing](#testing)
- [Operations & Runbooks](#operations--runbooks)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

TutTime consists of 10 microservices plus an API Gateway:

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

Infrastructure: PostgreSQL (each service has its own DB), Redis (caching, rate limiting, booking locks, token whitelist), Kafka (event bus), Zipkin (tracing), Graylog (logging), OpenSearch + MongoDB (Graylog backend).

**Production / HA posture (k3s):**
- **Kafka** — 3-broker KRaft cluster with SASL/SCRAM-SHA-256, RF=3 / minISR=2, controlled topic creation (auto-create off), and per-topic `.DLT` dead-letter topics. Consumers rethrow on failure so the DLQ engages.
- **Redis** — 1 master + 2 replicas + 3 Sentinels (automatic failover); clients are Sentinel-aware.
- **Edge** — Traefik ingress with cert-manager TLS (`letsencrypt-production`) + security-headers middleware.
- **Security** — per-DB + app-tier NetworkPolicies, non-root pods (`runAsNonRoot`, seccomp), gateway rate limiting + brute-force protection (fail-closed), and optional service-to-service **mTLS via Linkerd**.
- **Stateless tier** runs ≥2 replicas. See the runbooks in [`docs/`](docs/).

> See [docs/HA_INFRA.md](docs/HA_INFRA.md) (Kafka/Redis), [docs/MTLS.md](docs/MTLS.md), [docs/DB_HA.md](docs/DB_HA.md) (CloudNativePG), and [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md) (backup/restore).

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

All Kubernetes YAMLs are in `k3s/` and are wired together with **Kustomize** — this is the
supported way to deploy everything (infra, services, edge TLS, NetworkPolicies):

```bash
kubectl create namespace tuttime
kubectl apply -k k3s/

# Wait for HA infra to stabilise before the services hammer it:
kubectl -n tuttime rollout status statefulset/tuttime-redis
kubectl -n tuttime rollout status statefulset/tuttime-redis-sentinel
kubectl -n tuttime rollout status statefulset/tuttime-kafka-cluster
kubectl -n tuttime wait --for=condition=complete job/kafka-topic-initializer --timeout=300s
```

You can preview the fully-rendered manifests offline with `kubectl kustomize k3s/`.

**Prerequisites that are NOT applied by Kustomize** (install first; see the linked runbooks):
- **cert-manager** — required by `infra/cluster-issuer.yaml` for edge TLS.
- **CloudNativePG operator** — required only if you adopt DB HA ([docs/DB_HA.md](docs/DB_HA.md)).
- **Linkerd** — required only if you enable service mesh mTLS ([docs/MTLS.md](docs/MTLS.md)).
- **Real secrets** — the committed `*-secret.yaml` / `global-infra-secret.yaml` hold default
  credentials; replace them out-of-band before production (do not commit real secrets).
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
| `SPRING_PROFILES_ACTIVE` | `k3s` for the cluster, `graylog-enabled` for logging |
| `JWT_SECRET` | Secret key for JWT signing                          |
| `SPRING_DATA_REDIS_SENTINEL_MASTER` | Sentinel master name (`tuttime-redis-master`)        |
| `SPRING_DATA_REDIS_SENTINEL_NODE_1..3` | Sentinel endpoints (`tuttime-redis-sentinel-N.tuttime.svc.cluster.local:26379`) |
| `SPRING_DATA_REDIS_PASSWORD` | Redis auth (`= INFRA_REDIS_PASSWORD`)                |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap (`kafka:9093`)                       |
| `SPRING_KAFKA_SECURITY_PROTOCOL` | `SASL_PLAINTEXT` on the cluster                      |
| `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` | Per-service SCRAM credentials (in each Secret)       |
| `YOOKASSA_SHOP_ID` / `YOOKASSA_SECRET_KEY` | Payment gateway credentials                         |
| `GOOGLE_CLIENT_ID` / `APPLE_CLIENT_ID` | OAuth / email sender credentials                    |
| `RATE_LIMIT_REPLENISH_RATE` / `RATE_LIMIT_BURST_CAPACITY` | Gateway Redis rate-limiter (tokens/sec, burst) |
| `BRUTEFORCE_FAIL_OPEN` | Auth brute-force behaviour on Redis outage (`false` = fail-closed) |
| `SPRINGDOC_ENABLED` | Gateway Swagger/API-docs toggle (`false` in cluster) |
| `APP_TEST_ENDPOINTS_ENABLED` | Enables `/payments/test/**` (dev only; `false`/unset in cluster) |

> Cluster-wide Kafka SCRAM users/passwords live in `k3s/infra/global-infra-config*` /
> `global-infra-secret*` (lists are positionally aligned). Defaults are placeholders — replace before production.
---

## Testing

Unit tests run in the CI pipeline (`mvn clean test`) and locally:

```bash
mvn clean test                                   # all modules
mvn clean test -pl services/booking-service -am  # a single service (+ its deps)
```

> Tests use JUnit 5 + Mockito (via `spring-boot-starter-test`). The root POM pins
> `maven-surefire-plugin` 3.x so Jupiter tests are actually discovered — older surefire silently
> reports `Tests run: 0`.

---

## Operations & Runbooks

Detailed, step-by-step guides for the production/HA concerns live in [`docs/`](docs/):

| Runbook | Covers |
|---------|--------|
| [docs/HA_INFRA.md](docs/HA_INFRA.md) | 3-broker Kafka (SASL) + Redis Sentinel: apply order, validation, rollback |
| [docs/MTLS.md](docs/MTLS.md) | Service-to-service mTLS with Linkerd: install, verify, caveats |
| [docs/DB_HA.md](docs/DB_HA.md) | PostgreSQL HA via CloudNativePG: operator install, per-service cutover |
| [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md) | Backup (pg_dump CronJob), restore, full-cluster rebuild, RPO/RTO |

> ⚠️ The HA/mTLS/DB-HA manifests render with `kubectl kustomize` but must be validated on a test
> cluster (quorum/failover/cert issuance) before production.

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
