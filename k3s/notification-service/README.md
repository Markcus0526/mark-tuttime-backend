kubectl apply -f k8s/notification-service/db-secret.yaml
kubectl apply -f k8s/notification-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/notification-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/notification-service/configmap.yaml
kubectl apply -f k8s/notification-service/secret.yaml
kubectl apply -f k8s/notification-service/deployment.yaml
kubectl apply -f k8s/notification-service/service.yaml