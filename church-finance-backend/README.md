## Small Church Finance App

## Scope

Church financial management for small independent churches, covering income, expenditure, fund management, budgeting, reporting and Gift Aid submission generation. Multi-tenant. Parent charity relationships and inter-charity reporting are out of scope.

## Prerequisites

- Java 21
- Docker (required for tests — Testcontainers runs PostgreSQL)

## Local Development

1. Copy the example config and fill in your database credentials:
   ```bash
   cp src/main/resources/application-local.yaml.example src/main/resources/application-local.yaml
   ```

2. Run the application with the local profile:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

3. API docs are available at `/swagger-ui.html` when running.

## Database

- PostgreSQL with Flyway migrations
- Schema: `church_finance`
- Hibernate `ddl-auto` is set to `validate` — all schema changes must go through Flyway migrations in `src/main/resources/db/migration/`

## Testing

```bash
./gradlew test
```

Tests use Testcontainers with PostgreSQL — Docker must be running. No local database setup is needed for tests.

## Contributing

This project uses KtLint to maintain consistency. Set up the git hook via:
```bash
./gradlew addKtlintCheckGitPreCommitHook
```
