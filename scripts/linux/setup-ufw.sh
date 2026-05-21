#!/bin/bash
# Save as setup-ufw.sh, run with sudo

# Enable ufw
ufw --force enable

# Default policies
ufw default deny incoming
ufw default allow outgoing

# SSH (change port if needed)
ufw allow 22/tcp comment 'SSH'

# K3s API server
ufw allow 6443/tcp comment 'K3s API'

# Flannel VXLAN (if using Flannel CNI)
ufw allow 8472/udp comment 'Flannel VXLAN'

# NodePort range (expose services externally)
ufw allow 30000:32767/tcp comment 'Kubernetes NodePorts'

# Optional: direct gateway port (if using NodePort, not needed, but for direct mapping)
ufw allow 8080/tcp comment 'API Gateway (if not NodePort)'

# Optional: allow service debug ports (only if exposed)
# ufw allow 5000:5010/tcp comment 'Debug ports'

# Reload to apply
ufw reload

# Show status
ufw status verbose