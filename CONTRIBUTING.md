# Contributing to TutTime Backend

Thanks for your interest in TutTime! Bug reports, features, documentation fixes and ideas are all welcome.

## Ways to contribute

- **Report a bug** — open an [issue](https://github.com/Markcus0526/tuttime-backend/issues/new/choose) with steps to reproduce.
- **Suggest a feature** — open an issue and describe the problem it solves.
- **Improve the docs** — typos, unclear steps and missing examples are great first contributions.
- **Send code** — look for issues labelled `good first issue` or `help wanted`.

For larger changes, please open an issue first so we can agree on the approach before you spend time on it.

## Development setup

1. Fork the repository and clone your fork.
2. Install **Java 21**, **Maven 3.9+** and **Docker** with Compose v2.
3. Create your local config files from the templates (see the [Quick start](README.md#-quick-start-docker)).
4. Build and run the tests:

   ```bash
   mvn clean install
   ```

5. Start the stack with `./scripts/linux/docker-start-all.sh`.

## Making a change

1. Create a branch from `main`: `feature/short-description` or `fix/short-description`.
2. Keep the change focused — one feature or fix per pull request.
3. Follow the existing code style (IntelliJ defaults, Java 21, Lombok, MapStruct, Spring idioms).
4. Add or update unit tests (JUnit 5 + Mockito) for new behaviour.
5. If you change a database schema, add a new Flyway migration in `db-migrations/<service>/` — never edit an existing one.
6. If you change a Kafka event or gRPC contract in `common-libs/common-event`, keep it backward compatible or update every consumer.
7. Update the README or docs when behaviour or configuration changes.

## Commit messages

Write clear, imperative messages, for example:

```text
Add slot capacity check to booking-service
Fix refund status not updated after YooKassa webhook
```

## Pull requests

- Make sure `mvn clean test` passes.
- Fill in the pull request template and link the related issue (`Closes #123`).
- Be ready to discuss and adjust the change — reviews are a conversation.

## Security

- **Never commit secrets.** Use `.env` / `.env.docker` files and Kubernetes Secrets (all git-ignored).
- Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md), not in a public issue.

## Code of Conduct

By participating you agree to follow our [Code of Conduct](CODE_OF_CONDUCT.md).
