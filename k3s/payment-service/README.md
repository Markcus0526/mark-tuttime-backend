kubectl apply -f k8s/payment-service/db-secret.yaml
kubectl apply -f k8s/payment-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/payment-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/payment-service/configmap.yaml
kubectl apply -f k8s/payment-service/secret.yaml
kubectl apply -f k8s/payment-service/deployment.yaml
kubectl apply -f k8s/payment-service/service.yaml