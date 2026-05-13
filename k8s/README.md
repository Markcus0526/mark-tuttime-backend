# 1. Create namespace
kubectl create namespace tuttime

# 2. Apply infrastructure (databases, redis, kafka, etc.)
kubectl apply -f k8s/infra/ -n tuttime

# 3. Apply API gateway
kubectl apply -f k8s/api-gateway/ -n tuttime

# 4. Apply microservices (one by one or all)
kubectl apply -f k8s/user-service/ -n tuttime
kubectl apply -f k8s/restaurant-service/ -n tuttime
# ... etc.