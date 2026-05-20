kubectl apply -f k8s/pos-service/db-secret.yaml
kubectl apply -f k8s/pos-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/pos-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/pos-service/configmap.yaml
kubectl apply -f k8s/pos-service/secret.yaml
kubectl apply -f k8s/pos-service/deployment.yaml
kubectl apply -f k8s/pos-service/service.yaml