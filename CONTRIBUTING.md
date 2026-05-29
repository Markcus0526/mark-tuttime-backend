# Internal Contributing Guide

## Branching Strategy

- `main` – production‑ready code
- `develop` – integration branch
- Feature branches: `feature/description`

## Pull Requests

All changes must go through a pull request with at least one approval from a senior developer.

## Code Style

- Follow the existing formatting (IntelliJ default with Java 21)
- Use Lombok, MapStruct, and Spring idioms consistently
- Write unit tests for new functionality (minimum 80% coverage)

## Commit Messages

Use clear, descriptive messages. Reference internal ticket IDs when applicable.

## Security & Compliance

- Never commit secrets (use environment variables or Kubernetes Secrets)
- Run `mvn dependency:check` before pushing to avoid vulnerable libraries

**This guide is for internal use only.**