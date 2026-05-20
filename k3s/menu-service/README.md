kubectl apply -f k8s/menu-service/db-secret.yaml
kubectl apply -f k8s/menu-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/menu-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/menu-service/configmap.yaml
kubectl apply -f k8s/menu-service/secret.yaml
kubectl apply -f k8s/menu-service/deployment.yaml
kubectl apply -f k8s/menu-service/service.yaml