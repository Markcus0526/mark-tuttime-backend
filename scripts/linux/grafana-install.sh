#!/bin/bash
# install-grafana.sh
# Installs Grafana (via kube-prometheus-stack) on K3s if not already present.
# Configures NodePort access and prints login credentials.

set -e

# --- Helper functions ---
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

confirm() {
    read -p "$1 (y/N): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

# --- Install Helm if missing ---
if ! command_exists helm; then
    echo "Helm not found. Installing Helm..."
    curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
    chmod 700 get_helm.sh
    ./get_helm.sh
    rm get_helm.sh
else
    echo "Helm is already installed."
fi

# --- Add Prometheus community repo ---
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# --- Check if Grafana (kube-prometheus-stack) is already installed ---
RELEASE_NAME="monitoring"
NAMESPACE="monitoring"
if helm list -n "$NAMESPACE" | grep -q "$RELEASE_NAME"; then
    echo "Grafana (kube-prometheus-stack) is already installed in namespace $NAMESPACE."
    echo "Skipping installation."
else
    echo "Installing Grafana via kube-prometheus-stack..."
    # Create namespace if not exists
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    # Install with NodePort for Grafana
    helm install "$RELEASE_NAME" prometheus-community/kube-prometheus-stack \
        --namespace "$NAMESPACE" \
        --set grafana.service.type=NodePort \
        --set grafana.service.nodePort=32000 \
        --set prometheus.service.type=NodePort \
        --set prometheus.service.nodePort=32001
    echo "Installation completed."
fi

# --- Ensure Grafana is ready ---
echo "Waiting for Grafana pod to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana -n "$NAMESPACE" --timeout=180s

# --- Retrieve admin password ---
GRAFANA_SECRET=$(kubectl get secret -n "$NAMESPACE" -l app.kubernetes.io/name=grafana -o jsonpath="{.items[0].metadata.name}")
if [ -z "$GRAFANA_SECRET" ]; then
    # Fallback to the default secret name
    GRAFANA_SECRET="***REMOVED***"
fi
PASSWORD=$(kubectl get secret -n "$NAMESPACE" "$GRAFANA_SECRET" -o jsonpath="{.data.admin-password}" | base64 --decode)

# --- Get Node IP and NodePort ---
NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
NODE_PORT=$(kubectl get svc -n "$NAMESPACE" monitoring-grafana -o jsonpath='{.spec.ports[?(@.name=="http-web")].nodePort}')

if [ -z "$NODE_PORT" ]; then
    # Fallback to default 32000 if service not found
    NODE_PORT="32000"
fi

# --- Output access information ---
echo ""
echo "========================================="
echo "✅ Grafana is ready!"
echo "Access URL: http://$NODE_IP:$NODE_PORT"
echo "Username: admin"
echo "Password: $PASSWORD"
echo ""
echo "To change the password, log in and go to Profile → Change Password."
echo "========================================="