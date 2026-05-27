#!/bin/bash
# reinstall-k3s.sh - Complete removal and fresh install of K3s with flannel

set -e

echo "========================================="
echo "⚠️  This will COMPLETELY REMOVE K3s and all data."
echo "========================================="
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operation cancelled."
    exit 1
fi

# 1. Stop and uninstall K3s
echo "Stopping K3s..."
sudo systemctl stop k3s 2>/dev/null || true
if [ -f /usr/local/bin/k3s-uninstall.sh ]; then
    sudo /usr/local/bin/k3s-uninstall.sh
else
    echo "Uninstall script not found – cleaning manually."
    sudo systemctl disable k3s 2>/dev/null || true
    sudo rm -f /etc/systemd/system/k3s.service
fi

# 2. Remove all K3s data
echo "Removing data directories..."
sudo rm -rf /var/lib/rancher /etc/rancher ~/.kube /var/lib/kubelet

# 3. Flush iptables
echo "Flushing iptables..."
sudo iptables -F && sudo iptables -t nat -F && sudo iptables -t mangle -F && sudo iptables -X

# 4. Install K3s (Uses Flannel VXLAN backend by default)
echo "Installing fresh K3s..."
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644" sh -

# 5. Wait for API server
echo "Waiting for API server..."
for i in {1..90}; do
    if sudo k3s kubectl get nodes &>/dev/null; then
        echo "API server reachable."
        break
    fi
    echo -n "."
    sleep 2
done
echo ""

# 6. Wait for node to become Ready (Confirms CNI/Flannel is active)
echo "Waiting for node to be Ready..."
for i in {1..90}; do
    # Using wildcard [*] to safely handle empty node arrays during early initialization
    READY=$(sudo k3s kubectl get nodes -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
    if [[ "$READY" == *"True"* ]]; then
        echo "Node is Ready."
        break
    fi
    echo -n "."
    sleep 2
done
echo ""

# 7. Configure kubectl for current user
echo "Setting up kubectl for user..."
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config
export KUBECONFIG=~/.kube/config
grep -qxF 'export KUBECONFIG=~/.kube/config' ~/.bashrc || echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc

# 8. Test kubectl
echo "Testing kubectl..."
kubectl get nodes
echo "Showing system startup pods:"
kubectl get pods -n kube-system

echo "✅ K3s reinstallation completed."
