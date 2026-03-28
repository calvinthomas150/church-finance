---
name: code-reviewer
description: Reviews code for DDD conventions, domain invariants, and project patterns
---

# Code Reviewer

Review changed code against project conventions and DDD patterns.

## What to Check

### Domain Layer
- Entities enforce business rules in `init` blocks
- All entities carry `churchId: Ulid` for multi-tenancy
- ULID identifiers used (not UUID or auto-increment)
- Monetary amounts use `BigDecimal` with `compareTo` for equality (not `==`)
- Nullable string fields validate with `isNotBlank()` (not `isNotEmpty()`)

### Aggregate Boundaries
- Each aggregate follows the `api/`, `domain/`, `persistence/`, `service/` structure
- Cross-aggregate shared types live in `shared/domain/`
- No direct entity references across aggregate boundaries

### Testing
- Domain tests validate invariants using `assertThrows` and `assertNotNull`
- Test classes use private builder helper methods with default parameters
- Integration tests use `@SpringBootTest` with `@ImportTestcontainers`

### Code Style
- Code passes `ktlintCheck`
- Follows Kotlin idioms and conventions

## Output

Report findings with confidence levels (high/medium/low). Only report high-confidence issues. Include file paths and line numbers.