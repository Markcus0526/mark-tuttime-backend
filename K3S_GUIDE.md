# TutTime Backend – K3s Deployment & Management Guide

This document provides a complete workflow for deploying the TutTime microservices on a K3s cluster (single‑node Ubuntu VPS). It covers firewall setup, K3s installation, image building, importing into containerd, applying Kubernetes manifests, and day‑to‑day management.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Prepare the Ubuntu VPS Host](#2-prepare-the-ubuntu-vps-host)
3. [Install K3s](#3-install-k3s)
4. [Post‑Installation Configuration](#4-post-installation-configuration)
5. [Verify Core Components](#5-verify-core-components)
6. [Prepare Custom Docker Images](#6-prepare-custom-docker-images)
7. [Import Images into K3s (containerd)](#7-import-images-into-k3s-containerd)
8. [Example: Full rebuild and replace of a service (analytics-service)](#8-example-full-rebuild-and-replace-of-a-service-analytics-service)
9. [Deploy the Manifests](#9-deploy-the-manifests)
10. [Expose the API Gateway](#10-expose-the-api-gateway)
11. [Useful Commands](#11-useful-commands)
    - [Cluster & Nodes](#cluster--nodes)
    - [Namespaces](#namespaces)
    - [Pods](#pods)
    - [Logs & Debugging](#logs--debugging)
    - [Deployments](#deployments)
    - [Services & Networking](#services--networking)
    - [PVCs & Storage](#pvcs--storage)
    - [ConfigMaps & Secrets](#configmaps--secrets)
    - [Ingress & Traefik](#ingress--traefik)
    - [Events & Metrics](#events--metrics)
    - [Apply & Delete Resources](#apply--delete-resources)
12. [Full Reset / Clean Start](#12-full-reset--clean-start)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Prerequisites

- Ubuntu 22.04 or 24.04 VPS with at least 4 CPU cores and 8 GB RAM (8 cores / 24 GB recommended).
- `sudo` access.
- Basic familiarity with Docker, Kubernetes, and command line.

---

## 2. Prepare the Ubuntu VPS Host

Update the system and open required firewall ports.

```bash
sudo apt update && sudo apt upgrade -y

# Open Kubernetes API, Flannel VXLAN, and Kubelet ports
sudo ufw allow 6443/tcp
sudo ufw allow 8472/udp
sudo ufw allow 10250/tcp

# Allow routing on virtual interfaces (critical for pod‑to‑pod communication)
sudo ufw allow in on cni0
sudo ufw allow in on flannel.1

sudo ufw reload
```

---

## 3. Install K3s

### Standard Installation

```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644" sh -
```

Wait about 30 seconds for initialisation.

### Reinstall (if needed)

```bash
sudo /usr/local/bin/k3s-uninstall.sh
sudo rm -rf /var/lib/rancher /etc/rancher ~/.kube

curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--flannel-backend=flannel" sh -
sleep 30
kubectl get nodes
```

---

## 4. Post‑Installation Configuration

### Fix `kubectl` Permissions

```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config

export KUBECONFIG=~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc

kubectl get nodes   # test
```

### Create the TutTime Namespace

```bash
kubectl create namespace tuttime
```

---

## 5. Verify Core Components

Check that the **Local Path Storage Provisioner** and **CoreDNS** are healthy.

```bash
kubectl get storageclass
# Expected: local-path (default)

kubectl get pods -n kube-system -l k3s-app=kube-dns
# Expected: coredns-... Running

kubectl get daemonset -n kube-system | grep -E 'flannel|calico|canal|cni'
# Should show flannel or similar CNI
```

**Test Redis connectivity** (once Redis is deployed):

```bash
kubectl run test --image=redis:7-alpine -it --rm -n tuttime -- redis-cli -h redis -p 6379 ping
```

---

## 6. Prepare Custom Docker Images

### Build an Image for a Service (Example: API Gateway)

```bash
docker build -f api-gateway/docker/Dockerfile -t tuttime/api-gateway:v1 .
```

**For microservices** (e.g., booking‑service):

```bash
docker build -f services/booking-service/docker/Dockerfile -t tuttime/booking-service:v1 .
```

Repeat for all services (user, restaurant, menu, booking, payment, order, pos, review, analytics, notification).

---

## 7. Import Images into K3s (containerd)

K3s uses containerd, not Docker. Import each image after building.

### Single Image Import

```bash
docker save tuttime/api-gateway:v1 | sudo k3s ctr image import -
```

### Automate with a Script (`k3s-import-image-all.sh`)

```bash
#!/bin/bash
for img in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep tuttime); do
    docker save "$img" | sudo k3s ctr image import -
done
```

Make executable and run:

```bash
chmod +x k3s-import-image-all.sh
./k3s-import-image-all.sh
```

**Verify images are present:**

```bash
sudo k3s ctr image ls | grep tuttime
```

---

## 8. Example: Full rebuild and replace of a service (analytics-service)

This example shows how to completely rebuild a service’s Docker image, import it, delete its existing Kubernetes resources, and scale the deployment down to zero. Use the same pattern for any other service.

```bash
# Build the new Docker image
docker build -f services/analytics-service/docker/Dockerfile -t tuttime/analytics-service:v1 .
docker save tuttime/analytics-service:v1 | sudo k3s ctr image import -

# Delete the service’s Kubernetes resources (deployment, service, configmap, secrets)
kubectl delete deployment/analytics-service service/analytics-service configmap/analytics-service-config secret/analytics-service-secrets -n tuttime --force --grace-period=0

# Also delete the database resources (if any)
kubectl delete deployment/analytics-service-db service/analytics-service-db configmap/analytics-service-db-config secret/analytics-service-db-secrets -n tuttime --force --grace-period=0

# Scale the deployment to zero (optional, to ensure no leftover pods)
kubectl scale deployment analytics-service -n tuttime --replicas=0
```

> **Note:** After deleting the resources, you can reapply the service’s YAML files (from `k3s/analytics-service/`) or simply let the deployment be recreated by your usual apply command. The scaling to zero is useful when you want to temporarily stop the service.

Apply the same steps for `notification-service` (or any other) by replacing the service name accordingly.

---

## 9. Deploy the Manifests

All YAML files are located under `k3s/`. Apply in the recommended order.

### Deploy Infrastructure (databases, Kafka, Redis, etc.)

```bash
kubectl apply -f k3s/infra/ -n tuttime
```

Wait for infrastructure pods to become `Running`:

```bash
kubectl get pods -n tuttime -w
```

### Deploy API Gateway

```bash
kubectl apply -f k3s/api-gateway/ -n tuttime
```

### Deploy All Microservices

You can apply each service individually or use Kustomize.

**Individual services:**

```bash
kubectl apply -f k3s/user-service/ -n tuttime
kubectl apply -f k3s/restaurant-service/ -n tuttime
# ... repeat for all services
```

**Using Kustomize (if you have `k3s/kustomization.yaml`):**

```bash
kubectl apply -k k3s/
```

---

## 10. Expose the API Gateway

The gateway is exposed via a NodePort service. Ensure your `k3s/api-gateway/service.yaml` has:

```yaml
spec:
  type: NodePort
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      nodePort: 30080
```

Apply the service:

```bash
kubectl apply -f k3s/api-gateway/service.yaml -n tuttime
```

Now access your API at `http://<VPS‑IP>:30080`.

---

## 11. Useful Commands

### Cluster & Nodes

```bash
kubectl cluster-info
kubectl get nodes
kubectl describe node <node-name>
sudo systemctl status k3s
sudo systemctl restart k3s
sudo journalctl -u k3s -f -n 100
```

### Namespaces

```bash
kubectl get namespaces
kubectl create namespace tuttime
kubectl delete namespace tuttime
kubectl config set-context --current --namespace=tuttime
```

### Pods

```bash
kubectl get pods -n tuttime
kubectl get pods -n tuttime -w
kubectl get pods -n tuttime -o wide
kubectl get pods -n tuttime --show-labels
kubectl delete pod <pod-name> -n tuttime
kubectl delete pods --all -n tuttime
kubectl delete pod <pod-name> -n tuttime --force --grace-period=0
```

### Logs & Debugging

```bash
kubectl logs <pod-name> -n tuttime
kubectl logs -f <pod-name> -n tuttime
kubectl logs <pod-name> -n tuttime --previous
kubectl logs deployment/<deployment-name> -n tuttime --tail=50
kubectl describe pod <pod-name> -n tuttime
```

**Using `stern` for multi‑pod log tailing:**

Install stern:

```bash
cd /tmp
curl -L https://github.com/stern/stern/releases/download/v1.30.0/stern_1.30.0_linux_amd64.tar.gz -o stern.tar.gz
tar xzf stern.tar.gz
sudo mv stern /usr/local/bin/
```

Examples:

```bash
stern . -n tuttime                            # all pods
stern user-service -n tuttime                 # specific pod
stern "user|restaurant" -n tuttime            # regex
stern . -n tuttime --exclude "mongodb|redis"  # exclude
stern . -n tuttime --tail 10                  # last 10 lines
stern api-gateway -n tuttime -t               # with timestamps
```

### Deployments

```bash
kubectl get deployments -n tuttime
kubectl describe deployment <deployment-name> -n tuttime
kubectl rollout restart deployment/<deployment-name> -n tuttime
kubectl rollout status deployment/<deployment-name> -n tuttime
kubectl scale deployment/<deployment-name> --replicas=3 -n tuttime
kubectl rollout history deployment/<deployment-name> -n tuttime
kubectl rollout undo deployment/<deployment-name> -n tuttime
```

### Services & Networking

```bash
kubectl get svc -n tuttime
kubectl describe svc <service-name> -n tuttime
kubectl port-forward -n tuttime service/api-gateway 8080:8080
kubectl get endpoints -n tuttime
```

### PVCs & Storage

```bash
kubectl get pvc -n tuttime
kubectl describe pvc <pvc-name> -n tuttime
kubectl get pv
kubectl delete pvc <pvc-name> -n tuttime
```

### ConfigMaps & Secrets

```bash
kubectl get configmap -n tuttime
kubectl get configmap <configmap-name> -n tuttime -o yaml
kubectl edit configmap <configmap-name> -n tuttime
kubectl create configmap my-config --from-literal=KEY=value -n tuttime

kubectl get secrets -n tuttime
kubectl get secret <secret-name> -n tuttime -o yaml
# Decode a secret value
kubectl get secret api-gateway-secrets -n tuttime -o jsonpath='{.data.JWT_SECRET}' | base64 -d
```

### Ingress & Traefik

```bash
kubectl get ingress -n tuttime
kubectl describe ingress <ingress-name> -n tuttime
kubectl get pods -n kube-system | grep traefik
kubectl logs -n kube-system deployment/traefik
```

### Events & Metrics

```bash
kubectl get events --all-namespaces --sort-by='.lastTimestamp'
kubectl get events -n tuttime --sort-by='.lastTimestamp'
kubectl get events -n tuttime -w
kubectl top nodes
kubectl top pods -n tuttime
```

### Apply & Delete Resources

```bash
kubectl apply -f k3s/infra/ -n tuttime
kubectl apply -k k3s/
kubectl delete -f k3s/infra/ -n tuttime
kubectl delete pods -l app=api-gateway -n tuttime
kubectl delete all --all -n tuttime
```

### Image Import & containerd

```bash
# Import a single image
docker save tuttime/api-gateway:v1 | sudo k3s ctr image import -

# List images
sudo k3s ctr image ls | grep tuttime

# Remove an image
sudo k3s ctr image rm docker.io/tuttime/api-gateway:v1
```

### Check Pod Termination Reason

```bash
kubectl get pod -l app=user-service -n tuttime -o jsonpath='{.items[0].status.containerStatuses[0].lastState.terminated}'
```

### Force Delete a Service’s Resources

```bash
kubectl delete deployment/booking-service service/booking-service configmap/booking-service-config secret/booking-service-secrets -n tuttime --force --grace-period=0
```

### Restart a Deployment

```bash
kubectl rollout restart deployment/api-gateway -n tuttime
```

---

## 12. Full Reset / Clean Start

Delete the entire namespace and recreate everything.

```bash
kubectl delete namespace tuttime
kubectl create namespace tuttime
kubectl apply -f k3s/infra/ -n tuttime
kubectl apply -f k3s/api-gateway/ -n tuttime
kubectl apply -f k3s/ -n tuttime   # or apply each service
```

**Complete K3s uninstall** (if you want to start over on the node):

```bash
sudo /usr/local/bin/k3s-uninstall.sh
sudo rm -rf /etc/rancher /var/lib/rancher ~/.kube
sudo iptables -F
sudo systemctl restart ssh
```

---

## 13. Troubleshooting

### Pod stuck in `Pending`

- Check PVC binding: `kubectl get pvc -n tuttime`
- Ensure `storageClassName: local-path` is set in PVCs.
- Check node capacity: `kubectl describe node`

### ImagePullBackOff

- Import the missing image into containerd (see Section 7).
- Set `imagePullPolicy: IfNotPresent` in the deployment.

### CrashLoopBackOff

- Check logs: `kubectl logs <pod> -n tuttime --previous`
- Check termination reason (command above).
- Verify ConfigMap and Secret values are correct.

### Redis / Network Issues

- Test connectivity from a temporary pod:
  ```bash
  kubectl run test --image=busybox -it --rm -n tuttime -- nc -zv redis 6379
  ```
- Restart flannel: `kubectl delete pod -n kube-system -l k3s-app=flannel`

### Firewall Lockout (SSH lost)

If K3s locks you out, connect via VPS console and reset iptables:

```bash
sudo iptables -P INPUT ACCEPT
sudo iptables -P FORWARD ACCEPT
sudo iptables -P OUTPUT ACCEPT
sudo iptables -t nat -F
sudo iptables -t mangle -F
sudo iptables -F
sudo iptables -X
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo netfilter-persistent save
```

---

**Your K3s cluster is now ready to host the TutTime backend.**  
Use this guide as a reference for daily operations and troubleshooting.
```
