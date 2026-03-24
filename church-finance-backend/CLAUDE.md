# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew build                        # Build the project
./gradlew test                         # Run all tests
./gradlew test --tests='*ClassName*'   # Run a specific test class
./gradlew bootRun                      # Run the application
./gradlew ktlintFormat                 # Auto-format Kotlin code
./gradlew addKtlintCheckGitPreCommitHook  # Install KtLint pre-commit hook
```

Tests use Testcontainers with PostgreSQL — Docker must be running.

## Architecture

**Spring Boot 4.0.3 / Kotlin 2.3.10 / Java 21 / PostgreSQL / Flyway**

The project follows Domain-Driven Design organized by aggregate. Each aggregate lives under `src/main/kotlin/com/calvintech/churchfinance/` with a consistent layered structure:

```
<aggregate>/
├── api/           # REST controllers
├── domain/        # Entities, value objects, business rules
├── persistence/   # JPA repositories
└── service/       # Application services
```

**Aggregates**: administration, financial-transaction, bank-reconciliation, account, budget, donor, giftaid, report

The domain model was derived from event storming — see `src/documents/domain-model.md` for the full aggregate and event definitions.

## Key Design Decisions

- **Multi-tenancy**: All entities carry a `churchId: Ulid` to scope data per church
- **ULID identifiers**: Uses `com.github.f4b6a3:ulid-creator` for distributed-friendly IDs
- **Financial precision**: All monetary amounts use `BigDecimal`
- **Categorisation validation**: `FinancialTransaction` enforces that categorisation amounts sum to the transaction total (domain invariant in init block)
- **API docs**: SpringDoc OpenAPI at `/v3/api-docs` and `/swagger-ui.html`

## Git Conventions

See `src/documents/git-conventions.md` for full details.

- **Branch names**: `feature/`, `fix/`, `chore/`, `docs/` prefixes
- **Commit messages**: Conventional Commits format — `<type>: <short description>` (present tense, lowercase, no period)
- **Types**: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`
- All changes go through PRs to `main`
