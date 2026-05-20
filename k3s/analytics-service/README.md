kubectl apply -f k8s/analytics-service/db-secret.yaml
kubectl apply -f k8s/analytics-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/analytics-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/analytics-service/configmap.yaml
kubectl apply -f k8s/analytics-service/secret.yaml
kubectl apply -f k8s/analytics-service/deployment.yaml
kubectl apply -f k8s/analytics-service/service.yaml