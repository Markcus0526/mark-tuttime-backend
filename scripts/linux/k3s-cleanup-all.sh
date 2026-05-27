#!/bin/bash
# cleanup-tuttime.sh
# Removes all TutTime resources from K3s (deployments, services, PVCs, etc.)
# and optionally deletes the imported Docker images from containerd.

set -e

AUTO_YES=false
while getopts "y" opt; do
    case $opt in
        y) AUTO_YES=true ;;
        *) echo "Usage: $0 [-y]"; exit 1 ;;
    esac
done

confirm() {
    if [ "$AUTO_YES" = true ]; then
        return 0
    fi
    read -p "$1 (y/N): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

echo "========================================="
echo "TutTime Cleanup Script"
echo "========================================="

# Check if namespace exists
if ! kubectl get namespace tuttime &>/dev/null; then
    echo "Namespace 'tuttime' does not exist. Nothing to clean."
    exit 0
fi

# ----------------------------------------------------------------------
# 1. Optionally delete all resources in the namespace (without deleting namespace)
# ----------------------------------------------------------------------
echo "Step 1: Deleting all resources in namespace 'tuttime'"
if confirm "Delete all deployments, services, configmaps, secrets, PVCs, etc. in tuttime?"; then
    # Delete all resources except the namespace itself
    kubectl delete all --all -n tuttime --ignore-not-found=true
    kubectl delete configmap --all -n tuttime --ignore-not-found=true
    kubectl delete secret --all -n tuttime --ignore-not-found=true
    kubectl delete pvc --all -n tuttime --ignore-not-found=true
    kubectl delete ingress --all -n tuttime --ignore-not-found=true
    echo "Resources deleted."
else
    echo "Skipping resource deletion."
fi

# ----------------------------------------------------------------------
# 2. Optionally delete the namespace itself
# ----------------------------------------------------------------------
if confirm "Delete the entire 'tuttime' namespace (this will remove any remaining resources)?"; then
    kubectl delete namespace tuttime --timeout=60s || {
        echo "Normal deletion failed, forcing..."
        kubectl delete namespace tuttime --force --grace-period=0 &>/dev/null
    }
    echo "Namespace deleted."
else
    echo "Keeping namespace."
fi

# ----------------------------------------------------------------------
# 3. Optionally remove imported tuttime images from containerd
# ----------------------------------------------------------------------
if confirm "Remove all 'tuttime/*' images from K3s containerd?"; then
    echo "Listing current tuttime images in containerd..."
    IMAGES=$(sudo k3s ctr image ls | grep "tuttime/" | awk '{print $1}')
    if [ -z "$IMAGES" ]; then
        echo "No tuttime images found in containerd."
    else
        echo "Found images:"
        echo "$IMAGES"
        for img in $IMAGES; do
            echo "Removing $img ..."
            sudo k3s ctr image rm "$img" || echo "Failed to remove $img (may still be in use?)"
        done
        echo "Image removal complete."
    fi
else
    echo "Skipping image removal."
fi

echo ""
echo "✅ Cleanup completed."
echo "If you also want to remove Docker images from your local Docker daemon, run: docker rmi \$(docker images -q --filter 'reference=tuttime/*')"