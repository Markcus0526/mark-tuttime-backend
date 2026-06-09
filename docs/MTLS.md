# Service-to-service mTLS (Linkerd)

In-cluster service↔service traffic (HTTP/REST via the gateway path, gRPC between services, and
Kafka/Redis/Postgres connections) is encrypted + mutually authenticated by the **Linkerd** service
mesh. Each workload pod runs an injected `linkerd-proxy` sidecar; the mesh issues and rotates
per-pod mTLS certificates automatically.

This complements (does not replace) the existing controls:
- `X-Internal-Gateway-Token` (proves the request came through the gateway) — application-layer.
- NetworkPolicies (`infra/networkpolicy-app-tier.yaml` + per-DB) — L3/L4 segmentation.
- mTLS — encrypts + authenticates the transport between pods. Defense in depth.

## How it's wired
`kustomization.yaml` patches every Deployment/StatefulSet pod template with
`linkerd.io/inject: enabled`. The annotation is **inert until the Linkerd control plane is
installed** — so applying the manifests without Linkerd changes nothing (safe default).

## Install (one-time, per cluster)
```sh
# CLI
curl -sL https://run.linkerd.io/install | sh
export PATH=$PATH:$HOME/.linkerd2/bin

# Pre-flight, then install CRDs + control plane
linkerd check --pre
linkerd install --crds | kubectl apply -f -
linkerd install | kubectl apply -f -
linkerd check

# (optional) viz extension for the dashboard / live mTLS inspection
linkerd viz install | kubectl apply -f -
```

Then (re)apply the app and restart so sidecars are injected:
```sh
kubectl apply -k k3s/
kubectl -n tuttime rollout restart deploy,statefulset
```

## Verify mTLS is active
```sh
# Every meshed pod should show 2/2 (app + linkerd-proxy) containers
kubectl -n tuttime get pods

# Confirm traffic between meshed pods is TLS'd (needs viz)
linkerd viz -n tuttime edges deployment
linkerd viz -n tuttime tap deploy/api-gateway   # rows show tls=true
```

## Caveats
- **StatefulSets with non-HTTP protocols** (Kafka 9093/9094, Redis 6379/26379, Postgres 5432):
  Linkerd meshes TCP transparently, but if a component misbehaves under the proxy you can opaque-port
  or skip it via `config.linkerd.io/opaque-ports` / `config.linkerd.io/skip-outbound-ports`
  annotations on that workload. Validate Kafka inter-broker (9094 controller) + Redis Sentinel
  failover with the mesh enabled.
- **Health probes**: kubelet probes are not meshed; Linkerd allows them by default. Confirm
  readiness still passes after injection.
- **Order of operations**: install Linkerd BEFORE relying on the annotations; otherwise pods run
  un-meshed (no mTLS) but otherwise normal.
- Alternative: Istio (`istio-injection=enabled` on the namespace) if you already standardize on it —
  swap the annotation accordingly. Linkerd chosen here for lighter footprint + automatic mTLS.

⚠️ Not runtime-validated here — apply on a test cluster and run `linkerd check` + the verify steps.
