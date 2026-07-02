# Workshop ERP - On-Premise Device Repair Management

![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1.8-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=flat&logo=flyway&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-Observability-F46800?style=flat&logo=grafana&logoColor=white)

> **Enterprise-grade ERP solution engineered for near zero-latency local network environments.**  
> Built for device repair workshops requiring high availability without cloud dependency.

---

## Performance Benchmarks

**Methodology:** Same endpoint, same data returned, different persistence implementation.  
**Test:** 1000 requests, 50 concurrent connections  
**Environment:** Windows 11, Intel i5, 16GB RAM, PostgreSQL 16 in Docker

| Implementation        | p50  | p95   | p99   | Throughput      |
|-----------------------|------|-------|-------|-----------------|
| **JDBC** (native SQL) | 34ms | 65ms  | 80ms  | **1,362 req/s** |
| **JPA** (Hibernate)   | 77ms | 110ms | 127ms | 624 req/s       |

### Key Findings

| Metric                 | JDBC Advantage |
|------------------------|----------------|
| **Latency (p50)**      | 2.3x faster    |
| **Throughput**         | 2.2x higher    |
| **Tail Latency (p99)** | 37% lower      |

> **Why the difference?** The JDBC implementation uses a single SQL query with `LATERAL JOIN` and `json_agg()`
> to fetch customer + phones + devices in one database round-trip. JPA/Hibernate executes multiple queries
> (N+1 pattern) and adds ORM overhead for entity hydration and dirty checking.

<details>
<summary>View raw benchmark data</summary>

```
JDBC (GET /v1/customer/{id})
  Requests/sec: 1362.27
  Latency: 10%=19ms, 50%=34ms, 90%=54ms, 99%=80ms

JPA (GET /v1/customer/{id}/jpa)  
  Requests/sec: 623.88
  Latency: 10%=59ms, 50%=77ms, 90%=101ms, 99%=127ms
```

</details>

---

## The Problem & Solution

### The Challenge

Device repair workshops operate in environments where **internet connectivity is unreliable** or **latency-sensitive
operations cannot tolerate cloud round-trips**. Traditional SaaS solutions fail when:

- Network outages halt business operations
- Cloud latency impacts user experience during peak hours
- Data sovereignty requires on-premise storage
- Operational costs of cloud infrastructure don't scale for small businesses

### The Solution

This project applies **cloud-native engineering practices** to an **on-premise deployment model**:

| Cloud-Native Practice        | On-Premise Benefit                              |
|------------------------------|-------------------------------------------------|
| Docker containerization      | Reproducible deployments, isolated environments |
| CI/CD pipelines              | Automated, reliable updates to local server     |
| Database migrations (Flyway) | Version-controlled schema evolution             |
| Health checks & monitoring   | Self-healing infrastructure                     |
| WAL archiving (pgBackRest)   | Enterprise-grade backup without cloud storage   |

**Result:** A system that runs entirely on local hardware with the reliability and maintainability of modern cloud
applications.

---

## Architecture Design Records

### Key Engineering Decisions

#### ADR-001: Why Monolith?

**Decision:** Single deployable unit (Spring Boot JAR) instead of microservices.

**Context:** On-premise deployment to a single server with limited operational overhead.

**Rationale:**

- **Near zero network latency** between modules — all communication is in-process method calls
- **Simplified deployment** — Two Docker Compose stacks (app + gateway) manage the infrastructure
- **Reduced operational complexity** — no service discovery, no distributed tracing, no inter-service authentication
- **Transactional consistency** — ACID transactions across the entire domain without saga patterns

**Trade-offs accepted:** Horizontal scaling requires vertical scaling of the host machine. This is acceptable for the
target deployment environment (single workshop location).

---

#### ADR-002: Hybrid Persistence Strategy (The "Secret Sauce")

**Decision:** Implement implicit CQRS using JPA for writes and Spring JDBC for reads.

**Context:** Complex reporting queries with multiple JOINs and aggregations were causing Hibernate N+1 problems and
excessive memory usage.

**Implementation:**

```
┌─────────────────────────────────────────────────────────────┐
│                     REPOSITORY LAYER                        │
│                                                             │
│   ┌─────────────────────┐    ┌─────────────────────────┐    │
│   │   JPA (Commands)    │    │    JDBC (Queries)       │    │
│   │                     │    │                         │    │
│   │ • Entity management │    │ • Native SQL            │    │
│   │ • Relationships     │    │ • Transactions          │    │
│   │ • Cascade operations│    │ • Direct DTO mapping    │    │
│   └─────────────────────┘    └─────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

**Code Pattern:**

```java
// Single interface combining both strategies
@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, Integer>, CustomerRepositoryJdbc {

    // JPA: Used for writes and simple lookups
    Optional<Customer> findFirstByCpf(String cpf);
}

// JDBC interface for optimized reads
public interface CustomerRepositoryJdbc {
    Optional<CustomerOutputDto> findCustomerById(int id);

    Page<CustomerListOutputDto> findCustomersByFilter(CustomerFilterDto filters);
}
```

**SQL Externalization:**

Complex queries are stored in external `.sql` files for maintainability:

```
src/main/resources/db/query/
├── customer/
│   ├── getCustomer.sql          # Single customer with phones (LATERAL JOIN)
│   └── listCustomers.sql        # Paginated list with filters
└── device/
    ├── getDevice.sql            # Device with all relations
    └── deviceStatistics.sql     # Aggregated metrics
```

**Advanced PostgreSQL Features Used:**

- `LATERAL JOIN` for correlated subqueries without N+1
- `json_agg()` / `jsonb_build_object()` for nested object construction in SQL
- `unaccent()` for accent-insensitive search
- Array parameters (`ANY(:STATUS::text[])`) for multi-value filters

**Result:**

- Read operations return DTOs directly — no entity-to-DTO conversion layer
- Complex reports execute in single database round-trip
- Write operations maintain full Hibernate benefits (dirty checking, cascades, audit)

---

#### ADR-003: Event-Driven Internals

**Decision:** Use Spring Application Events for cross-cutting concerns.

**Context:** Operations like "update last viewed timestamp" should not block the main response.

**Implementation:**

```java
// Publishing event without blocking response
@Transactional(readOnly = true)
public DeviceOutputDto findDeviceById(int deviceId) {
    DeviceOutputDto device = deviceRepository.findByDeviceId(deviceId);
    eventPublisher.publishEvent(new DeviceViewedEvent(this, deviceId));
    return device;  // Returns immediately
}

// Async listener handles secondary operation
@EventListener
@Transactional
@Async
public void onDeviceViewed(DeviceViewedEvent event) {
    Device device = deviceRepository.findById(event.getDeviceId())
            .orElseThrow(() -> new NotFoundException("Aparelho não encontrado"));

    device.setLastViewedAt(Timestamp.from(Instant.now()));
    deviceRepository.save(device);
}
```

**Benefits:**

- Main thread returns response immediately
- Secondary operations execute in background
- Domain events provide audit trail
- Easy to add new listeners without modifying publishers

---

## Security & High Availability

The system implements a **Zero Trust** hybrid security model designed to function seamlessly both over the internet (
WAN) and within the local network (LAN) without internet access.

### Hybrid Access Strategy

| Context            | Technology        | Mechanism           | Benefit                                      |
|--------------------|-------------------|---------------------|----------------------------------------------|
| **WAN (Internet)** | Cloudflare Tunnel | Zero Trust Edge     | DDoS protection, WAF, no open firewall ports |
| **LAN (Offline)**  | Split-Brain DNS   | Caddy (Gateway)     | High availability even during ISP outages    |

**Key Feature: Split-Brain DNS**

- **Public DNS:** Resolves `api.eletroluk.com` to Cloudflare edge.
- **Local Router DNS:** Resolves `api.eletroluk.com` to the local server IP (e.g., 192.168.x.x).
- **Caddy Proxy (Gateway):** Handles automatic SSL certificates (via DNS-01 challenge) so the same domain works everywhere with
  valid encryption, removing the need for SSL pinning on clients.

### Authentication & Authorization

1. **Hybrid Auth Flow:**
    - **API Keys:** Long-lived keys used for device onboarding (Mobile/CLI).
    - **JWT (RS256):** Short-lived (15 min) access tokens for session management.
    - **Refresh Tokens:** Opaque, revocable tokens (7 days) linked to the API Key.
2. **RBAC (Role-Based Access Control):**
    - `ADMIN`: Full system management, including key generation.
    - `SERVICE`: Standard business operations.
3. **Key Management:** Cryptographic keys are injected via CI/CD secrets (Production) or mounted locally (Dev), never
   hardcoded.

---

## Environment Strategy

We strictly segregate **QA** and **Production** environments to allow infrastructure experimentation without risking
production data stability.

| Layer        | Production (`api.eletroluk.com`) | QA (`api-qa.eletroluk.com`)        |
|--------------|----------------------------------|------------------------------------|
| **Network**  | Dedicated Cloudflare Tunnel      | Separate Tunnel + Split DNS        |
| **Database** | `workshop_db`                    | `workshop_db_qa` (Isolated Volume) |
| **Keys**     | Production RSA Keys              | QA RSA Keys (Separate signature)   |
| **Safety**   | **Prod Tokens Invalid in QA**    | **QA Tokens Invalid in Prod**      |

> **Fail-Safe Design:** The application profile (`-Dspring.profiles.active=qa`) enforces strict connection isolation. A
> QA build is physically incapable of connecting to the Production database or accepting Production tokens.

---

## Tech Stack

| Layer             | Technology      | Version     | Purpose                                         |
|-------------------|-----------------|-------------|-------------------------------------------------|
| **Runtime**       | Java            | 21 LTS      | Modern language features, virtual threads ready |
| **Framework**     | Spring Boot     | 3.1.8       | Production-ready application framework          |
| **Persistence**   | Spring Data JPA | -           | ORM for write operations                        |
| **Persistence**   | Spring JDBC     | -           | Native SQL for read operations                  |
| **Database**      | PostgreSQL      | 16          | Advanced SQL features (LATERAL, JSON)           |
| **Migrations**    | Flyway          | 11.11.0     | Version-controlled schema management            |
| **Backup**        | pgBackRest      | -           | WAL archiving, point-in-time recovery           |
| **Build**         | Gradle          | 8.4         | Dependency management, build automation         |
| **Container**     | Docker          | Multi-stage | Optimized production images                     |
| **CI/CD**         | GitHub Actions  | -           | Automated deployment pipeline                   |
| **Observability** | Grafana + Loki  | 10.2 / 2.9  | Log aggregation and dashboards                  |

---

## Observability

The application includes a complete observability stack for log aggregation and visualization.

### Architecture

```
Application → Promtail → Loki → Grafana
   (JSON logs)   (collector)  (storage)  (dashboards)
```

### Features

| Feature                    | Description                                              |
|----------------------------|----------------------------------------------------------|
| **Structured Logs**        | JSON format with correlation ID, HTTP metadata, duration |
| **Request Tracing**        | `X-Request-Id` header propagated through all logs        |
| **Sensitive Data Masking** | Automatic masking of passwords, tokens, CPF, etc.        |
| **Error Correlation**      | Full request body logged on 4xx/5xx errors               |
| **7-Day Retention**        | Configurable log retention in Loki                       |

### Quick Start (Local with Observability)

```bash
# Start full stack: database + app + observability
docker-compose -f docker-compose-local-full.yml up -d --build

# Access Grafana
open http://localhost:3000  # admin/admin
```

> **Note:** The application must run as a Docker container for Promtail to capture logs.

### Service Endpoints (with Observability)

| Service | URL                     | Purpose                        |
|---------|-------------------------|--------------------------------|
| Grafana | `http://localhost:3000` | Dashboards and log exploration |
| Loki    | `http://localhost:3100` | Log aggregation API            |

### Useful LogQL Queries

```logql
# All API logs
{container_name="workshop-api"}

# Only errors
{container_name="workshop-api"} | json | level="ERROR"

# Slow requests (>1s)
{container_name="workshop-api"} | json | durationMs > 1000

# Trace specific request
{container_name="workshop-api"} | json | requestId="abc-123"
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development)
- Git

### One Command Start

```bash
# Clone the repository
git clone https://github.com/your-username/workshop_rest_api.git
cd workshop_rest_api

# Start PostgreSQL database
docker-compose -f docker-compose-local.yml up -d

# Run the application
./gradlew bootRun
```

### Service Endpoints

| Service      | URL                                     | Purpose            |
|--------------|-----------------------------------------|--------------------|
| API          | `http://localhost:8080`                 | REST API           |
| Health Check | `http://localhost:8080/actuator/health` | Application health |
| Database     | `localhost:5445`                        | PostgreSQL (dev)   |

### Stopping the Environment

```bash
# Stop containers and remove volumes (clean slate)
docker-compose -f docker-compose-local.yml down --volumes
```

### Running Tests

```bash
# All tests (unit + integration)
./gradlew test

# Integration tests only (~200 scenarios)
./gradlew integrationTests
```

---

## CI/CD Pipeline

### Deployment Architecture

The project uses a robust **Self-Hosted** deployment pipeline designed for reliability and zero-downtime updates.

```
┌─────────────┐
│  Git Tag    │
│  (v1.0.0)   │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                             GitHub Actions                               │
│                          (Self-Hosted Runner)                            │
│                                                                          │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐            │
│  │  BUILD   ├───►│  DEPLOY  ├───►│  VERIFY  ├───►│ CLEANUP  │            │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘    └──────────┘            │
│       │               │               │                                  │
│   1. Backup       4. Compose Up   6. Obs. Check                          │
│   2. Inject Keys  5. Health Check 7. pgBackRest                          │
│   3. Build Img        │                                                  │
│                       ▼                                                  │
│                 (Rollback on Fail)                                       │
└──────────────────────────────────────────────────────────────────────────┘
```

### Pipeline Features

| Feature           | Implementation                                                 |
|-------------------|----------------------------------------------------------------|
| **Trigger**       | Push to tag `v*` (e.g., `v1.2.0`) or Manual Dispatch           |
| **Runner**        | Self-hosted on target server                                   |
| **Secrets**       | Injected via GitHub Secrets → `.env` & `config/keys/`          |
| **Backup**        | Current running image is tagged `:backup` before build         |
| **Rollback**      | **Automated** restore of `:backup` image if Health Check fails |
| **Verification**  | Validates Loki, Grafana, Promtail, and PostgreSQL status       |
| **Observability** | Deploys with full logging stack enabled                        |

### Health & Safety Checks

The pipeline implements a "Trust but Verify" approach:

1. **API Health:** Polls `/actuator/health` for 90 attempts (approx 7.5 min).
2. **Observability:** Ensures logs are being collected and Grafana is accessible.
3. **Backup Stanza:** Verifies pgBackRest configuration is valid.
4. **Rollback:** If the API fails to start, the previous stable image is immediately restored to minimize downtime.

### Environments

- **Production:** Deploys from `main` (via tags) to `api.eletroluk.com`.
- **QA:** Manual deployment via `workflow_dispatch` to `api-qa.eletroluk.com` (Isolated DB and Keys).

---

## Database Migrations

Schema changes are managed through Flyway migrations:

```
src/main/resources/db/migration/postgresql/
├── V1__10_08_2025_initial.sql
├── V2__31_08_2025_add_many_to_many_customer_phones.sql
├── V3__03_09_2025_sanatize_data.sql
├── V4__02_12_25_fix_color_duplicates_before_unique_constraints.sql
├── V5__02_12_25_add_unique_constraints.sql
├── V6__03_01_2026_create_api_keys_table.sql
└── V7__03_01_2026_create_refresh_tokens_table.sql
```

**Migration Best Practices Applied:**

- Forward-only migrations (no rollbacks in production)
- Idempotent operations where possible
- Data migrations separated from schema migrations
- Baseline on migrate for existing databases

---

## Project Structure

```
workshop_rest_api/
├── src/main/java/com/tproject/workshop/
│   ├── config/              # Configuration classes (Security, OpenAPI, Jackson)
│   ├── controller/          # REST endpoints (Interface + Impl pattern)
│   ├── service/             # Business logic, transaction boundaries
│   ├── repository/          # JPA repositories
│   │   └── jdbc/            # JDBC repositories for optimized reads
│   ├── model/               # JPA entities
│   ├── dto/                 # Request/Response objects
│   ├── events/              # Domain events
│   ├── exception/           # Custom exceptions
│   ├── errorhandling/       # Global exception handler
│   └── validation/          # Custom validators
├── src/main/resources/
│   ├── db/migration/        # Flyway migrations
│   ├── db/query/            # External SQL files
│   └── keys/                # Cryptographic keys
├── docker-compose-local.yml               # Development: database only
├── docker-compose-local-full.yml          # Development: full stack with observability
├── docker-compose-production.yml          # Production: Application stack
├── docker-compose-qa.yml                  # QA: Isolated environment
├── docker-compose-gateway.yml             # Gateway: Ingress + Tunnels
├── infra/                                 # Infrastructure configurations
│   ├── act/                 # GitHub Actions runner utils
│   ├── caddy/               # Reverse proxy config
│   │   └── Caddyfile-gateway
│   ├── loki-config.yaml
│   └── promtail-config.yaml
├── Dockerfile                             # Multi-stage build
├── Dockerfile.pgbackrest                  # Backup sidecar container
└── .github/
    ├── actions/
    │   ├── shared/          # Shared composite actions (api+gateway)
    │   ├── api/             # API-specific composite actions
    │   └── gateway/         # Gateway-specific composite actions
    ├── scripts/
    │   ├── api/             # API deploy/rollback/verify/cleanup scripts
    │   └── gateway/         # Gateway deploy/rollback/verify/cleanup scripts
    └── workflows/
        ├── deploy-api-prod.yml            # API Spring Boot (production)
        ├── deploy-api-qa.yml              # API Spring Boot (QA)
        ├── deploy-gateway-prod.yml        # Caddy Gateway (production)
        └── deploy-observability-prod.yml  # Loki + Grafana (production)
```

---

## License

This project is proprietary software developed for internal use.

---

<p align="center">
  <sub>Engineered with attention to performance, maintainability, and operational excellence.</sub>
</p>