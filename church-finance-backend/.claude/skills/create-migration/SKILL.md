---
name: create-migration
description: Create a new Flyway migration file with the correct version number and naming convention
disable-model-invocation: true
---

# Create Migration

Create a new Flyway migration SQL file.

## Steps

1. Find the next version number by listing existing migrations:
```bash
ls src/main/resources/db/migration/V*.sql 2>/dev/null | sort -V | tail -1
```
Extract the highest version number and increment by 1.

2. Ask the user for a short description if not provided as an argument (e.g. "add fund table"). Convert to snake_case for the filename.

3. Create the migration file at:
```
src/main/resources/db/migration/V{n}__{description}.sql
```

4. Write the SQL with these conventions:
   - All tables in `church_finance` schema
   - Include `version BIGINT NOT NULL DEFAULT 0` on entity tables (for optimistic locking)
   - Foreign keys: `CONSTRAINT fk_{table}_{referenced_table}` naming
   - Unique constraints: `CONSTRAINT {table}_{columns}_unique` naming
   - Add indexes on foreign key columns: `CREATE INDEX idx_{table}_{column}`

5. Show the user the generated file for review before proceeding.