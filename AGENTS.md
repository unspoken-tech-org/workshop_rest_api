# AGENTS.md

Only what the code does not tell you. Scoped rules live in nested files:
`.github/workflows/AGENTS.md` covers CI/CD (workflows, actions, deploy scripts,
secrets, Environments).

## Communication

- In your summary, list the commands you ran and the files you changed.

## Environment Parity

- QA must be a faithful mirror of production: same software versions (e.g.,
  PostgreSQL), resource limits (e.g., JVM memory) and network architecture.
- Differences must be minimal, documented, and limited to what testing needs
  (e.g., security keys).

## Tests

- All API tests: `./gradlew :apps:api:test :apps:api:integrationTests`
- Integration tests are a separate task (`:apps:api:integrationTests`, tag
  `integration`), extend `AbstractIntegrationLiveTest`, and run the app on port
  `8081` with the `test` profile; the DB must be running.
- A missing JSON snapshot in `apps/api/src/test/resources/jsons/` is written on
  the first run; review a new snapshot before committing it.
- Token/auth flows are sensitive to API key status and device IDs.

## Local Dev

- Start API DB: `docker compose --project-directory . -p workshop_rest_api -f apps/api/compose/local.yml up -d`
- Stop and clear API DB:
  `docker compose --project-directory . -p workshop_rest_api -f apps/api/compose/local.yml down --volumes`
- Local observability stack:
  `docker compose --project-directory . -p workshop-observability-local -f platform/observability/local/compose.yml --profile api up -d --build --wait` (
  see `platform/observability/local/README.md`)

## Code

- Complex reads use JDBC repositories with SQL files under
  `apps/api/src/main/resources/db/query/`, not inline SQL.
- Map every new exception that reaches a controller in `GlobalExceptionHandler`.
- Prefer `Optional` over ternaries for null values when it reads better.
- No formatter is configured: do not reformat or reorder imports in code you
  are not otherwise changing.

## Database

- Flyway migrations live in `apps/api/src/main/resources/db/migration/postgresql/`
  and are forward-only: never modify an existing version.

## Security

- The keys in `apps/api/src/main/resources/keys/` are dev/test keys only; real
  keys come from external mounts.
- Respect profile isolation (`test`, `qa`, `prod`).

## Do Not

- Add new README or docs unless requested.
- Touch unrelated files, especially migrations and test fixtures.
