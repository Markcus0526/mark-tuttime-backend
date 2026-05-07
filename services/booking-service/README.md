# Check Redis is reachable from the container
docker exec tuttime-booking-service bash -c "timeout 2 cat < /dev/null > /dev/tcp/redis/6379 && echo 'Redis reachable' || echo 'Redis not reachable'"

docker exec tuttime-redis redis-cli -h redis -p 6379 ping

docker logs tuttime-booking-service 2>&1 | grep -i redisson


# Check health endpoint includes Redis status
curl http://localhost:28004/actuator/health/readiness