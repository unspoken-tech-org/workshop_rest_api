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
- Build API: `./gradlew :apps:api:build`
- Run API: `./gradlew :apps:api:bootRun`
- Run with profile: `./gradlew :apps:api:bootRun --args='--spring.profiles.active=qa'`
- Clean API build: `./gradlew :apps:api:clean :apps:api:build`
- Build API image: `./gradlew :apps:api:bootBuildImage`

## Test Commands
- All API tests (unit + integration): `./gradlew :apps:api:test :apps:api:integrationTests`
- Integration tests only (tagged `integration`): `./gradlew :apps:api:integrationTests`
- Single unit test class: `./gradlew :apps:api:test --tests "com.tproject.workshop.validation.DeviceStatusValidatorTest"`
- Single integration test class: `./gradlew :apps:api:integrationTests --tests "com.tproject.workshop.integration.controller.AuthControllerIT"`
- Single test method: `./gradlew :apps:api:test --tests "com.tproject.workshop.validation.DeviceStatusValidatorTest.shouldPassValidation_whenDeviceStatusIsValid"`
- Run tests with info: `./gradlew :apps:api:test --info`
- Note: integration tests run the app on port `8081` with `test` profile.

## Docker/Local Dev
- Start API DB: `docker compose --project-directory . -p workshop_rest_api -f apps/api/compose/local.yml up -d`
- Full stack (DB + observability): `docker compose --project-directory . -p workshop_rest_api -f environments/local/compose.yml up -d --build`
- Stop and clear API DB: `docker compose --project-directory . -p workshop_rest_api -f apps/api/compose/local.yml down --volumes`

## CI/CD (GitHub Actions + Tailscale)
- **Transporte SSH:** Tailscale (WireGuard, nós efêmeros via `tailscale/github-action` v4.1.3, fixada por SHA); os workflows não usam chave SSH privada, o acesso é por Tailscale SSH com `tag:ci`
- **Servidor (PRD):** `${{ secrets.PROD_SSH_USER }}@${{ secrets.TS_TAILSCALE_IP }}` (IP Tailscale, formato `100.x.x.x`); **Servidor (QA):** `${{ secrets.QA_SSH_USER }}@${{ secrets.QA_TS_TAILSCALE_IP }}`
- **Acesso manual:** Tailscale SSH (o Tunnel Cloudflare `ssh.eletroluk.com` foi aposentado)
- **Secrets Tailscale:** PRD `PRD_TS_OAUTH_CLIENT_ID`, `PRD_TS_OAUTH_SECRET`, `TS_TAILSCALE_IP`; QA `QA_TS_OAUTH_CLIENT_ID`, `QA_TS_OAUTH_SECRET`, `QA_TS_TAILSCALE_IP`
- **Secrets SSH:** PRD `PROD_SSH_USER`, `PROD_SSH_KNOWN_HOSTS`; QA `QA_SSH_USER`, `QA_SSH_KNOWN_HOSTS`
- **Workflows (`deploy-{servico}-{ambiente}` e auxiliares):**
  - `ci.yml` — build, testes e scan Trivy (push e PR)
  - `deploy-api-prod.yml` — API Spring Boot (produção, hosted + Tailscale)
  - `deploy-api-qa.yml` — API Spring Boot (QA, hosted + Tailscale)
  - `deploy-gateway-prod.yml` — Caddy Gateway (produção, hosted + Tailscale)
  - `deploy-gateway-qa.yml` — Caddy Gateway (QA, hosted + Tailscale)
  - `deploy-observability-control-plane-qa.yml` — Observability control plane (QA, VM via Tailscale)
  - `deploy-observability-edge-prod.yml` — Observability edge collector (produção, VM via Tailscale)
  - `deploy-observability-prod.yml` — arquivado, não executa deploy
  - `terraform-cloudflare-observability.yml` — Terraform do Cloudflare (plan em PR/push; apply manual com plano revisado)
  - `terraform-state-drill-observability.yml` — drill de lock e restore do state Terraform (manual)
- **Composite actions (`.github/actions/`):**
  - `shared/` — `print-banner` (banner de deploy), `setup-ssh-known-hosts` (known_hosts por secret ou ssh-keyscan no túnel)
  - `api/deploy-via-ssh-api` e `api-qa/deploy-via-ssh-api-qa` — deploy da API (PRD e QA)
  - `gateway/deploy-via-ssh-gateway` e `gateway-qa/deploy-via-ssh-gateway-qa` — deploy do gateway (PRD e QA)
  - `observability-control-plane/` — `connect`, `deploy-via-ssh-observability-control-plane`, `run-remote-script` (QA)
  - `observability-edge/deploy-via-ssh-observability-edge` — edge collector (PRD)
  - `observability-terraform/init-state` — init do Terraform com backend R2
  - `observability/deploy-via-ssh-observability` — arquivado, recusa execução
- **Scripts (`.github/scripts/<grupo>/`, sem sufixo):** `api/`, `api-qa/`, `gateway/`, `gateway-qa/` com deploy/rollback/verify/cleanup.sh; `observability-control-plane/` e `observability-edge/` idem, mais `common.sh` (carregado por source); `observability/` arquivado (stubs sem operação remota)
- **Plano:** Tailscale Personal (gratuito, 1.000 min efêmeros/mês)
- **Deploy:** tag semver (`vX.Y.Z` e `vX.Y.Z-*`) dispara apenas `deploy-api-prod`; os demais deploys são manuais via `workflow_dispatch`, e os de PRD `deploy-gateway-prod` e `deploy-observability-edge-prod` exigem `confirm` = `DEPLOY`

## Linting/Formatting
- No dedicated linter or formatter configured in `build.gradle`.
- Use IDE auto-formatting; keep consistent with existing spacing.
- Keep line breaks similar to existing code; avoid reformatting unrelated code.

## Project Structure
- `apps/api/src/main/java/com/tproject/workshop/` contains application code.
- `controller/` defines API interfaces with OpenAPI annotations.
- `controller/impl/` contains `@RestController` implementations.
- `service/` holds business logic and transaction boundaries.
- `repository/` is JPA repositories; `repository/jdbc/` for SQL reads.
- `dto/` holds request/response types (records and classes).
- `model/` holds JPA entities.
- `exception/` defines domain exceptions.
- `errorhandling/` provides global exception mapping.
- `config/` includes security, logging, Jackson, OpenAPI config.
- `apps/api/src/main/resources/db/query/` contains external SQL for reads.
- `apps/api/src/main/resources/db/migration/` contains Flyway migrations.
- `apps/api/src/test/java/` holds unit and integration tests.
- `apps/api/src/test/resources/jsons/` stores golden JSON snapshots.
- `apps/api/src/test/resources/test-scripts/` holds SQL scripts for tests.

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
- When adding new queries, place SQL under `apps/api/src/main/resources/db/query/`.

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
- JSON snapshot assertions live in `apps/api/src/test/resources/jsons/`.
- When adding new snapshot files, follow existing naming pattern.

## Database and Migrations
- Use Flyway migrations under `apps/api/src/main/resources/db/migration/postgresql/`.
- Migrations are forward-only; avoid modifying existing versions.
- Keep SQL deterministic and safe for repeated runs when possible.
- For query SQL, prefer file-based SQL over inline strings.

## Security and Configuration
- Do not hardcode secrets or keys in source.
- Keys are expected in `apps/api/src/main/resources/keys/` or external mounts.
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
