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