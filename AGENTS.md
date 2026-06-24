# AGENTS.md

## Purpose
- This file guides agentic coding assistants working in this repo.
- Follow existing patterns; keep changes minimal and consistent.
- When unsure, prefer mirroring nearby code and tests.

## Rule Sources
- No `.cursor/rules/`, `.cursorrules`, or `.github/copilot-instructions.md` found.
- If new rule files appear later, follow them for scoped directories.

## Environment Parity
- The QA environment must be a faithful mirror of the production environment.
- Maintain parity in software versions (e.g., PostgreSQL), resource limits (e.g., JVM memory), and network architecture.
- Differences should be minimal, documented, and restricted only to what is necessary for testing new features (e.g., security keys).

## Communication
- Responda sempre em português brasileiro (pt-br).

## Quick Commands (Gradle Wrapper)
- Use `./gradlew` on macOS/Linux, `gradlew.bat` on Windows.
- Build: `./gradlew build`
- Run app: `./gradlew bootRun`
- Run with profile: `./gradlew bootRun --args='--spring.profiles.active=qa'`
- Clean build: `./gradlew clean build`
- Build image: `./gradlew bootBuildImage`

## Test Commands
- All tests (unit + integration): `./gradlew test`
- Integration tests only (tagged `integration`): `./gradlew integrationTests`
- Single unit test class: `./gradlew test --tests "com.tproject.workshop.validation.DeviceStatusValidatorTest"`
- Single integration test class: `./gradlew integrationTests --tests "com.tproject.workshop.integration.controller.AuthControllerIT"`
- Single test method: `./gradlew test --tests "com.tproject.workshop.validation.DeviceStatusValidatorTest.shouldAcceptNewStatus"`
- Run tests with info: `./gradlew test --info`
- Note: integration tests run the app on port `8081` with `test` profile.

## Docker/Local Dev
- Start DB: `docker-compose -f docker-compose-local.yml up -d`
- Full stack (DB + observability): `docker-compose -f docker-compose-local-full.yml up -d --build`
- Stop and clear: `docker-compose -f docker-compose-local.yml down --volumes`

## CI/CD (GitHub Actions + Tailscale)
- **Transporte SSH:** Tailscale (WireGuard, nós efêmeros via `tailscale/github-action@v4`)
- **Servidor:** `workshop@${{ secrets.TS_TAILSCALE_IP }}` (IP Tailscale, formato `100.x.x.x`)
- **Acesso manual:** Cloudflare tunnel (`cloudflared-ssh.service`) — não usado para CI/CD
- **Secrets Tailscale:** `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `TS_TAILSCALE_IP`
- **Secrets SSH:** `PROD_SSH_KEY`, `PROD_SSH_USER`, `PROD_SSH_HOST` (IP Tailscale)
- **Workflows (padrão `deploy-{servico}-{ambiente}`):**
  - `deploy-api-prod.yml` — API Spring Boot (produção, hosted + Tailscale)
  - `deploy-api-qa.yml` — API Spring Boot (QA, self-hosted)
  - `deploy-gateway-prod.yml` — Caddy Gateway (produção, hosted + Tailscale)
  - `deploy-observability-prod.yml` — Loki + Grafana (produção, self-hosted)
- **Composite actions:** `.github/actions/{shared,api,gateway}/` (shared = `print-banner`; api = `deploy-via-ssh-api`; gateway = `deploy-via-ssh-gateway`)
- **Scripts:** `.github/scripts/{api,gateway}/` (deploy/rollback/verify/cleanup.sh, sem sufixo)
- **Plano:** Tailscale Personal (gratuito, 1.000 min efêmeros/mês)
- **Deploy:** tag `v*` em `release/*` dispara `deploy-{api,gateway,observability}-prod`; QA (`deploy-api-qa`) é manual via `workflow_dispatch`

## Linting/Formatting
- No dedicated linter or formatter configured in `build.gradle`.
- Use IDE auto-formatting; keep consistent with existing spacing.
- Keep line breaks similar to existing code; avoid reformatting unrelated code.

## Project Structure
- `src/main/java/com/tproject/workshop/` contains application code.
- `controller/` defines API interfaces with OpenAPI annotations.
- `controller/impl/` contains `@RestController` implementations.
- `service/` holds business logic and transaction boundaries.
- `repository/` is JPA repositories; `repository/jdbc/` for SQL reads.
- `dto/` holds request/response types (records and classes).
- `model/` holds JPA entities.
- `exception/` defines domain exceptions.
- `errorhandling/` provides global exception mapping.
- `config/` includes security, logging, Jackson, OpenAPI config.
- `src/main/resources/db/query/` contains external SQL for reads.
- `src/main/resources/db/migration/` contains Flyway migrations.
- `src/test/java/` holds unit and integration tests.
- `src/test/resources/jsons/` stores golden JSON snapshots.
- `src/test/resources/test-scripts/` holds SQL scripts for tests.

## Java Style Basics
- Use 4-space indentation; keep braces on same line.
- Files start with `package`, blank line, then `import` blocks.
- Import order: project + third-party, blank line, then `java.*`.
- Static imports come after regular imports with a blank line.
- Wildcard imports are used for DTO/model packages; follow existing file style.
- Prefer `final` fields with constructor injection (`@RequiredArgsConstructor`).
- Avoid field injection unless required by framework.
- Keep methods short and cohesive; extract helpers when logic grows.
- Avoid one-letter variable names unless in small, clear scopes (e.g., lambdas).
- Prefira `Optional` ao invés de ternários para valores nulos quando isso melhorar legibilidade.


## Naming Conventions
- Classes: `PascalCase`; methods/fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- DTO records often end in `Record` (e.g., `DeviceInputDtoRecord`).
- Integration tests end with `IT`; unit tests end with `Test`.
- Controllers use `*Controller` interfaces + `*ControllerImpl` classes.
- Services use `*Service`; repositories use `*Repository`.

## Controller Conventions
- Define endpoints in interface with OpenAPI annotations.
- Implement controller in `controller/impl` with `@RestController`.
- Use `@RequestMapping` at class level for base paths.
- Accept input with `@RequestBody`, `@Valid`, and `@PathVariable`.
- Return DTOs; avoid returning entities directly from controllers.
- Use `ApiGlobalResponses` and `@ApiResponse` for consistent responses.

## Service/Repository Conventions
- Keep business rules in `service/` classes.
- Use `@Transactional` on service methods when mutating state.
- Use `@Transactional(readOnly = true)` for read-only flows.
- Repositories return `Optional<T>` for single-entity fetches.
- For complex reads, use JDBC repositories with external SQL files.
- When adding new queries, place SQL under `src/main/resources/db/query/`.

## DTO and Validation Conventions
- Use Java `record` for simple request/response DTOs.
- Use `jakarta.validation` annotations for request validation.
- Use `@Schema` annotations for API docs examples.
- Prefer `List` and `Map` from `java.util` for collections.

## Error Handling
- Throw domain exceptions (`NotFoundException`, `BadRequestException`, etc.).
- Map new exceptions in `GlobalExceptionHandler` if they reach controllers.
- Use `ResponseError` and `ErrorMetadata.Error` for standardized responses.
- Log exceptions with `LoggerFactory.getLogger(...)` and structured messages.

## Logging
- Use `org.slf4j.Logger`/`LoggerFactory` for logging.
- Avoid logging secrets (tokens, API keys, passwords, PII).
- Favor `warn` for client errors and `error` for server failures.

## Testing Conventions
- Tests use JUnit 5 and RestAssured.
- Integration tests extend `AbstractIntegrationLiveTest`.
- Integration tests are tagged `@Tag("integration")` for Gradle filtering.
- Use `@Sql` scripts to seed/clean test data.
- JSON snapshot assertions live in `src/test/resources/jsons/`.
- When adding new snapshot files, follow existing naming pattern.

## Database and Migrations
- Use Flyway migrations under `src/main/resources/db/migration/postgresql/`.
- Migrations are forward-only; avoid modifying existing versions.
- Keep SQL deterministic and safe for repeated runs when possible.
- For query SQL, prefer file-based SQL over inline strings.

## Security and Configuration
- Do not hardcode secrets or keys in source.
- Keys are expected in `src/main/resources/keys/` or external mounts.
- Respect profile isolation (`test`, `qa`, `prod`).
- JWT and API key logic lives under `config/security` and `service/auth`.

## Suggested Workflow for Changes
- Identify the layer you are changing (controller/service/repo/DTO).
- Update relevant tests or snapshots when behavior changes.
- Run targeted tests before full suites when possible.
- Avoid touching unrelated files (especially migrations and fixtures).

## Common Pitfalls
- Integration tests need the DB running and use port `8081`.
- Some tests create snapshot files if they are missing.
- Missing JSON snapshots will be generated on first run.
- Token/auth flows are sensitive to API key status and device IDs.

## When in Doubt
- Mirror nearby code style and patterns in the same package.
- Prefer minimal, surgical changes.
- Ask for clarification if a change risks touching multiple layers.

## File Hygiene
- Do not add new README or docs unless requested.
- Keep file endings with a trailing newline.
- Avoid reorganizing imports/formatting unless editing logic.

## Contact Points
- If something is unclear, surface it in your response.
- Provide commands used and affected files in your summary.
