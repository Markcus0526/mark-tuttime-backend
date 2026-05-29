# Monitoring TutTime with Prometheus & Grafana on K3s

This guide explains how to deploy a full monitoring stack (Prometheus + Grafana) on your K3s cluster, configure it to scrape metrics from your Spring Boot microservices, and visualise the data.

---

## Prerequisites

- K3s cluster up and running.
- `kubectl` configured.
- Helm 3 installed (`sudo snap install helm --classic`).

---

## 1. Install the kube-prometheus-stack (includes Prometheus, Grafana, Alertmanager)

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

---

## 2. Access Grafana

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

---

## 3. Configure Spring Boot Services to Expose Metrics

Each microservice must have the following dependency (already in your `pom.xml`):

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

And ensure the actuator endpoint is enabled in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

After redeploying the services, Prometheus will automatically discover them because the `kube-prometheus-stack` includes a `PodMonitor` that scrapes any pod with the annotation `prometheus.io/scrape: "true"`. Your `deployment.yaml` files should already have this annotation (as shown in earlier manifests).

---

## 4. Default Dashboards

The stack comes with many pre‑configured dashboards for Kubernetes clusters. To view them:

- Log into Grafana.
- Click the **Dashboards** icon (four squares) → **Browse**.
- Look for dashboards like:
    - **Kubernetes / Compute Resources / Pod**
    - **Kubernetes / Networking / Pod**
    - **Spring Boot Statistics** (if you import a Spring Boot dashboard)

---

## 5. Import a Spring Boot Dashboard

To see detailed JVM, HTTP request, and database metrics:

1. In Grafana, go to **Dashboards** → **Import**.
2. Enter dashboard ID **12900** (Spring Boot 2.1+ Statistics) or **4701** (Spring Boot 2.1+).
3. Select the Prometheus data source (named `Prometheus`).
4. Click **Import**.

Now you can see metrics from each of your microservices.

---

## 6. (Optional) Expose Prometheus externally

If you want to query Prometheus directly, use the NodePort service:

```bash
kubectl get svc -n monitoring monitoring-kube-prometheus-prometheus
```

Access at `http://<VPS‑IP>:32001`.

---

## 7. Troubleshooting

- **No metrics appear**: Ensure your services have the Prometheus annotation (`prometheus.io/scrape: "true"`) and that the `kube-prometheus-stack` `PodMonitor` is correctly selecting them.
- **Check if Prometheus is scraping**:
  ```bash
  kubectl port-forward -n monitoring service/monitoring-kube-prometheus-prometheus 9090:9090
  ```
  Then open `http://localhost:9090/targets` – you should see your services as targets.
- **Grafana not reachable**: Verify the NodePort service is running and the VPS firewall allows the port (e.g., `sudo ufw allow 32000/tcp`).

---

## 8. Clean Up

To remove the monitoring stack:

```bash
helm uninstall monitoring -n monitoring
kubectl delete namespace monitoring
```

---

Now you have a professional monitoring system for your TutTime backend. Use it to track pod CPU/memory usage, request latencies, error rates, and more.
```
