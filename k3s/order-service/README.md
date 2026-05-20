kubectl apply -f k8s/order-service/db-secret.yaml
kubectl apply -f k8s/order-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/order-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/order-service/configmap.yaml
kubectl apply -f k8s/order-service/secret.yaml
kubectl apply -f k8s/order-service/deployment.yaml
kubectl apply -f k8s/order-service/service.yaml