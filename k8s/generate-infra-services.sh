#!/bin/bash

# Directory where infra manifests are stored
INFRA_DIR="k8s/infra"

# Create directory if it doesn't exist
mkdir -p "$INFRA_DIR"

# 1. Redis Service
cat > "$INFRA_DIR/redis-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: tuttime
spec:
  selector:
    app: redis
  ports:
  - port: 6379
    targetPort: 6379
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/redis-service.yaml"

# 2. Kafka Service
cat > "$INFRA_DIR/kafka-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: tuttime
spec:
  selector:
    app: kafka
  ports:
  - name: internal
    port: 9093
    targetPort: 9093
  - name: controller
    port: 9094
    targetPort: 9094
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/kafka-service.yaml"

# 3. Zipkin Service
cat > "$INFRA_DIR/zipkin-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: zipkin
  namespace: tuttime
spec:
  selector:
    app: zipkin
  ports:
  - port: 9411
    targetPort: 9411
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/zipkin-service.yaml"

# 4. MongoDB Service
cat > "$INFRA_DIR/mongodb-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: mongodb
  namespace: tuttime
spec:
  selector:
    app: mongodb
  ports:
  - port: 27017
    targetPort: 27017
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/mongodb-service.yaml"

# 5. OpenSearch Service
cat > "$INFRA_DIR/opensearch-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: opensearch
  namespace: tuttime
spec:
  selector:
    app: opensearch
  ports:
  - port: 9200
    targetPort: 9200
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/opensearch-service.yaml"

# 6. Graylog Service (UDP for GELF, HTTP for web)
cat > "$INFRA_DIR/graylog-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: graylog
  namespace: tuttime
spec:
  selector:
    app: graylog
  ports:
  - name: web
    port: 9002
    targetPort: 9002
  - name: gelf-udp
    port: 12201
    targetPort: 12201
    protocol: UDP
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/graylog-service.yaml"

# 7. Kafka UI Service (optional, for internal access)
cat > "$INFRA_DIR/kafka-ui-service.yaml" <<EOF
apiVersion: v1
kind: Service
metadata:
  name: kafka-ui
  namespace: tuttime
spec:
  selector:
    app: kafka-ui
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
EOF
echo "Created $INFRA_DIR/kafka-ui-service.yaml"

echo "All infrastructure service YAML files have been generated in $INFRA_DIR/"