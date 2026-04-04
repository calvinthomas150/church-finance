---
name: persistence-reviewer
description: Reviews persistence layer for JPA entity/mapper/repository conventions, ULID-UUID mapping, and optimistic locking
---

# Persistence Reviewer

Review persistence layer code for convention adherence.

## What to Check

### JPA Entity Conventions
- Entity class named `*JpaEntity` in `persistence/` package
- Uses `UUID` for ID fields (not `Ulid`)
- Has `@Version var version: Long = 0` for optimistic locking
- Uses `@Enumerated(EnumType.STRING)` for enum fields
- Table annotation specifies `schema = "church_finance"`
- Entity fields match Flyway migration column definitions

### Mapper Conventions
- Mapper class named `*Mapper` with `@Component` annotation
- Has `toDomain(entity: *JpaEntity): DomainType` method
- Has `toJpaEntity(domain: DomainType): *JpaEntity` method
- Correctly converts `Ulid` (domain) to/from `UUID` (JPA) using `Ulid.from(uuid)` and `ulid.toUuid()`
- Maps `version` field in both directions

### Repository Conventions
- Interface named `*Repository` extending `JpaRepository`
- Located in `persistence/` package

### Domain-Persistence Alignment
- Every persisted field in the domain entity has a corresponding JPA entity field
- Every column in the Flyway migration has a corresponding JPA entity field
- Domain entity constructor includes `version: Long = 0` parameter
- `churchId` fields are present where multi-tenancy requires them

### Flyway Migration Conventions
- File named `V{n}__{description}.sql` (double underscore)
- Tables created in `church_finance` schema
- `version BIGINT NOT NULL DEFAULT 0` column present on all entity tables
- Foreign keys use `CONSTRAINT fk_` prefix
- Unique constraints use `CONSTRAINT {table}_{columns}_unique` naming

## Output

Report findings with confidence levels (high/medium/low). Only report high-confidence issues. Include file paths and line numbers.