# Security Policy

## Supported versions

Security fixes are applied to the latest code on the `main` branch.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report vulnerabilities privately through GitHub:
[**Report a vulnerability**](https://github.com/Markcus0526/mark-tuttime-backend/security/advisories/new)
(Security tab → *Report a vulnerability*).

Please include:

- the affected service or component,
- steps to reproduce or a proof of concept,
- the potential impact.

You can expect an acknowledgement within a few days. Once the issue is confirmed and fixed, we will publish
an advisory and credit you, unless you prefer to stay anonymous.

## Deployment hardening

Mark Tuttime ships with development defaults. Before running it in production:

- Replace every `dev-only-…` value and default password with a long random secret (`openssl rand -base64 48`).
- Keep secrets out of git: use `.env` files locally and Kubernetes Secrets (or an external secret manager) in clusters.
- Disable Swagger (`SPRINGDOC_ENABLED=false`) and test endpoints (`APP_TEST_ENDPOINTS_ENABLED=false`).
- Enable TLS at the edge (cert-manager) and, ideally, service-to-service mTLS ([docs/MTLS.md](docs/MTLS.md)).
