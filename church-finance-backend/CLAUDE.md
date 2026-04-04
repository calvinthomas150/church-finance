# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew build                        # Build the project
./gradlew test                         # Run all tests
./gradlew test --tests='*ClassName*'   # Run a specific test class
./gradlew bootRun                      # Run the application
./gradlew ktlintCheck                  # Check Kotlin code style
./gradlew ktlintFormat                 # Auto-format Kotlin code
./gradlew addKtlintCheckGitPreCommitHook  # Install KtLint pre-commit hook
./gradlew dependencyUpdates               # Check for newer dependency versions
```

Tests use Testcontainers with PostgreSQL — Docker must be running.

## Environment Setup

Required environment variables (or use `application-local.yaml`):
- `DATABASE_URL` — PostgreSQL JDBC URL (default: `jdbc:postgresql://localhost:5432/church_finance`)
- `DATABASE_USERNAME` — database user
- `DATABASE_PASSWORD` — database password

Copy `src/main/resources/application-local.yaml.example` to `application-local.yaml` and fill in local values, then run with `--spring.profiles.active=local`.

## Architecture

**Spring Boot 4.0.5 / Kotlin 2.3.20 / Java 21 / PostgreSQL / Flyway**

The project follows Domain-Driven Design organized by aggregate. Each aggregate lives under `src/main/kotlin/com/calvintech/churchfinance/` with a consistent layered structure:

```
<aggregate>/
├── api/           # REST controllers
├── domain/        # Entities, value objects, business rules
├── persistence/   # JPA repositories
└── service/       # Application services
```

**Aggregates** (directory names): `account`, `administration`, `budget`, `donor`, `financialtransaction`, `fund`, `giftaid`, `reconciliation`, `report`, `user` (note: `shared` is not an aggregate)

Cross-aggregate shared types (e.g. `FinancialTransactionType`, `Money`) live in `shared/domain/`.

The domain model was derived from event storming — see `src/documents/domain-model.md` for the full aggregate and event definitions.

## Persistence Conventions

Each persisted aggregate has a consistent structure in its `persistence/` package:
- `*JpaEntity` — JPA entity class, uses `UUID` for IDs and `@Version` for optimistic locking
- `*Mapper` — `@Component` that converts between domain entities (`Ulid` IDs) and JPA entities (`UUID` IDs) via `toDomain()`/`toJpaEntity()`
- `*Repository` — Spring Data `JpaRepository` interface

Domain entities carry a `version: Long` field (default `0`) for optimistic locking — always map it through the persistence layer.

Flyway migration naming: `V{n}__{description}.sql` (double underscore). Schema: `church_finance`.

## Key Design Decisions

- **Multi-tenancy**: Most entities carry a `churchId: Ulid` to scope data per church. Exception: `User` is church-independent — church membership is modelled via `ChurchMembership`
- **ULID identifiers**: Uses `com.github.f4b6a3:ulid-creator` for distributed-friendly IDs
- **Financial precision**: All monetary amounts use `BigDecimal`. Use `compareTo` (not `==`) when comparing BigDecimal values — Kotlin's `==` delegates to `equals()` which is scale-sensitive (`BigDecimal("0.00") != BigDecimal.ZERO`)
- **Domain invariants in init blocks**: Entities enforce business rules at construction time (e.g. `FinancialTransaction` validates categorisation amounts sum to total; `Fund` and `BankAccount` enforce zero balance when closed)
- **BankAccount vs Fund**: `BankAccount` (in `account`) = physical bank account; `Fund` = virtual allocation of money for a purpose. A `FinancialTransaction` references a `BankAccount` (where money moved) while its categorisations reference a `Fund` (how it's allocated)
- **API docs**: SpringDoc OpenAPI at `/v3/api-docs` and `/swagger-ui.html`
- **Flyway migrations**: `src/main/resources/db/migration/`, schema `church_finance`. Hibernate `ddl-auto` is set to `validate` — all schema changes must go through Flyway
- **Dependency versions**: Most dependency versions are managed by the Spring BOM via `io.spring.dependency-management`. Only explicitly versioned dependencies (in `build.gradle.kts`) should be bumped directly. The `dependencyUpdates` task will also report transitive/BOM-managed dependencies — these update when their parent plugin or BOM is updated, not independently

## Testing Conventions

- Domain unit tests validate invariants using JUnit 5 (`assertThrows`, `assertNotNull`)
- Service unit tests use MockK (`io.mockk:mockk`) to mock dependencies — use `@MockK` annotations with `MockKAnnotations.init(this)` in `@BeforeTest`
- Controller integration tests use `@SpringBootTest` with `@AutoConfigureMockMvc` (from `org.springframework.boot.webmvc.test.autoconfigure` — Spring Boot 4 relocated this package)
- Each test class has private builder helper methods (e.g. `buildFund()`) with default parameters for constructing test entities
- Nullable string fields should reject blank values — use `isNotBlank()`, not `isNotEmpty()`
- Integration tests use `@SpringBootTest` with `@ImportTestcontainers(TestcontainersConfiguration::class)` — see `TestcontainersConfiguration.kt` for the shared PostgreSQL container setup (uses `org.testcontainers.postgresql.PostgreSQLContainer`, not the deprecated `org.testcontainers.containers` package)
- **Spring profile**: Tests run with the `test` profile (set via `systemProperty` in `build.gradle.kts`) — some components like `StubCurrentUserProvider` are scoped to `local`/`test` profiles only

## Git Conventions

See `src/documents/git-conventions.md` for full details.

- **Branch names**: `feature/`, `fix/`, `chore/`, `docs/` prefixes
- **Commit messages**: Conventional Commits format — `<type>: <short description>` (present tense, lowercase, no period)
- **Types**: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`
- All changes go through PRs to `main`
