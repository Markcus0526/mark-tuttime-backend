kubectl apply -f k8s/review-service/db-secret.yaml
kubectl apply -f k8s/review-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/review-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/review-service/configmap.yaml
kubectl apply -f k8s/review-service/secret.yaml
kubectl apply -f k8s/review-service/deployment.yaml
kubectl apply -f k8s/review-service/service.yaml