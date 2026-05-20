kubectl apply -f k8s/booking-service/db-secret.yaml
kubectl apply -f k8s/booking-service/db-init-configmap.yaml   # if needed
kubectl apply -f k8s/booking-service/db-deployment.yaml
# Wait for database to be ready, then:
kubectl apply -f k8s/booking-service/configmap.yaml
kubectl apply -f k8s/booking-service/secret.yaml
kubectl apply -f k8s/booking-service/deployment.yaml
kubectl apply -f k8s/booking-service/service.yaml