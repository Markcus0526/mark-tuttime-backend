kubectl create namespace tuttime

# Infrastructure (databases, redis, kafka, etc.)
kubectl apply -f k8s/infra/ -n tuttime

# Wait for critical pods (e.g., redis, kafka) to be Running
kubectl get pods -n tuttime -w

# API Gateway
kubectl apply -f k8s/api-gateway/ -n tuttime

# Microservices (user, restaurant, etc.)
kubectl apply -f k8s/user-service/ -n tuttime
kubectl apply -f k8s/restaurant-service/ -n tuttime
# ... add others

# For development, you can revert to Docker Compose anytime by
kubectl delete -f k8s/
docker compose up

kubectl create namespace tuttime


kubectl apply -f k8s/infra/ -n tuttime


kubectl delete pods --all -n tuttime

kubectl delete pods -n tuttime -l app=api-gateway

kubectl apply -f k8s/infra/kafka-deployment.yaml -n tuttime

kubectl rollout restart deployment/api-gateway -n tuttime

kubectl describe pod -n tuttime

kubectl logs -n tuttime -l app=graylog --tail=100 -f


kubectl describe pod -n tuttime api-gateway-787f4db9d6-fbjxr


kubectl delete pods --all -n tuttime

kubectl delete namespace tuttime