kubectl apply -f k8s/user-service/db-secret.yaml
kubectl apply -f k8s/user-service/db-init-configmap.yaml
kubectl apply -f k8s/user-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/user-service/configmap.yaml
kubectl apply -f k8s/user-service/secret.yaml
kubectl apply -f k8s/user-service/deployment.yaml
kubectl apply -f k8s/user-service/service.yaml