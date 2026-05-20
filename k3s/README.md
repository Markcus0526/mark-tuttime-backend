
# Steps to reinstall K3s:

## Alternately:
```bash
# 1. Uninstall K3s completely
sudo /usr/local/bin/k3s-uninstall.sh
sudo rm -rf /var/lib/rancher /etc/rancher ~/.kube

# 2. Reinstall K3s with explicit flannel backend
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--flannel-backend=flannel" sh -

# 3. Wait for the node to become Ready (about 30 seconds)
sleep 30
kubectl get nodes

# 4. Verify flannel pod is running
kubectl get pods -n kube-system | grep flannel
```
Then test Redis connectivity:
```bash
kubectl run test --image=redis:7-alpine -it --rm -n tuttime -- redis-cli -h redis -p 6379 ping
```


## Ubuntu VPS hosting a microservices backend
## Step 1: Prepare the Ubuntu VPS Host
Before installing K3s, you must open the required firewall ports so your microservices can communicate internally over the virtual network.
```bash
# 1. Update your system repositories
sudo apt update && sudo apt upgrade -y

# 2. Open Kubernetes API and CNI overlay ports in UFW
sudo ufw allow 6443/tcp   # Kubernetes API Server
sudo ufw allow 8472/udp   # Flannel VXLAN Overlay Network
sudo ufw allow 10250/tcp  # Kubelet Metrics

# 3. Allow routing on virtual interfaces (Crucial for Redis/Gateway connection)
sudo ufw allow in on cni0
sudo ufw allow in on flannel.1

# 4. Reload firewall
sudo ufw reload
```

## Step 2: Install K3s with Production Parameters
On a standard VPS, you want to ensure K3s utilizes the local file system cleanly for your Persistent Volume Claims (like your redis-pvc). Run the optimized one-liner installation script:
```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644" sh -
```

## Step 3: Configure User Access & Verify Health
Give K3s about 30 seconds to initialize its internal engine, then copy the access keys into your user's home profile directory:
```bash
# 1. Create standard Kubernetes configuration directory
mkdir -p ~/.kube

# 2. Copy the active cluster keys
cp /etc/rancher/k3s/k3s.yaml ~/.kube/config

# 3. Verify that your VPS node is online and healthy
kubectl get nodes
```
Expected Output: Your host machine should be listed with a Status of Ready.

## Step 4: Verify the Internal CoreDNS & Storage Services
Before deploying your microservice stack, confirm that K3s's internal network layer and storage driver initialized flawlessly:
```bash
# 1. Ensure the Local Path Storage Provisioner is active (for your Redis PVC)
kubectl get storageclass
```
Expected Output: You must see local-path (default) in the list.

```bash
# 2. Ensure CoreDNS is running to manage microservice naming
kubectl get pods -n kube-system -l k8s-app=kube-dns
```
Expected Output: The coredns-... pod status must be Running.

## Step 5: Initialize Your Custom Namespace
Now that the system is fully configured, create the custom namespace you used in your configuration files to isolate your backend application environment:
```bash
kubectl create namespace tuttime
```
Your Ubuntu VPS is now a perfectly healthy, freshly provisioned K3s single-node cluster ready to host your microservices.



# Verify CNI absence

```bash
kubectl get daemonset -n kube-system | grep -E 'flannel|calico|canal|cni'
```

## Install stern
```bash
# Install stern first, then use it:
cd /tmp
curl -L https://github.com/stern/stern/releases/download/v1.30.0/stern_1.30.0_linux_amd64.tar.gz -o stern.tar.gz
tar xzf stern.tar.gz
sudo mv stern /usr/local/bin/
stern --version

# Then:
stern . -n tuttime
```

Filter stern logs by:

Specific pod: stern user-service -n tuttime

Multiple pods: stern "user|restaurant" -n tuttime (regex)

Exclude pod: stern . -n tuttime --exclude "mongodb|redis"

Tail last N lines: stern . -n tuttime --tail 10

Follow only new logs (no tail): stern . -n tuttime --tail 0

Show timestamps: stern . -n tuttime -t

Output to file: stern . -n tuttime > logs.txt

Example: stern "user-service|api-gateway" -n tuttime --tail 20 -t

## Run it:
```bash
chmod +x docker-image-all.sh
./docker-image-all.sh
```
```bash
kubectl rollout restart deployment/restaurant-service -n tuttime

kubectl delete pods -n tuttime -l app=api-gateway
```


## Import images into K3s (containerd)
```bash
chmod +x import-to-k3s.sh
```

### Verify images are in containerd:
```bash
sudo k3s ctr image ls | grep tuttime
```

### Fix kubectl permissions
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config
export KUBECONFIG=~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
kubectl get nodes   # test
```

## Deploy the Kubernetes manifests
```bash
# Create namespace
kubectl create namespace tuttime

# Apply infrastructure (databases, Kafka, Redis, etc.)
kubectl apply -f k8s/infra/ -n tuttime

# Wait for infrastructure to be ready
kubectl get pods -n tuttime -w

# Apply API gateway
kubectl apply -f k8s/api-gateway/ -n tuttime

# Apply all microservices
kubectl apply -f k8s/ -n tuttime
./import-to-k3s.sh
```

## Expose API gateway externally (NodePort)
```bash
kubectl apply -f k8s/api-gateway/service.yaml -n tuttime
```
Now access your API at http://<VPS-IP>:30080

## Monitor
```bash
kubectl get pods -n tuttime -w
kubectl logs -f <pod-name> -n tuttime
```

# Full Useful Commands for K3s & TutTime

## Cluster & Node Management
```bash
# Get cluster info
kubectl cluster-info

# List all nodes
kubectl get nodes

# Describe a node (resource usage, taints, conditions)
kubectl describe node <node-name>

# Check K3s service status
sudo systemctl status k3s

# Restart K3s
sudo systemctl restart k3s

# View K3s logs
sudo journalctl -u k3s -f -n 100
```

## Namespace Operations
```bash
# List namespaces
kubectl get namespaces

# Create a namespace
kubectl create namespace tuttime

# Delete a namespace (removes all resources inside)
kubectl delete namespace tuttime

# Switch default namespace (optional)
kubectl config set-context --current --namespace=tuttime
```

## Pods
```bash
# List all pods in namespace
kubectl get pods -n tuttime

# Watch pods (live updates)
kubectl get pods -n tuttime -w

# List pods with more details (IP, node)
kubectl get pods -n tuttime -o wide

# Show pods with labels
kubectl get pods -n tuttime --show-labels

# Delete a pod (will be recreated by its controller)
kubectl delete pod <pod-name> -n tuttime

# Delete all pods in namespace
kubectl delete pods --all -n tuttime

# Force delete a pod (if stuck)
kubectl delete pod <pod-name> -n tuttime --force --grace-period=0
```

## Logs & Debugging
```bash
# View logs of a running pod
kubectl logs <pod-name> -n tuttime

# Stream logs (follow)
kubectl logs -f <pod-name> -n tuttime

# View previous container logs (after restart)
kubectl logs <pod-name> -n tuttime --previous

# View logs for all pods of a deployment
kubectl logs deployment/<deployment-name> -n tuttime --tail=50

# Describe a pod (events, status, conditions)
kubectl describe pod <pod-name> -n tuttime

# Run a temporary debug pod
kubectl run debug --image=busybox -it --rm -n tuttime -- sh
```

## Deployments & Controllers
```bash
# List deployments
kubectl get deployments -n tuttime

# Describe a deployment
kubectl describe deployment <deployment-name> -n tuttime

# Restart a deployment
kubectl rollout restart deployment/<deployment-name> -n tuttime

# Check rollout status
kubectl rollout status deployment/<deployment-name> -n tuttime

# Scale replicas
kubectl scale deployment/<deployment-name> --replicas=3 -n tuttime

# See revision history
kubectl rollout history deployment/<deployment-name> -n tuttime

# Rollback to previous revision
kubectl rollout undo deployment/<deployment-name> -n tuttime
```

## Services & Networking
```bash
# List services
kubectl get svc -n tuttime

# Describe a service
kubectl describe svc <service-name> -n tuttime

# Port‑forward a service to local machine
kubectl port-forward -n tuttime service/api-gateway 8080:8080

# Get endpoints for a service
kubectl get endpoints -n tuttime
```

## Persistent Volumes & Claims (PVC/PV)
```bash
# List PVCs
kubectl get pvc -n tuttime

# Describe a PVC (check binding errors)
kubectl describe pvc <pvc-name> -n tuttime

# List PersistentVolumes
kubectl get pv

# Delete a PVC (may delete bound PV depending on reclaim policy)
kubectl delete pvc <pvc-name> -n tuttime
```

## ConfigMaps & Secrets
```bash
# List ConfigMaps
kubectl get configmap -n tuttime

# View a ConfigMap (yaml)
kubectl get configmap <configmap-name> -n tuttime -o yaml

# Edit a ConfigMap
kubectl edit configmap <configmap-name> -n tuttime

# Create a ConfigMap from literal values
kubectl create configmap my-config --from-literal=KEY=value -n tuttime

# List Secrets
kubectl get secrets -n tuttime

# View a Secret (base64 encoded)
kubectl get secret <secret-name> -n tuttime -o yaml

# Decode a secret value (example for JWT_SECRET)
kubectl get secret api-gateway-secrets -n tuttime -o jsonpath='{.data.JWT_SECRET}' | base64 -d
```

## Ingress & Traefik
```bash
# List ingresses
kubectl get ingress -n tuttime

# Describe an ingress
kubectl describe ingress <ingress-name> -n tuttime

# Check Traefik pods (system)
kubectl get pods -n kube-system | grep traefik

# View Traefik logs
kubectl logs -n kube-system deployment/traefik

# Change Traefik ports (via HelmChartConfig – see README)
```

## Events & Metrics
```bash
# List cluster events (sorted)
kubectl get events --all-namespaces --sort-by='.lastTimestamp'

# Events in namespace
kubectl get events -n tuttime --sort-by='.lastTimestamp'

# Watch events in real time
kubectl get events -n tuttime -w

# If metrics-server is installed, get resource usage
kubectl top nodes
kubectl top pods -n tuttime
```

## Apply & Delete Resources
```bash
# Apply all YAMLs in a directory (or single file)
kubectl apply -f k8s/infra/ -n tuttime

# Apply using Kustomize (if kustomization.yaml exists)
kubectl apply -k k8s/

# Delete resources by file/directory
kubectl delete -f k8s/infra/ -n tuttime

# Delete a specific resource type by label
kubectl delete pods -l app=api-gateway -n tuttime

# Delete all resources in the namespace (but keep namespace)
kubectl delete all --all -n tuttime
```

## Image Import & containerd
```bash
# Import a single image from Docker into containerd (K3s runtime)
docker save tuttime/api-gateway:v1 | sudo k3s ctr image import -

# List images in containerd
sudo k3s ctr image ls | grep tuttime

# Remove an image from containerd (if needed)
sudo k3s ctr image rm docker.io/tuttime/api-gateway:v1
```

## Full Reset / Clean Start
```bash
# Delete the entire namespace (all resources, PVCs, secrets, etc.)
kubectl delete namespace tuttime

# Wait for termination, then recreate namespace
kubectl create namespace tuttime

# Reapply everything
kubectl apply -f k8s/infra/ -n tuttime
# … then api-gateway, then other services
```

## Alternatively, uninstall K3s completely
```bash
sudo /usr/local/bin/k3s-uninstall.sh
sudo rm -rf /etc/rancher /var/lib/rancher ~/.kube
sudo iptables -F
sudo systemctl restart ssh
```

## SSH & Firewall (if K3s locked you out)
```bash
# From VPS console (out-of-band), reset iptables
sudo iptables -P INPUT ACCEPT
sudo iptables -P FORWARD ACCEPT
sudo iptables -P OUTPUT ACCEPT
sudo iptables -t nat -F
sudo iptables -t mangle -F
sudo iptables -F
sudo iptables -X

# Then add SSH rule
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# Save iptables rules (if using iptables-persistent)
sudo netfilter-persistent save
```

## Quick Health Check
```bash
# One-liner to see all important resources
kubectl get all -n tuttime

# Watch only problematic pods
kubectl get pods -n tuttime --field-selector status.phase!=Running

# Show pods with restart counts
kubectl get pods -n tuttime --sort-by='.status.containerStatuses[0].restartCount'
```