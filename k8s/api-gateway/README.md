# Create namespace
kubectl create namespace tuttime

# Apply api-gateway manifests
kubectl apply -f k8s/api-gateway/

# Check status
kubectl get pods -n tuttime
kubectl logs -f deployment/api-gateway -n tuttime

Inside the cluster, other services can reach the gateway at http://api-gateway.tuttime.svc.cluster.local:8080.

If you use the Ingress, ensure you have an Ingress controller installed (e.g., kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.0/deploy/static/provider/cloud/deploy.yaml).

