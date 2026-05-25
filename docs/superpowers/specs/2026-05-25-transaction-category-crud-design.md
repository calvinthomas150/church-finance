# TransactionCategory — Read / List / Update / Deactivate / Activate

- **Date:** 2026-05-25
- **Branch:** `feature/transaction-category-crud`
- **Status:** Approved — ready for implementation plan
- **Aggregate:** `administration`

## Context

The `TransactionCategory` aggregate currently exposes only `POST /api/v1/churches/{churchId}/transaction-categories` (create). The `Church` aggregate has a full slice (GET / POST / PUT / PATCH activate / PATCH deactivate) that this work mirrors. The branch is named `feature/transaction-category-crud` and should ship a full configuration-CRUD surface before merging to `main`.

Categories are church-level configuration that financial transactions will later reference. They are reference data: once a category has been used by a transaction (future work), it must not be hard-deletable. The design therefore adopts soft-deactivation from day one, even though no transactions reference categories yet — this keeps the deletion model stable across the next aggregate.

## Goals

1. Provide GET-by-id, list (with filters), PUT (update name), PATCH activate, PATCH deactivate — all scoped to a church.
2. Add a `status` field (`ACTIVE` / `INACTIVE`) to support soft-deactivation.
3. Replace the current `(church_id, name)` unique constraint with `(church_id, name, transaction_type)` to let the same name coexist as INCOME and EXPENDITURE.
4. Fix the existing bug where creating a category for a non-existent church returns 500 (DB FK violation) instead of 404.
5. Match the Church aggregate's layered conventions (domain-specific exceptions, optimistic locking, service-layer business rules, `ErrorResponse(message: String)` envelope).

## Non-goals

- No pagination on the list endpoint (categories per church will be dozens; YAGNI).
- No structured error codes in the response envelope (`{ message }` shape is kept).
- No retry-after headers on 409.
- No multi-field validation `details` array (matches Church behaviour).
- No changes to `OpenApiConfig` beyond annotations on the new endpoints.
- No frontend work, no `FinancialTransactionCategorisation` work (those are separate branches per the next-steps doc).
- No backfill of `addedBy` semantics — the current `StubCurrentUserProvider` covers `local` and `test` profiles; production auth is out of scope.

## Design decisions (rationale captured)

| Decision | Choice | Rationale |
|---|---|---|
| Delete model | Soft: status enum + activate/deactivate | Mirrors Church; categories are reference data once transactions exist; switching later would be a breaking schema change. |
| Uniqueness scope | `(church_id, name, transaction_type)` | Lets the same name (e.g. "Tithes") exist as both INCOME and EXPENDITURE in one church; matches the next-steps doc's stated rule. |
| Inactive rows in uniqueness | Counted (no partial index) | Simpler to reason about; reactivation can never collide because the inactive row was already occupying the slot. |
| Update scope | Name only; type immutable | Prevents future violation of the transaction-type-must-match-category-type invariant once transactions reference categories. To change type, deactivate and create new. |
| List default | `ACTIVE` only | The common case for a treasurer UI; `?status=ALL` or `?status=INACTIVE` available; `?type=` filter composes. |
| List filtering location | Service-side over `findAllByChurchId` | Categories per church are small; repo stays simple; one DB round-trip regardless of filter combo. |
| Uniqueness conflict surfacing | Approach A: service pre-check + specific exception | Matches the project's three-layer validation principle; gives an actionable error message; DB constraint remains the safety net for TOCTOU races. |
| Path-consistency | Category's `church_id` must match URL's `{churchId}` | Mismatch returns 404 (not 403/409) to avoid leaking the existence of categories across tenants. |
| Migration default for `status` | `DEFAULT 'ACTIVE'`, kept | JPA always supplies the value on insert, so the DB default is invisible to the app — it exists purely so the `ALTER TABLE ADD COLUMN` succeeds. |

## API surface

All endpoints under `/api/v1/churches/{churchId}/transaction-categories`.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/` | 201 + `TransactionCategoryResponse` | 400 validation, 404 church not found, 409 name conflict |
| GET | `/{id}` | 200 + `TransactionCategoryResponse` | 404 not found / tenant mismatch |
| GET | `/` | 200 + `List<TransactionCategoryResponse>` (sorted by name asc) | 404 church not found |
| PUT | `/{id}` | 200 + `TransactionCategoryResponse` | 400, 404, 409 (name conflict or stale version) |
| PATCH | `/{id}/deactivate?version=N` | 200 + `TransactionCategoryResponse` | 404, 409 stale version |
| PATCH | `/{id}/activate?version=N` | 200 + `TransactionCategoryResponse` | 404, 409 stale version |

### Request DTOs (api/)

**`CreateTransactionCategoryRequest`** (already exists, unchanged):
```kotlin
data class CreateTransactionCategoryRequest(
    @field:NotBlank val name: String,
    val type: FinancialTransactionType,
)
```

**`UpdateTransactionCategoryRequest`** (new):
```kotlin
data class UpdateTransactionCategoryRequest(
    @field:NotBlank val name: String,
    val version: Long,
)
```

**`TransactionCategoryStatusFilter`** (new — query-param-only enum, lives in `api/`):
```kotlin
enum class TransactionCategoryStatusFilter { ACTIVE, INACTIVE, ALL }
```
Spring binds it from `?status=` via enum binding. Default value on the controller method is `ACTIVE`.

### Response DTO

**`TransactionCategoryResponse`** gains `status`:
```kotlin
data class TransactionCategoryResponse(
    val id: UUID,
    val name: String,
    val type: FinancialTransactionType,
    val status: TransactionCategoryStatus,
    val version: Long,
)
```
The existing create-201 integration test will need updating to assert on the new `status` field.

## Domain & persistence changes

### New domain types (administration/domain/)

```kotlin
enum class TransactionCategoryStatus { ACTIVE, INACTIVE }
```

```kotlin
class TransactionCategoryNotFoundException(id: UUID)
    : RuntimeException("Transaction category not found: $id")
```

```kotlin
class TransactionCategoryNameConflictException(
    churchId: UUID,
    name: String,
    type: FinancialTransactionType,
) : RuntimeException(
    "A category named '$name' already exists for $type in this church."
)
```

### TransactionCategory domain class — adds `status`

```kotlin
data class TransactionCategory(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: Instant,
    val addedBy: Ulid,
    val name: String,
    val transactionType: FinancialTransactionType,
    val status: TransactionCategoryStatus = TransactionCategoryStatus.ACTIVE,
    val version: Long = 0,
) {
    init { require(name.isNotBlank()) { "Name must not be blank" } }
}
```

### JPA entity (TransactionCategoryJpaEntity) — adds `status`

```kotlin
@Enumerated(EnumType.STRING)
var status: TransactionCategoryStatus = TransactionCategoryStatus.ACTIVE
```
Mapper round-trips the field in both directions.

### Repository additions

```kotlin
interface TransactionCategoryRepository : JpaRepository<TransactionCategoryJpaEntity, UUID> {
    fun findByChurchIdAndNameAndTransactionType(
        churchId: UUID,
        name: String,
        transactionType: FinancialTransactionType,
    ): TransactionCategoryJpaEntity?

    fun findAllByChurchId(churchId: UUID): List<TransactionCategoryJpaEntity>
}
```

### Flyway migration `V3__transaction_category_status_and_unique.sql`

```sql
ALTER TABLE church_finance.transaction_category
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE church_finance.transaction_category
    DROP CONSTRAINT transaction_category_church_name_unique;

ALTER TABLE church_finance.transaction_category
    ADD CONSTRAINT transaction_category_church_name_type_unique
        UNIQUE (church_id, name, transaction_type);
```

## Service-layer logic

`TransactionCategoryService` gains a `ChurchRepository` dependency and grows the following methods (signature-level — see brainstorming pseudocode for the full shape).

### Public methods

```kotlin
fun create(churchId: UUID, req: CreateTransactionCategoryRequest): TransactionCategoryResponse
fun get(churchId: UUID, id: UUID): TransactionCategoryResponse
fun list(
    churchId: UUID,
    status: TransactionCategoryStatusFilter,   // default ACTIVE — defaulted at controller layer
    type: FinancialTransactionType?,           // null = no type filter
): List<TransactionCategoryResponse>
fun update(churchId: UUID, id: UUID, req: UpdateTransactionCategoryRequest): TransactionCategoryResponse
fun deactivate(churchId: UUID, id: UUID, version: Long): TransactionCategoryResponse
fun activate(churchId: UUID, id: UUID, version: Long): TransactionCategoryResponse
```

### Private helpers

- **`requireChurchExists(churchId: UUID)`** — `churchRepository.existsById(churchId)` else throws `ChurchNotFoundException(churchId)` → 404. Called by `create` and `list`. (For `get`/`update`/`deactivate`/`activate` it's redundant: `findCategoryScopedToChurch` already loads a category and verifies its `churchId`.)
- **`requireNameAvailable(churchId, name, type, excludingId: UUID?)`** — queries `findByChurchIdAndNameAndTransactionType`; if found and `existing.id != excludingId`, throws `TransactionCategoryNameConflictException(churchId, name, type)` → 409.
- **`findCategoryScopedToChurch(churchId, id): TransactionCategory`** — loads by id; if missing OR its `church_id != churchId`, throws `TransactionCategoryNotFoundException(id)` → 404.
- **`saveAndRespond(category): TransactionCategoryResponse`** — existing pattern; persists and rebuilds the DTO from the round-tripped domain entity.

### Validation ordering

- **create**: `requireChurchExists` → `requireNameAvailable(excludingId = null)` → build + save.
- **update**: `findCategoryScopedToChurch` → if `existing.name != req.name` then `requireNameAvailable(existing.transactionType, excludingId = id)` → `copy(name, version)` → save.
- **activate / deactivate**: `findCategoryScopedToChurch` → `copy(status, version)` → save. No uniqueness check needed because inactive rows still count toward uniqueness.
- **list**: `requireChurchExists` → load all → filter by status and type → sort by name → map.

## Error handling

Additions to `GlobalExceptionHandler`:

| New handler | HTTP | Message |
|---|---|---|
| `TransactionCategoryNotFoundException` | 404 | `ex.message` (`"Transaction category not found: <id>"`) |
| `TransactionCategoryNameConflictException` | 409 | `ex.message` (`"A category named '<name>' already exists for <type> in this church."`) |
| `DataIntegrityViolationException` (TOCTOU safety net) | 409 | `"Conflict — please refresh and try again."` |

Reused (existing) handlers:
- `ChurchNotFoundException` → 404
- `MethodArgumentNotValidException` → 400
- `ObjectOptimisticLockingFailureException` → 409 (`"Someone else has made a change. Please refresh and try again."`)

Response envelope is the existing `ErrorResponse(message: String)`.

## Testing approach

Three layers; ~30 new test methods total.

### Service unit tests (`administration/service/TransactionCategoryTest.kt`)

Existing file's create happy-path stays. Add a `ChurchRepository` mock to the setup.

- `create` — existing happy path
- `create throws ChurchNotFoundException when church does not exist`
- `create throws TransactionCategoryNameConflictException when (church, name, type) already exists`
- `get returns category when it belongs to the church`
- `get throws TransactionCategoryNotFoundException when id unknown`
- `get throws TransactionCategoryNotFoundException when category belongs to a different church` (tenant-leak guard)
- `list returns only ACTIVE categories sorted by name when no filters supplied`
- `list with status=ALL returns all rows`
- `list with status=INACTIVE returns only inactive`
- `list with type filter returns only matching type`
- `list with both filters composes correctly`
- `list throws ChurchNotFoundException when church does not exist`
- `update changes name, preserves type and status, returns updated response`
- `update skips uniqueness check when name is unchanged` (verified via MockK `verify(exactly = 0)`)
- `update throws conflict when new name collides with another category of the same type`
- `update permits keeping the same name as the category's own current value` (excludingId guard)
- `update throws not-found on tenant mismatch`
- `deactivate sets status to INACTIVE`
- `activate sets status to ACTIVE`
- `deactivate throws not-found on tenant mismatch`
- `activate throws not-found on tenant mismatch`

### Persistence integration tests (`administration/persistence/TransactionCategoryPersistenceTest.kt`)

Existing round-trip test stays. Add:

- `status field round-trips through JPA` (ACTIVE → save → fetch → flip to INACTIVE → save → fetch)
- `unique constraint rejects duplicate (church_id, name, transaction_type)` — assert `DataIntegrityViolationException`
- `unique constraint allows same name with different transaction_type` (regression-guard for the migration's intent)
- `unique constraint allows same name in different churches` (tenancy boundary sanity)

### Controller integration tests (`administration/api/TransactionCategoryIntegrationTest.kt`)

Existing create-201 test stays but is **updated to assert on the new `status` field** (it doesn't today). Add helpers `createCategory`, `extractId`, `extractVersion` (mirroring the Church integration test helpers).

- `POST returns 409 when name already exists for the same type in the same church`
- `POST returns 201 when name exists but for a different type` (positive uniqueness case)
- `POST returns 404 when church does not exist`
- `GET by id returns the category`
- `GET by id returns 404 when category does not exist`
- `GET by id returns 404 when category belongs to a different church`
- `GET list returns only ACTIVE categories by default`
- `GET list with status=ALL includes inactive`
- `GET list with type=INCOME filters correctly`
- `GET list returns empty list when church has no categories` (not 404 — empty collection is the right answer)
- `GET list returns 404 when church does not exist`
- `GET list sorts by name ascending`
- `PUT updates name and returns 200`
- `PUT returns 400 on blank name`
- `PUT returns 404 on unknown id`
- `PUT returns 404 on tenant mismatch`
- `PUT returns 409 when stale version`
- `PUT returns 409 when new name collides with another category of the same type`
- `PATCH deactivate sets status to INACTIVE`
- `PATCH activate sets status to ACTIVE`
- `PATCH deactivate returns 404 on unknown id or tenant mismatch`
- `PATCH activate returns 404 on unknown id or tenant mismatch`
- `Deactivated category is excluded from default list but included with status=ALL`

### Out of test scope

- OpenAPI / Swagger doc rendering. SpringDoc internals.
- Performance / load testing of list endpoint. Reference-table scale assumed.

## Implementation ordering (informative — `writing-plans` will sequence)

The natural TDD order:

1. Flyway `V3` migration + persistence test for the new unique constraint → green.
2. Domain changes (`TransactionCategoryStatus` enum, `status` field on `TransactionCategory`, two new exceptions) + JPA entity + mapper changes + persistence test for `status` round-trip → green.
3. `TransactionCategoryResponse` gains `status` field; update existing create-201 integration test assertion.
4. Service-level: add `ChurchRepository` dependency, the three helpers, and each new method one at a time with its service unit tests → green.
5. `GlobalExceptionHandler` additions (three new handlers).
6. Controller: add `UpdateTransactionCategoryRequest`, `TransactionCategoryStatusFilter`, and the four new endpoints (GET-by-id, list, PUT, PATCH activate, PATCH deactivate) with OpenAPI annotations matching the Church controller's style.
7. Integration tests for each new endpoint → green.
8. `./gradlew ktlintFormat ktlintCheck test` → all green.
9. Sanity-check the OpenAPI surface in the browser; eyeball-only.

## Open items

None blocking. Items the implementation plan must confirm:

- Today's `TransactionCategoryIntegrationTest.kt` does not assert on `status` (it doesn't exist yet) — update the existing assertion in the same change that adds the field.
- `OpenApiConfig` may not need changes; annotations on the new controller methods should suffice. Confirm during implementation.
