kubectl apply -f k8s/restaurant-service/db-secret.yaml
kubectl apply -f k8s/restaurant-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/restaurant-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/restaurant-service/configmap.yaml
kubectl apply -f k8s/restaurant-service/secret.yaml
kubectl apply -f k8s/restaurant-service/deployment.yaml
kubectl apply -f k8s/restaurant-service/service.yaml