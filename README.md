docker compose --env-file .env.docker up xx-service-db

- Healthy check
docker inspect tuttime-api-gateway --format='{{range .State.Health.Log}}{{.Output}}{{end}}'

docker inspect tuttime-api-gateway --format='{{json .State.Health}}'

docker compose config

docker compose up -d --build menu-service

docker compose up -d --force-recreate booking-service

docker compose up -d --build