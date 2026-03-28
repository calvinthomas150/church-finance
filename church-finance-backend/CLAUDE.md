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
```

Tests use Testcontainers with PostgreSQL — Docker must be running.

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

**Aggregates** (directory names): `account`, `administration`, `budget`, `donor`, `financialtransaction`, `fund`, `giftaid`, `reconciliation`, `report`

Cross-aggregate shared types (e.g. `FinancialTransactionType`) live in `shared/domain/`.

The domain model was derived from event storming — see `src/documents/domain-model.md` for the full aggregate and event definitions.

## Key Design Decisions

- **Multi-tenancy**: All entities carry a `churchId: Ulid` to scope data per church
- **ULID identifiers**: Uses `com.github.f4b6a3:ulid-creator` for distributed-friendly IDs
- **Financial precision**: All monetary amounts use `BigDecimal`. Use `compareTo` (not `==`) when comparing BigDecimal values — Kotlin's `==` delegates to `equals()` which is scale-sensitive (`BigDecimal("0.00") != BigDecimal.ZERO`)
- **Domain invariants in init blocks**: Entities enforce business rules at construction time (e.g. `FinancialTransaction` validates categorisation amounts sum to total; `Fund` and `BankAccount` enforce zero balance when closed)
- **BankAccount vs Fund**: `BankAccount` (in `account`) = physical bank account; `Fund` = virtual allocation of money for a purpose. A `FinancialTransaction` references a `BankAccount` (where money moved) while its categorisations reference a `Fund` (how it's allocated)
- **API docs**: SpringDoc OpenAPI at `/v3/api-docs` and `/swagger-ui.html`
- **Flyway migrations**: `src/main/resources/db/migration/` (not yet populated)

## Testing Conventions

- Domain unit tests validate invariants using JUnit 5 (`assertThrows`, `assertNotNull`)
- Each test class has private builder helper methods (e.g. `buildFund()`) with default parameters for constructing test entities
- Nullable string fields should reject blank values — use `isNotBlank()`, not `isNotEmpty()`
- Integration tests use `@SpringBootTest` with `@ImportTestcontainers` — see `TestcontainersConfiguration.kt` for the shared PostgreSQL container setup

## Git Conventions

See `src/documents/git-conventions.md` for full details.

- **Branch names**: `feature/`, `fix/`, `chore/`, `docs/` prefixes
- **Commit messages**: Conventional Commits format — `<type>: <short description>` (present tense, lowercase, no period)
- **Types**: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`
- All changes go through PRs to `main`
