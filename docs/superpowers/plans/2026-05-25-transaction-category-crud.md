# TransactionCategory CRUD Completion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Commits:** Calvin commits to git himself. Every task ends with a "Stage and commit" step showing the exact `git add` + `git commit` commands — these are commands **Calvin runs**, not commands the implementing agent runs. The agent stops at that step and surfaces the suggested message.

**Goal:** Complete the TransactionCategory CRUD surface on `feature/transaction-category-crud` — GET-by-id, list (filterable), PUT (name only), PATCH activate/deactivate — mirroring the existing Church aggregate pattern. Adds a soft-deactivation status field, swaps the unique constraint to be type-scoped, and fixes a 500-vs-404 bug on missing church.

**Architecture:** Backend Kotlin/Spring Boot/JPA following DDD vertical-slice packaging (`administration` aggregate, four sub-packages: `domain`, `persistence`, `service`, `api`). Service-layer pre-emptive validation for uniqueness and church existence; DB constraint as TOCTOU safety net. Tenant-scoped 404 (never leak that an entity exists in a different church). Domain entities use Ulid IDs; persistence layer uses UUIDs; manual mappers translate.

**Tech Stack:** Kotlin 2.3.20, Spring Boot 4.0.5, Java 21, PostgreSQL via Flyway, Spring Data JPA, JUnit 5 + kotlin.test, MockK for service unit tests, Testcontainers (`PostgreSQLContainer`) for persistence + integration tests, SpringDoc OpenAPI.

**Spec:** [`docs/superpowers/specs/2026-05-25-transaction-category-crud-design.md`](../specs/2026-05-25-transaction-category-crud-design.md)

---

## File Structure

**Backend root for all paths below:** `church-finance-backend/`

**New files to create:**

| Path | Responsibility |
|---|---|
| `src/main/resources/db/migration/V3__transaction_category_status_and_unique.sql` | Add `status` column; swap unique constraint to `(church_id, name, transaction_type)` |
| `src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryStatus.kt` | `ACTIVE` / `INACTIVE` enum |
| `src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNotFoundException.kt` | Domain exception, mapped to 404 |
| `src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNameConflictException.kt` | Domain exception, mapped to 409 |
| `src/main/kotlin/com/calvintech/churchfinance/administration/api/UpdateTransactionCategoryRequest.kt` | Request DTO for PUT |
| `src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryStatusFilter.kt` | Query-param enum for list endpoint |

**Existing files to modify:**

| Path | Responsibility |
|---|---|
| `src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategory.kt` | Add `status` field |
| `src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryJpaEntity.kt` | Add `status` column |
| `src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryMapper.kt` | Round-trip `status` |
| `src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryRepository.kt` | Add two derived-query methods |
| `src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryResponse.kt` | Add `status` field |
| `src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt` | Add 4 new endpoints (GET-by-id, list, PUT, PATCH activate/deactivate) |
| `src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt` | Add `ChurchRepository` dep, 3 helpers, 5 new methods |
| `src/main/kotlin/com/calvintech/churchfinance/shared/api/GlobalExceptionHandler.kt` | Add 3 handlers (NotFound 404, NameConflict 409, DataIntegrityViolation 409) |

**Test files to modify (no new test files needed):**

| Path | Additions |
|---|---|
| `src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt` | Status round-trip; 3 constraint tests; 2 repo query tests |
| `src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt` | ~20 new MockK tests |
| `src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt` | ~22 new MockMvc tests + helper methods |

---

## Task 1: Flyway V3 migration + persistence tests for new unique constraint

**Files:**
- Create: `church-finance-backend/src/main/resources/db/migration/V3__transaction_category_status_and_unique.sql`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt`

- [ ] **Step 1: Write failing persistence tests for new constraint behavior**

Open `TransactionCategoryPersistenceTest.kt` and add (alongside the existing test) — leave the existing round-trip test untouched:

```kotlin
@Test
fun `unique constraint allows same name with different transaction_type`() {
    val churchId = persistChurch()
    val income = buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME)
    val expense = buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.EXPENSE)

    transactionCategoryRepository.saveAndFlush(income)
    transactionCategoryRepository.saveAndFlush(expense)

    val all = transactionCategoryRepository.findAll().filter { it.churchId == churchId }
    assertEquals(2, all.size)
}

@Test
fun `unique constraint allows same name in different churches`() {
    val churchOne = persistChurch(name = "Church One")
    val churchTwo = persistChurch(name = "Church Two")
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchOne, name = "Tithes", type = FinancialTransactionType.INCOME),
    )
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchTwo, name = "Tithes", type = FinancialTransactionType.INCOME),
    )

    val all = transactionCategoryRepository.findAll()
    assertEquals(2, all.count { it.name == "Tithes" })
}

@Test
fun `unique constraint rejects duplicate church_id, name, transaction_type`() {
    val churchId = persistChurch()
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME),
    )

    assertFailsWith<DataIntegrityViolationException> {
        transactionCategoryRepository.saveAndFlush(
            buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME),
        )
    }
}
```

You will need (add to imports + helpers in the test file — see Step 2):

```kotlin
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
```

- [ ] **Step 2: Ensure test helpers exist in the file**

The existing test file may or may not have `buildJpaEntity` / `persistChurch` helpers. If not, add them as private methods inside the test class:

```kotlin
private fun persistChurch(name: String = "Test Church"): UUID {
    val church = ChurchJpaEntity(
        id = UUID.randomUUID(),
        createdAt = Instant.now(),
        addedBy = UUID.randomUUID(),
        name = name,
        status = ChurchStatus.ACTIVE,
    )
    churchRepository.saveAndFlush(church)
    return church.id
}

private fun buildJpaEntity(
    churchId: UUID,
    name: String,
    type: FinancialTransactionType,
): TransactionCategoryJpaEntity =
    TransactionCategoryJpaEntity(
        id = UUID.randomUUID(),
        churchId = churchId,
        createdAt = Instant.now(),
        addedBy = UUID.randomUUID(),
        name = name,
        transactionType = type,
    )
```

You'll also need `@Autowired lateinit var churchRepository: ChurchRepository` in the test class if it's not already there, plus the obvious imports (`ChurchJpaEntity`, `ChurchStatus`, `ChurchRepository`, `FinancialTransactionType`, `Instant`, `UUID`).

- [ ] **Step 3: Run new tests to confirm they fail**

Run:

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest*' --rerun-tasks
```

Expected: the two new "allows" tests fail (DB still rejects same name across different types) OR the "rejects duplicate" test still passes but for the wrong reason (current constraint is `(church_id, name)`). At minimum, `unique constraint allows same name with different transaction_type` should fail with `DataIntegrityViolationException`. That is the regression we are fixing.

- [ ] **Step 4: Create the Flyway migration**

Create `church-finance-backend/src/main/resources/db/migration/V3__transaction_category_status_and_unique.sql`:

```sql
ALTER TABLE church_finance.transaction_category
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE church_finance.transaction_category
    DROP CONSTRAINT transaction_category_church_name_unique;

ALTER TABLE church_finance.transaction_category
    ADD CONSTRAINT transaction_category_church_name_type_unique
        UNIQUE (church_id, name, transaction_type);
```

Note: the `status` column is added now so we don't need a fourth migration in Task 2. JPA does not yet map it (Task 2 wires that), but the schema has it with a safe default.

- [ ] **Step 5: Run new tests to confirm they pass**

Run:

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest*' --rerun-tasks
```

Expected: all three new tests PASS. The existing round-trip test also PASSES (it doesn't reference `status` yet — Task 2 updates that).

- [ ] **Step 6: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/resources/db/migration/V3__transaction_category_status_and_unique.sql \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt
git commit -m "feat: swap transaction_category unique constraint to (church, name, type)

Add Flyway V3: introduces status column (defaulted ACTIVE) and
replaces (church_id, name) unique constraint with type-scoped
variant. Adds persistence tests covering: duplicate rejection,
same-name-different-type allowance, and cross-church allowance."
```

---

## Task 2: Add `TransactionCategoryStatus` enum + `status` field through domain, JPA, mapper

**Files:**
- Create: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryStatus.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategory.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryJpaEntity.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryMapper.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt`

- [ ] **Step 1: Write failing test for status round-trip**

Add to `TransactionCategoryPersistenceTest.kt`:

```kotlin
@Test
fun `status field round-trips through JPA`() {
    val churchId = persistChurch()
    val entity = buildJpaEntity(churchId = churchId, name = "Offerings", type = FinancialTransactionType.INCOME)
        .also { it.status = TransactionCategoryStatus.ACTIVE }

    transactionCategoryRepository.saveAndFlush(entity)
    val loaded = transactionCategoryRepository.findById(entity.id).orElseThrow()
    assertEquals(TransactionCategoryStatus.ACTIVE, loaded.status)

    loaded.status = TransactionCategoryStatus.INACTIVE
    transactionCategoryRepository.saveAndFlush(loaded)
    val reloaded = transactionCategoryRepository.findById(entity.id).orElseThrow()
    assertEquals(TransactionCategoryStatus.INACTIVE, reloaded.status)
}
```

Also update the existing round-trip test (whatever its name is) to assert `status == ACTIVE` on the round-tripped domain entity. Add to imports: `com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus`.

- [ ] **Step 2: Run new test to verify it fails**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest.status field round-trips*' --rerun-tasks
```

Expected: FAIL — `TransactionCategoryStatus` doesn't exist yet (compile error).

- [ ] **Step 3: Create `TransactionCategoryStatus` enum**

Create `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryStatus.kt`:

```kotlin
package com.calvintech.churchfinance.administration.domain

enum class TransactionCategoryStatus {
    ACTIVE,
    INACTIVE,
}
```

- [ ] **Step 4: Add `status` field to domain class**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategory.kt`:

```kotlin
package com.calvintech.churchfinance.administration.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.Ulid
import java.time.Instant

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
    init {
        require(name.isNotBlank()) {
            "Name must not be blank"
        }
    }
}
```

- [ ] **Step 5: Add `status` column to JPA entity**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryJpaEntity.kt`:

```kotlin
package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transaction_category", schema = "church_finance")
class TransactionCategoryJpaEntity(
    @Id
    var id: UUID,
    var churchId: UUID,
    var createdAt: Instant,
    var addedBy: UUID,
    var name: String,
    @Enumerated(EnumType.STRING)
    var transactionType: FinancialTransactionType,
    @Enumerated(EnumType.STRING)
    var status: TransactionCategoryStatus = TransactionCategoryStatus.ACTIVE,
    @Version
    var version: Long = 0,
)
```

- [ ] **Step 6: Update mapper to round-trip status**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryMapper.kt`:

```kotlin
package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.github.f4b6a3.ulid.Ulid
import org.springframework.stereotype.Component

@Component
class TransactionCategoryMapper {
    fun toDomain(entity: TransactionCategoryJpaEntity): TransactionCategory =
        TransactionCategory(
            id = Ulid.from(entity.id),
            churchId = Ulid.from(entity.churchId),
            createdAt = entity.createdAt,
            name = entity.name,
            addedBy = Ulid.from(entity.addedBy),
            transactionType = entity.transactionType,
            status = entity.status,
            version = entity.version,
        )

    fun toJpaEntity(category: TransactionCategory): TransactionCategoryJpaEntity =
        TransactionCategoryJpaEntity(
            id = category.id.toUuid(),
            churchId = category.churchId.toUuid(),
            createdAt = category.createdAt,
            addedBy = category.addedBy.toUuid(),
            name = category.name,
            transactionType = category.transactionType,
            status = category.status,
            version = category.version,
        )
}
```

- [ ] **Step 7: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest*' --rerun-tasks
```

Expected: all persistence tests PASS, including the new round-trip test.

- [ ] **Step 8: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryStatus.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategory.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryJpaEntity.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryMapper.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt
git commit -m "feat: add status field to TransactionCategory domain and JPA

Introduces TransactionCategoryStatus (ACTIVE/INACTIVE) and threads
it through the domain entity, JPA entity, and mapper. Persistence
test now asserts the field round-trips correctly."
```

---

## Task 3: Add `status` to `TransactionCategoryResponse`; fix existing create-201 test

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryResponse.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Update existing integration test to expect status**

In `TransactionCategoryIntegrationTest.kt`, find the existing test `POST should create a transaction category and return 201`. Add one expectation:

```kotlin
.andExpect(jsonPath("$.status").value("ACTIVE"))
```

…inserted after the existing `jsonPath("$.type")` expectation.

- [ ] **Step 2: Run test to confirm it fails**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: FAIL — `status` field doesn't exist on the response yet.

- [ ] **Step 3: Add `status` to response DTO**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryResponse.kt`:

```kotlin
package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import java.util.UUID

data class TransactionCategoryResponse(
    val id: UUID,
    val name: String,
    val type: FinancialTransactionType,
    val status: TransactionCategoryStatus,
    val version: Long,
)
```

- [ ] **Step 4: Update service `toResponse` to set `status`**

In `TransactionCategoryService.kt`, replace the `toResponse` private method:

```kotlin
private fun toResponse(transactionCategory: TransactionCategory): TransactionCategoryResponse =
    TransactionCategoryResponse(
        id = transactionCategory.id.toUuid(),
        name = transactionCategory.name,
        type = transactionCategory.transactionType,
        status = transactionCategory.status,
        version = transactionCategory.version,
    )
```

(`create` already builds the `TransactionCategory` with the default `status = ACTIVE` from Task 2's domain change — no change needed there.)

- [ ] **Step 5: Update existing service unit test**

In `TransactionCategoryTest.kt` (service test), the existing `create should persist and return a new transaction category` test still passes because `buildTransactionCategory` defaults to ACTIVE. No change required, but add `assertEquals(TransactionCategoryStatus.ACTIVE, response.status)` near the existing assertions as belt-and-braces coverage. Add the import.

- [ ] **Step 6: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryIntegrationTest*' --tests='*TransactionCategoryTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 7: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryResponse.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt
git commit -m "feat: expose status on TransactionCategoryResponse"
```

---

## Task 4: Add new exception types and global handlers

**Files:**
- Create: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNotFoundException.kt`
- Create: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNameConflictException.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/shared/api/GlobalExceptionHandler.kt`

This task is exception types + their handlers — no test in this task because the handlers can't be tested in isolation; they're exercised by integration tests starting Task 8.

- [ ] **Step 1: Create `TransactionCategoryNotFoundException`**

Create `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNotFoundException.kt`:

```kotlin
package com.calvintech.churchfinance.administration.domain

import java.util.UUID

class TransactionCategoryNotFoundException(
    id: UUID,
) : RuntimeException("Transaction category not found: $id")
```

- [ ] **Step 2: Create `TransactionCategoryNameConflictException`**

Create `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNameConflictException.kt`:

```kotlin
package com.calvintech.churchfinance.administration.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import java.util.UUID

class TransactionCategoryNameConflictException(
    churchId: UUID,
    name: String,
    type: FinancialTransactionType,
) : RuntimeException(
    "A category named '$name' already exists for $type in this church.",
)
```

- [ ] **Step 3: Add handlers to `GlobalExceptionHandler`**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/shared/api/GlobalExceptionHandler.kt`:

```kotlin
package com.calvintech.churchfinance.shared.api

import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ChurchNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleChurchNotFound(ex: ChurchNotFoundException): ErrorResponse = ErrorResponse(ex.message ?: "Church not found")

    @ExceptionHandler(TransactionCategoryNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTransactionCategoryNotFound(ex: TransactionCategoryNotFoundException): ErrorResponse =
        ErrorResponse(ex.message ?: "Transaction category not found")

    @ExceptionHandler(TransactionCategoryNameConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleTransactionCategoryNameConflict(ex: TransactionCategoryNameConflictException): ErrorResponse =
        ErrorResponse(ex.message ?: "Category name conflict")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ErrorResponse {
        val message =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "Validation failed" }
        return ErrorResponse(message)
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleOptimisticLock(): ErrorResponse = ErrorResponse("Someone else has made a change. Please refresh and try again.")

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrityViolation(): ErrorResponse = ErrorResponse("Conflict — please refresh and try again.")
}
```

- [ ] **Step 4: Compile-check**

```bash
./gradlew :church-finance-backend:compileKotlin
```

Expected: SUCCESS — no compile errors.

- [ ] **Step 5: Run existing test suite to confirm no regression**

```bash
./gradlew :church-finance-backend:test --rerun-tasks
```

Expected: all existing tests still PASS.

- [ ] **Step 6: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNotFoundException.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/domain/TransactionCategoryNameConflictException.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/shared/api/GlobalExceptionHandler.kt
git commit -m "feat: add TransactionCategory domain exceptions and HTTP handlers

Adds TransactionCategoryNotFoundException (404) and
TransactionCategoryNameConflictException (409) with handlers in
GlobalExceptionHandler. Adds DataIntegrityViolationException
handler (409) as a TOCTOU safety net for the unique constraint."
```

---

## Task 5: Add repository derived-query methods

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryRepository.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt`

- [ ] **Step 1: Write failing tests**

Add to `TransactionCategoryPersistenceTest.kt`:

```kotlin
@Test
fun `findByChurchIdAndNameAndTransactionType returns matching category`() {
    val churchId = persistChurch()
    val entity = buildJpaEntity(churchId = churchId, name = "Offerings", type = FinancialTransactionType.INCOME)
    transactionCategoryRepository.saveAndFlush(entity)

    val found = transactionCategoryRepository.findByChurchIdAndNameAndTransactionType(
        churchId = churchId,
        name = "Offerings",
        transactionType = FinancialTransactionType.INCOME,
    )

    assertNotNull(found)
    assertEquals(entity.id, found.id)
}

@Test
fun `findByChurchIdAndNameAndTransactionType returns null when type differs`() {
    val churchId = persistChurch()
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchId, name = "Offerings", type = FinancialTransactionType.INCOME),
    )

    val found = transactionCategoryRepository.findByChurchIdAndNameAndTransactionType(
        churchId = churchId,
        name = "Offerings",
        transactionType = FinancialTransactionType.EXPENSE,
    )

    assertNull(found)
}

@Test
fun `findAllByChurchId returns only categories for that church`() {
    val churchOne = persistChurch("Church One")
    val churchTwo = persistChurch("Church Two")
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchOne, name = "A", type = FinancialTransactionType.INCOME),
    )
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchOne, name = "B", type = FinancialTransactionType.EXPENSE),
    )
    transactionCategoryRepository.saveAndFlush(
        buildJpaEntity(churchId = churchTwo, name = "C", type = FinancialTransactionType.INCOME),
    )

    val results = transactionCategoryRepository.findAllByChurchId(churchOne)

    assertEquals(2, results.size)
    assertTrue(results.all { it.churchId == churchOne })
}
```

Add to imports: `kotlin.test.assertNotNull`, `kotlin.test.assertNull`, `kotlin.test.assertTrue`.

- [ ] **Step 2: Run tests to confirm they fail**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest.findBy*' --tests='*TransactionCategoryPersistenceTest.findAllByChurchId*' --rerun-tasks
```

Expected: FAIL — methods don't exist (compile error).

- [ ] **Step 3: Add methods to repository**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryRepository.kt`:

```kotlin
package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionCategoryRepository : JpaRepository<TransactionCategoryJpaEntity, UUID> {
    fun findByChurchIdAndNameAndTransactionType(
        churchId: UUID,
        name: String,
        transactionType: FinancialTransactionType,
    ): TransactionCategoryJpaEntity?

    fun findAllByChurchId(churchId: UUID): List<TransactionCategoryJpaEntity>
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryPersistenceTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 5: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryRepository.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/persistence/TransactionCategoryPersistenceTest.kt
git commit -m "feat: add TransactionCategoryRepository derived queries"
```

---

## Task 6: Service — `ChurchRepository` dependency + `requireChurchExists` + create rejects missing church

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service unit test**

In `TransactionCategoryTest.kt`:

1. Add a new MockK field:

```kotlin
@MockK
lateinit var churchRepository: ChurchRepository
```

2. Update the `setUp` constructor call to:

```kotlin
transactionCategoryService = TransactionCategoryService(mapper, repository, userProvider, churchRepository)
```

3. In the existing `create should persist...` test, add a stub line so the new helper passes:

```kotlin
every { churchRepository.existsById(churchId) } returns true
```

…before the first `every { userProvider... }` line. Otherwise the existing test will start failing.

4. Add the new test:

```kotlin
@Test
fun `create throws ChurchNotFoundException when church does not exist`() {
    val churchId = UUID.randomUUID()
    every { churchRepository.existsById(churchId) } returns false

    assertFailsWith<ChurchNotFoundException> {
        transactionCategoryService.create(
            churchId,
            CreateTransactionCategoryRequest(name = "Offerings", type = FinancialTransactionType.INCOME),
        )
    }
}
```

Add imports: `ChurchRepository`, `ChurchNotFoundException`, `kotlin.test.assertFailsWith`.

- [ ] **Step 2: Write failing integration test**

In `TransactionCategoryIntegrationTest.kt`, add:

```kotlin
@Test
fun `POST should return 404 when church does not exist`() {
    val unknownChurchId = UUID.randomUUID()
    mockMvc
        .perform(
            post("/api/v1/churches/{churchId}/transaction-categories", unknownChurchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Offerings", "type": "INCOME"}"""),
        ).andExpect(status().isNotFound)
        .andExpect(jsonPath("$.message").exists())
}
```

- [ ] **Step 3: Run tests to confirm they fail**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest.create throws ChurchNotFoundException*' --tests='*TransactionCategoryIntegrationTest.POST should return 404*' --rerun-tasks
```

Expected: FAIL (the integration test currently returns 500 due to FK violation; the service test fails because the helper doesn't exist).

- [ ] **Step 4: Update service to inject `ChurchRepository` and gate `create` on existence**

Replace `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`:

```kotlin
package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryResponse
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TransactionCategoryService(
    private val mapper: TransactionCategoryMapper,
    private val repository: TransactionCategoryRepository,
    private val userProvider: CurrentUserProvider,
    private val churchRepository: ChurchRepository,
) {
    fun create(
        churchId: UUID,
        createTransactionCategoryRequest: CreateTransactionCategoryRequest,
    ): TransactionCategoryResponse {
        requireChurchExists(churchId)

        val transactionCategory =
            TransactionCategory(
                id = UlidCreator.getUlid(),
                churchId = Ulid.from(churchId),
                createdAt = Instant.now(),
                addedBy = userProvider.getCurrentUserId(),
                name = createTransactionCategoryRequest.name,
                transactionType = createTransactionCategoryRequest.type,
            )

        return saveAndRespond(transactionCategory)
    }

    private fun requireChurchExists(churchId: UUID) {
        if (!churchRepository.existsById(churchId)) {
            throw ChurchNotFoundException(churchId)
        }
    }

    private fun saveAndRespond(transactionCategory: TransactionCategory): TransactionCategoryResponse {
        val persisted = repository.save(mapper.toJpaEntity(transactionCategory))
        return toResponse(mapper.toDomain(persisted))
    }

    private fun toResponse(transactionCategory: TransactionCategory): TransactionCategoryResponse =
        TransactionCategoryResponse(
            id = transactionCategory.id.toUuid(),
            name = transactionCategory.name,
            type = transactionCategory.transactionType,
            status = transactionCategory.status,
            version = transactionCategory.version,
        )
}
```

- [ ] **Step 5: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 6: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: gate create on church existence with 404 response"
```

---

## Task 7: Service — `requireNameAvailable` helper + create rejects name conflict

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service test**

In `TransactionCategoryTest.kt`:

```kotlin
@Test
fun `create throws TransactionCategoryNameConflictException when (church, name, type) already exists`() {
    val churchId = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(name = "Offerings", transactionType = FinancialTransactionType.INCOME)

    every { churchRepository.existsById(churchId) } returns true
    every {
        repository.findByChurchIdAndNameAndTransactionType(
            churchId = churchId,
            name = "Offerings",
            transactionType = FinancialTransactionType.INCOME,
        )
    } returns existing

    assertFailsWith<TransactionCategoryNameConflictException> {
        transactionCategoryService.create(
            churchId,
            CreateTransactionCategoryRequest(name = "Offerings", type = FinancialTransactionType.INCOME),
        )
    }
}
```

Then update the existing `create should persist...` test: after `every { churchRepository.existsById(...) } returns true`, add:

```kotlin
every {
    repository.findByChurchIdAndNameAndTransactionType(
        churchId = churchId,
        name = "Offerings",
        transactionType = FinancialTransactionType.INCOME,
    )
} returns null
```

Add import: `TransactionCategoryNameConflictException`.

- [ ] **Step 2: Write failing integration tests**

In `TransactionCategoryIntegrationTest.kt`. First extract a small helper at the top of the class to reduce duplication:

```kotlin
private fun postCategory(churchId: UUID, name: String, type: String) =
    mockMvc.perform(
        post("/api/v1/churches/{churchId}/transaction-categories", churchId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "$name", "type": "$type"}"""),
    )
```

Then add:

```kotlin
@Test
fun `POST should return 409 when name already exists for the same type in the same church`() {
    val churchId = UUID.fromString(createChurch())
    postCategory(churchId, "Tithes", "INCOME").andExpect(status().isCreated)

    postCategory(churchId, "Tithes", "INCOME")
        .andExpect(status().isConflict)
        .andExpect(jsonPath("$.message").exists())
}

@Test
fun `POST should return 201 when name exists but for a different type`() {
    val churchId = UUID.fromString(createChurch())
    postCategory(churchId, "Tithes", "INCOME").andExpect(status().isCreated)

    postCategory(churchId, "Tithes", "EXPENSE").andExpect(status().isCreated)
}
```

- [ ] **Step 3: Run tests to confirm failure**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: new tests FAIL — service has no uniqueness pre-check yet.

- [ ] **Step 4: Update service with `requireNameAvailable`**

Replace `TransactionCategoryService.kt`:

```kotlin
package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryResponse
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TransactionCategoryService(
    private val mapper: TransactionCategoryMapper,
    private val repository: TransactionCategoryRepository,
    private val userProvider: CurrentUserProvider,
    private val churchRepository: ChurchRepository,
) {
    fun create(
        churchId: UUID,
        createTransactionCategoryRequest: CreateTransactionCategoryRequest,
    ): TransactionCategoryResponse {
        requireChurchExists(churchId)
        requireNameAvailable(
            churchId = churchId,
            name = createTransactionCategoryRequest.name,
            type = createTransactionCategoryRequest.type,
            excludingId = null,
        )

        val transactionCategory =
            TransactionCategory(
                id = UlidCreator.getUlid(),
                churchId = Ulid.from(churchId),
                createdAt = Instant.now(),
                addedBy = userProvider.getCurrentUserId(),
                name = createTransactionCategoryRequest.name,
                transactionType = createTransactionCategoryRequest.type,
            )

        return saveAndRespond(transactionCategory)
    }

    private fun requireChurchExists(churchId: UUID) {
        if (!churchRepository.existsById(churchId)) {
            throw ChurchNotFoundException(churchId)
        }
    }

    private fun requireNameAvailable(
        churchId: UUID,
        name: String,
        type: FinancialTransactionType,
        excludingId: UUID?,
    ) {
        val conflict = repository.findByChurchIdAndNameAndTransactionType(
            churchId = churchId,
            name = name,
            transactionType = type,
        )
        if (conflict != null && conflict.id != excludingId) {
            throw TransactionCategoryNameConflictException(churchId, name, type)
        }
    }

    private fun saveAndRespond(transactionCategory: TransactionCategory): TransactionCategoryResponse {
        val persisted = repository.save(mapper.toJpaEntity(transactionCategory))
        return toResponse(mapper.toDomain(persisted))
    }

    private fun toResponse(transactionCategory: TransactionCategory): TransactionCategoryResponse =
        TransactionCategoryResponse(
            id = transactionCategory.id.toUuid(),
            name = transactionCategory.name,
            type = transactionCategory.transactionType,
            status = transactionCategory.status,
            version = transactionCategory.version,
        )
}
```

- [ ] **Step 5: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 6: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: pre-emptive uniqueness check on category create

Service queries (church_id, name, transaction_type) before
insert and throws TransactionCategoryNameConflictException
when the slot is taken. DB constraint remains the safety net."
```

---

## Task 8: Service `get()` + `findCategoryScopedToChurch` helper + controller GET-by-id

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service unit tests**

Add to `TransactionCategoryTest.kt`:

```kotlin
@Test
fun `get returns category when it belongs to the church`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val entity = buildTransactionCategoryJpaEntity(id = id, name = "Offerings", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val domain = buildTransactionCategory(name = "Offerings", transactionType = FinancialTransactionType.INCOME)

    every { repository.findById(id) } returns java.util.Optional.of(entity)
    every { mapper.toDomain(entity) } returns domain.copy(churchId = Ulid.from(churchId))

    val response = transactionCategoryService.get(churchId, id)

    assertEquals("Offerings", response.name)
}

@Test
fun `get throws TransactionCategoryNotFoundException when id unknown`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    every { repository.findById(id) } returns java.util.Optional.empty()

    assertFailsWith<TransactionCategoryNotFoundException> {
        transactionCategoryService.get(churchId, id)
    }
}

@Test
fun `get throws TransactionCategoryNotFoundException when category belongs to a different church`() {
    val urlChurchId = UUID.randomUUID()
    val actualChurchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val entity = buildTransactionCategoryJpaEntity(id = id, name = "Offerings", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = actualChurchId }
    val domain = buildTransactionCategory(name = "Offerings", transactionType = FinancialTransactionType.INCOME)
        .copy(churchId = Ulid.from(actualChurchId))

    every { repository.findById(id) } returns java.util.Optional.of(entity)
    every { mapper.toDomain(entity) } returns domain

    assertFailsWith<TransactionCategoryNotFoundException> {
        transactionCategoryService.get(urlChurchId, id)
    }
}
```

Imports: `com.github.f4b6a3.ulid.Ulid`, `TransactionCategoryNotFoundException`.

- [ ] **Step 2: Write failing integration tests**

In `TransactionCategoryIntegrationTest.kt`, add a helper:

```kotlin
private fun createCategoryAndReturnBody(
    churchId: UUID,
    name: String = "Offerings",
    type: String = "INCOME",
): String =
    mockMvc.perform(
        post("/api/v1/churches/{churchId}/transaction-categories", churchId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "$name", "type": "$type"}"""),
    ).andExpect(status().isCreated)
        .andReturn().response.contentAsString

private fun extractId(body: String): String = objectMapper.readTree(body).get("id").asString()
```

Add tests:

```kotlin
@Test
fun `GET by id returns the category`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "Tithes", "INCOME"))

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Tithes"))
        .andExpect(jsonPath("$.type").value("INCOME"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
}

@Test
fun `GET by id returns 404 when category does not exist`() {
    val churchId = UUID.fromString(createChurch())
    val unknownId = UUID.randomUUID()

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, unknownId))
        .andExpect(status().isNotFound)
}

@Test
fun `GET by id returns 404 when category belongs to a different church`() {
    val churchOne = UUID.fromString(createChurch("Church One"))
    val churchTwo = UUID.fromString(createChurch("Church Two"))
    val id = extractId(createCategoryAndReturnBody(churchOne, "Tithes", "INCOME"))

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchTwo, id))
        .andExpect(status().isNotFound)
}
```

Add to imports if missing: `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get`.

- [ ] **Step 3: Run tests to verify failures**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest.get *' --tests='*TransactionCategoryIntegrationTest.GET by id*' --rerun-tasks
```

Expected: FAIL — service method and controller endpoint don't exist.

- [ ] **Step 4: Add `findCategoryScopedToChurch` + `get()` to service**

Add these methods inside `TransactionCategoryService` (between existing methods):

```kotlin
fun get(churchId: UUID, id: UUID): TransactionCategoryResponse =
    toResponse(findCategoryScopedToChurch(churchId, id))

private fun findCategoryScopedToChurch(churchId: UUID, id: UUID): TransactionCategory {
    val entity = repository.findById(id).orElseThrow { TransactionCategoryNotFoundException(id) }
    val domain = mapper.toDomain(entity)
    if (domain.churchId.toUuid() != churchId) {
        throw TransactionCategoryNotFoundException(id)
    }
    return domain
}
```

Add import: `com.calvintech.churchfinance.administration.domain.TransactionCategoryNotFoundException`.

- [ ] **Step 5: Add GET-by-id endpoint to controller**

Edit `TransactionCategoryController.kt`. Add the import for `GetMapping`:

```kotlin
import org.springframework.web.bind.annotation.GetMapping
```

Then add the endpoint method inside the controller class:

```kotlin
@GetMapping("/{id}")
@Operation(
    summary = "Get a transaction category by id",
    description = "Returns the transaction category identified by its UUID, scoped to the supplied church.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "Transaction category found"),
        ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
    ],
)
fun get(
    @Parameter(description = "Identifier of the church the category belongs to", required = true)
    @PathVariable churchId: UUID,
    @Parameter(description = "Identifier of the transaction category", required = true)
    @PathVariable id: UUID,
): TransactionCategoryResponse = transactionCategoryService.get(churchId, id)
```

- [ ] **Step 6: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 7: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: GET /churches/{churchId}/transaction-categories/{id}

Returns the category scoped to the church. Tenant mismatch is
indistinguishable from not-found to avoid leaking IDs across
tenants."
```

---

## Task 9: Service + Controller — `list()` + `TransactionCategoryStatusFilter` + GET list endpoint

**Files:**
- Create: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryStatusFilter.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service tests**

Add to `TransactionCategoryTest.kt`. First add a small helper:

```kotlin
private fun jpaList(vararg entities: TransactionCategoryJpaEntity) = entities.toList()
```

Then the tests:

```kotlin
@Test
fun `list returns only ACTIVE categories sorted by name when no filters supplied`() {
    val churchId = UUID.randomUUID()
    val active = buildTransactionCategoryJpaEntity(name = "Tithes", transactionType = FinancialTransactionType.INCOME)
    val inactive = buildTransactionCategoryJpaEntity(name = "Old", transactionType = FinancialTransactionType.INCOME)
        .also { it.status = TransactionCategoryStatus.INACTIVE }

    every { churchRepository.existsById(churchId) } returns true
    every { repository.findAllByChurchId(churchId) } returns jpaList(inactive, active)
    every { mapper.toDomain(active) } returns buildTransactionCategory(name = "Tithes", transactionType = FinancialTransactionType.INCOME)
    every { mapper.toDomain(inactive) } returns buildTransactionCategory(
        name = "Old",
        transactionType = FinancialTransactionType.INCOME,
    ).copy(status = TransactionCategoryStatus.INACTIVE)

    val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ACTIVE, type = null)

    assertEquals(1, result.size)
    assertEquals("Tithes", result.first().name)
}

@Test
fun `list with status=ALL returns all rows sorted by name`() {
    val churchId = UUID.randomUUID()
    val a = buildTransactionCategoryJpaEntity(name = "Z", transactionType = FinancialTransactionType.INCOME)
    val b = buildTransactionCategoryJpaEntity(name = "A", transactionType = FinancialTransactionType.EXPENSE)
        .also { it.status = TransactionCategoryStatus.INACTIVE }

    every { churchRepository.existsById(churchId) } returns true
    every { repository.findAllByChurchId(churchId) } returns jpaList(a, b)
    every { mapper.toDomain(a) } returns buildTransactionCategory(name = "Z", transactionType = FinancialTransactionType.INCOME)
    every { mapper.toDomain(b) } returns buildTransactionCategory(name = "A", transactionType = FinancialTransactionType.EXPENSE)
        .copy(status = TransactionCategoryStatus.INACTIVE)

    val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ALL, type = null)

    assertEquals(listOf("A", "Z"), result.map { it.name })
}

@Test
fun `list with status=INACTIVE returns only inactive`() {
    val churchId = UUID.randomUUID()
    val active = buildTransactionCategoryJpaEntity(name = "X", transactionType = FinancialTransactionType.INCOME)
    val inactive = buildTransactionCategoryJpaEntity(name = "Y", transactionType = FinancialTransactionType.INCOME)
        .also { it.status = TransactionCategoryStatus.INACTIVE }

    every { churchRepository.existsById(churchId) } returns true
    every { repository.findAllByChurchId(churchId) } returns jpaList(active, inactive)
    every { mapper.toDomain(active) } returns buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
    every { mapper.toDomain(inactive) } returns buildTransactionCategory(name = "Y", transactionType = FinancialTransactionType.INCOME)
        .copy(status = TransactionCategoryStatus.INACTIVE)

    val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.INACTIVE, type = null)

    assertEquals(listOf("Y"), result.map { it.name })
}

@Test
fun `list with type filter returns only matching type`() {
    val churchId = UUID.randomUUID()
    val income = buildTransactionCategoryJpaEntity(name = "X", transactionType = FinancialTransactionType.INCOME)
    val expense = buildTransactionCategoryJpaEntity(name = "Y", transactionType = FinancialTransactionType.EXPENSE)

    every { churchRepository.existsById(churchId) } returns true
    every { repository.findAllByChurchId(churchId) } returns jpaList(income, expense)
    every { mapper.toDomain(income) } returns buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
    every { mapper.toDomain(expense) } returns buildTransactionCategory(name = "Y", transactionType = FinancialTransactionType.EXPENSE)

    val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ACTIVE, type = FinancialTransactionType.INCOME)

    assertEquals(listOf("X"), result.map { it.name })
}

@Test
fun `list throws ChurchNotFoundException when church does not exist`() {
    val churchId = UUID.randomUUID()
    every { churchRepository.existsById(churchId) } returns false

    assertFailsWith<ChurchNotFoundException> {
        transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ACTIVE, type = null)
    }
}
```

Add imports: `TransactionCategoryStatus`, `TransactionCategoryStatusFilter`.

- [ ] **Step 2: Write failing integration tests**

In `TransactionCategoryIntegrationTest.kt`:

```kotlin
@Test
fun `GET list returns only ACTIVE categories by default`() {
    val churchId = UUID.fromString(createChurch())
    val activeId = extractId(createCategoryAndReturnBody(churchId, "Tithes", "INCOME"))
    val toDeactivateBody = createCategoryAndReturnBody(churchId, "Old", "INCOME")
    val toDeactivateId = extractId(toDeactivateBody)
    // We can't deactivate yet (Task 11). Insert it INACTIVE via PATCH route once it exists.
    // For now, this test will be updated in Task 11 once deactivate exists.
    // In this task, assert that the freshly created category appears in the default listing.
    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].id", hasItems(activeId, toDeactivateId)))
}

@Test
fun `GET list returns empty list when church has no categories`() {
    val churchId = UUID.fromString(createChurch())

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(0))
}

@Test
fun `GET list returns 404 when church does not exist`() {
    val unknownChurchId = UUID.randomUUID()

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", unknownChurchId))
        .andExpect(status().isNotFound)
}

@Test
fun `GET list with type=INCOME filters correctly`() {
    val churchId = UUID.fromString(createChurch())
    createCategoryAndReturnBody(churchId, "Tithes", "INCOME")
    createCategoryAndReturnBody(churchId, "Bills", "EXPENSE")

    mockMvc.perform(
        get("/api/v1/churches/{churchId}/transaction-categories", churchId)
            .param("type", "INCOME"),
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Tithes"))
}

@Test
fun `GET list sorts by name ascending`() {
    val churchId = UUID.fromString(createChurch())
    createCategoryAndReturnBody(churchId, "Zebra", "INCOME")
    createCategoryAndReturnBody(churchId, "Apple", "INCOME")
    createCategoryAndReturnBody(churchId, "Mango", "INCOME")

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$[0].name").value("Apple"))
        .andExpect(jsonPath("$[1].name").value("Mango"))
        .andExpect(jsonPath("$[2].name").value("Zebra"))
}
```

Add imports: `org.hamcrest.Matchers.hasItems`.

(The status=ALL and "deactivated excluded from default" tests come in Task 11 after deactivate exists.)

- [ ] **Step 3: Run tests to confirm failure**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest.list*' --tests='*TransactionCategoryIntegrationTest.GET list*' --rerun-tasks
```

Expected: FAIL — `list` method and endpoint don't exist; the filter enum doesn't exist.

- [ ] **Step 4: Create `TransactionCategoryStatusFilter`**

Create `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryStatusFilter.kt`:

```kotlin
package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus

enum class TransactionCategoryStatusFilter {
    ACTIVE,
    INACTIVE,
    ALL,
    ;

    fun matches(status: TransactionCategoryStatus): Boolean =
        when (this) {
            ALL -> true
            ACTIVE -> status == TransactionCategoryStatus.ACTIVE
            INACTIVE -> status == TransactionCategoryStatus.INACTIVE
        }
}
```

- [ ] **Step 5: Add `list()` to service**

In `TransactionCategoryService.kt`, add the import:

```kotlin
import com.calvintech.churchfinance.administration.api.TransactionCategoryStatusFilter
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
```

Then add the method (place it near the other public methods):

```kotlin
fun list(
    churchId: UUID,
    status: TransactionCategoryStatusFilter,
    type: FinancialTransactionType?,
): List<TransactionCategoryResponse> {
    requireChurchExists(churchId)
    return repository.findAllByChurchId(churchId)
        .map(mapper::toDomain)
        .filter { status.matches(it.status) }
        .filter { type == null || it.transactionType == type }
        .sortedBy { it.name }
        .map(::toResponse)
}
```

- [ ] **Step 6: Add GET list endpoint to controller**

In `TransactionCategoryController.kt`, add imports:

```kotlin
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import org.springframework.web.bind.annotation.RequestParam
```

Then add the endpoint method:

```kotlin
@GetMapping
@Operation(
    summary = "List transaction categories for a church",
    description = "Returns categories for the supplied church. Defaults to ACTIVE only; use `status=ALL` or `status=INACTIVE` to broaden, and `type=INCOME` or `type=EXPENSE` to narrow.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "Categories returned"),
        ApiResponse(responseCode = "404", description = "Church not found", content = []),
    ],
)
fun list(
    @Parameter(description = "Identifier of the church", required = true)
    @PathVariable churchId: UUID,
    @Parameter(description = "Status filter (default ACTIVE)")
    @RequestParam(defaultValue = "ACTIVE") status: TransactionCategoryStatusFilter,
    @Parameter(description = "Optional transaction type filter")
    @RequestParam(required = false) type: FinancialTransactionType?,
): List<TransactionCategoryResponse> = transactionCategoryService.list(churchId, status, type)
```

- [ ] **Step 7: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 8: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryStatusFilter.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: GET /churches/{churchId}/transaction-categories with filters

Adds list endpoint with status (default ACTIVE) and optional
type filter, sorted by name ascending. Service-side filtering
over a single findAllByChurchId query."
```

---

## Task 10: Service `update()` + `UpdateTransactionCategoryRequest` DTO + PUT endpoint

**Files:**
- Create: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/UpdateTransactionCategoryRequest.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service unit tests**

Add to `TransactionCategoryTest.kt`:

```kotlin
@Test
fun `update changes name and returns updated response`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "Old", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val existingDomain = buildTransactionCategory(name = "Old", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
    val saved = buildTransactionCategoryJpaEntity(id = id, name = "New", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId; it.version = 1 }
    val savedDomain = buildTransactionCategory(name = "New", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId), version = 1)

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    every {
        repository.findByChurchIdAndNameAndTransactionType(churchId, "New", FinancialTransactionType.INCOME)
    } returns null
    every { mapper.toJpaEntity(any()) } returns saved
    every { repository.save(saved) } returns saved
    every { mapper.toDomain(saved) } returns savedDomain

    val response = transactionCategoryService.update(
        churchId = churchId,
        id = id,
        req = UpdateTransactionCategoryRequest(name = "New", version = 0),
    )

    assertEquals("New", response.name)
}

@Test
fun `update skips uniqueness check when name is unchanged`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "Same", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val existingDomain = buildTransactionCategory(name = "Same", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    every { mapper.toJpaEntity(any()) } returns existing
    every { repository.save(existing) } returns existing
    every { mapper.toDomain(existing) } returns existingDomain

    transactionCategoryService.update(
        churchId = churchId,
        id = id,
        req = UpdateTransactionCategoryRequest(name = "Same", version = 0),
    )

    verify(exactly = 0) {
        repository.findByChurchIdAndNameAndTransactionType(any(), any(), any())
    }
}

@Test
fun `update throws conflict when new name collides with another category of the same type`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val collidingId = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "Old", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val existingDomain = buildTransactionCategory(name = "Old", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
    val colliding = buildTransactionCategoryJpaEntity(id = collidingId, name = "Taken", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    every {
        repository.findByChurchIdAndNameAndTransactionType(churchId, "Taken", FinancialTransactionType.INCOME)
    } returns colliding

    assertFailsWith<TransactionCategoryNameConflictException> {
        transactionCategoryService.update(
            churchId = churchId,
            id = id,
            req = UpdateTransactionCategoryRequest(name = "Taken", version = 0),
        )
    }
}

@Test
fun `update permits keeping the same name (excludingId guard)`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "Old", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val existingDomain = buildTransactionCategory(name = "Old", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    // findBy is NOT called because name is unchanged; covered by the "skips uniqueness check" test.
    every { mapper.toJpaEntity(any()) } returns existing
    every { repository.save(existing) } returns existing
    every { mapper.toDomain(existing) } returns existingDomain

    val response = transactionCategoryService.update(
        churchId = churchId,
        id = id,
        req = UpdateTransactionCategoryRequest(name = "Old", version = 0),
    )

    assertEquals("Old", response.name)
}

@Test
fun `update throws not-found on tenant mismatch`() {
    val urlChurchId = UUID.randomUUID()
    val actualChurchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = actualChurchId }
    val existingDomain = buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain

    assertFailsWith<TransactionCategoryNotFoundException> {
        transactionCategoryService.update(
            churchId = urlChurchId,
            id = id,
            req = UpdateTransactionCategoryRequest(name = "Y", version = 0),
        )
    }
}
```

Imports: `UpdateTransactionCategoryRequest`, `io.mockk.verify`.

- [ ] **Step 2: Write failing integration tests**

In `TransactionCategoryIntegrationTest.kt`:

```kotlin
@Test
fun `PUT updates name and returns 200`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "New", "version": 0}"""),
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.name").value("New"))
}

@Test
fun `PUT returns 400 on blank name`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "", "version": 0}"""),
    )
        .andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.message").exists())
}

@Test
fun `PUT returns 404 on unknown id`() {
    val churchId = UUID.fromString(createChurch())
    val unknownId = UUID.randomUUID()

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, unknownId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Anything", "version": 0}"""),
    )
        .andExpect(status().isNotFound)
}

@Test
fun `PUT returns 404 on tenant mismatch`() {
    val churchOne = UUID.fromString(createChurch("Church One"))
    val churchTwo = UUID.fromString(createChurch("Church Two"))
    val id = extractId(createCategoryAndReturnBody(churchOne, "Tithes", "INCOME"))

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchTwo, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Renamed", "version": 0}"""),
    )
        .andExpect(status().isNotFound)
}

@Test
fun `PUT returns 409 when stale version`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "First Update", "version": 0}"""),
    ).andExpect(status().isOk)

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Stale Update", "version": 0}"""),
    )
        .andExpect(status().isConflict)
        .andExpect(jsonPath("$.message").value("Someone else has made a change. Please refresh and try again."))
}

@Test
fun `PUT returns 409 when new name collides with another category of the same type`() {
    val churchId = UUID.fromString(createChurch())
    createCategoryAndReturnBody(churchId, "Taken", "INCOME")
    val id = extractId(createCategoryAndReturnBody(churchId, "Other", "INCOME"))

    mockMvc.perform(
        put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Taken", "version": 0}"""),
    )
        .andExpect(status().isConflict)
        .andExpect(jsonPath("$.message").exists())
}
```

Add imports: `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`.

- [ ] **Step 3: Run tests to verify failure**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest.update*' --tests='*TransactionCategoryIntegrationTest.PUT*' --rerun-tasks
```

Expected: FAIL — no `update` method, no DTO, no PUT endpoint.

- [ ] **Step 4: Create `UpdateTransactionCategoryRequest`**

Create `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/UpdateTransactionCategoryRequest.kt`:

```kotlin
package com.calvintech.churchfinance.administration.api

import jakarta.validation.constraints.NotBlank

data class UpdateTransactionCategoryRequest(
    @field:NotBlank
    val name: String,
    val version: Long,
)
```

- [ ] **Step 5: Add `update()` to service**

In `TransactionCategoryService.kt`, add the import:

```kotlin
import com.calvintech.churchfinance.administration.api.UpdateTransactionCategoryRequest
```

Add the method (after `get`):

```kotlin
fun update(
    churchId: UUID,
    id: UUID,
    req: UpdateTransactionCategoryRequest,
): TransactionCategoryResponse {
    val existing = findCategoryScopedToChurch(churchId, id)
    if (existing.name != req.name) {
        requireNameAvailable(
            churchId = churchId,
            name = req.name,
            type = existing.transactionType,
            excludingId = id,
        )
    }
    val updated = existing.copy(name = req.name, version = req.version)
    return saveAndRespond(updated)
}
```

- [ ] **Step 6: Add PUT endpoint to controller**

In `TransactionCategoryController.kt`, add imports:

```kotlin
import com.calvintech.churchfinance.administration.api.UpdateTransactionCategoryRequest
import org.springframework.web.bind.annotation.PutMapping
```

Add the endpoint:

```kotlin
@PutMapping("/{id}")
@Operation(
    summary = "Update a transaction category",
    description = "Updates the category's name. Type is immutable. Requires the current `version` for optimistic locking.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "Transaction category updated"),
        ApiResponse(responseCode = "400", description = "Validation failure", content = []),
        ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
        ApiResponse(responseCode = "409", description = "Name conflict or optimistic locking conflict", content = []),
    ],
)
fun update(
    @Parameter(description = "Identifier of the church", required = true)
    @PathVariable churchId: UUID,
    @Parameter(description = "Identifier of the transaction category", required = true)
    @PathVariable id: UUID,
    @Valid @RequestBody request: UpdateTransactionCategoryRequest,
): TransactionCategoryResponse = transactionCategoryService.update(churchId, id, request)
```

- [ ] **Step 7: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 8: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/UpdateTransactionCategoryRequest.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: PUT /churches/{churchId}/transaction-categories/{id}

Updates the category name (type is immutable). Pre-emptive
uniqueness check only when the name actually changes;
optimistic locking via version field; tenant-scoped 404."
```

---

## Task 11: Service `deactivate()` / `activate()` + PATCH endpoints

**Files:**
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt`
- Modify: `church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt`
- Modify: `church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt`

- [ ] **Step 1: Write failing service unit tests**

Add to `TransactionCategoryTest.kt`:

```kotlin
@Test
fun `deactivate sets status to INACTIVE`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId }
    val existingDomain = buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
    val deactivated = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId; it.status = TransactionCategoryStatus.INACTIVE; it.version = 1 }
    val deactivatedDomain = existingDomain.copy(status = TransactionCategoryStatus.INACTIVE, version = 1)

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    every { mapper.toJpaEntity(any()) } returns deactivated
    every { repository.save(deactivated) } returns deactivated
    every { mapper.toDomain(deactivated) } returns deactivatedDomain

    val response = transactionCategoryService.deactivate(churchId, id, version = 0)

    assertEquals(TransactionCategoryStatus.INACTIVE, response.status)
}

@Test
fun `activate sets status to ACTIVE`() {
    val churchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId; it.status = TransactionCategoryStatus.INACTIVE }
    val existingDomain = buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(churchId), status = TransactionCategoryStatus.INACTIVE)
    val activated = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = churchId; it.version = 2 }
    val activatedDomain = existingDomain.copy(status = TransactionCategoryStatus.ACTIVE, version = 2)

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain
    every { mapper.toJpaEntity(any()) } returns activated
    every { repository.save(activated) } returns activated
    every { mapper.toDomain(activated) } returns activatedDomain

    val response = transactionCategoryService.activate(churchId, id, version = 1)

    assertEquals(TransactionCategoryStatus.ACTIVE, response.status)
}

@Test
fun `deactivate throws not-found on tenant mismatch`() {
    val urlChurchId = UUID.randomUUID()
    val actualChurchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = actualChurchId }
    val existingDomain = buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain

    assertFailsWith<TransactionCategoryNotFoundException> {
        transactionCategoryService.deactivate(urlChurchId, id, version = 0)
    }
}

@Test
fun `activate throws not-found on tenant mismatch`() {
    val urlChurchId = UUID.randomUUID()
    val actualChurchId = UUID.randomUUID()
    val id = UUID.randomUUID()
    val existing = buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
        .also { it.churchId = actualChurchId }
    val existingDomain = buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

    every { repository.findById(id) } returns java.util.Optional.of(existing)
    every { mapper.toDomain(existing) } returns existingDomain

    assertFailsWith<TransactionCategoryNotFoundException> {
        transactionCategoryService.activate(urlChurchId, id, version = 0)
    }
}
```

- [ ] **Step 2: Write failing integration tests**

In `TransactionCategoryIntegrationTest.kt`:

```kotlin
@Test
fun `PATCH deactivate sets status to INACTIVE`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "X", "INCOME"))

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, id)
            .param("version", "0"),
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.status").value("INACTIVE"))
}

@Test
fun `PATCH activate sets status to ACTIVE`() {
    val churchId = UUID.fromString(createChurch())
    val id = extractId(createCategoryAndReturnBody(churchId, "X", "INCOME"))

    val deactivateBody = mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, id)
            .param("version", "0"),
    ).andExpect(status().isOk).andReturn().response.contentAsString

    val newVersion = objectMapper.readTree(deactivateBody).get("version").asString()

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchId, id)
            .param("version", newVersion),
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.status").value("ACTIVE"))
}

@Test
fun `PATCH deactivate returns 404 on unknown id`() {
    val churchId = UUID.fromString(createChurch())
    val unknownId = UUID.randomUUID()

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, unknownId)
            .param("version", "0"),
    ).andExpect(status().isNotFound)
}

@Test
fun `PATCH deactivate returns 404 on tenant mismatch`() {
    val churchOne = UUID.fromString(createChurch("Church One"))
    val churchTwo = UUID.fromString(createChurch("Church Two"))
    val id = extractId(createCategoryAndReturnBody(churchOne, "X", "INCOME"))

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchTwo, id)
            .param("version", "0"),
    ).andExpect(status().isNotFound)
}

@Test
fun `PATCH activate returns 404 on unknown id`() {
    val churchId = UUID.fromString(createChurch())
    val unknownId = UUID.randomUUID()

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchId, unknownId)
            .param("version", "0"),
    ).andExpect(status().isNotFound)
}

@Test
fun `PATCH activate returns 404 on tenant mismatch`() {
    val churchOne = UUID.fromString(createChurch("Church One"))
    val churchTwo = UUID.fromString(createChurch("Church Two"))
    val id = extractId(createCategoryAndReturnBody(churchOne, "X", "INCOME"))

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchTwo, id)
            .param("version", "0"),
    ).andExpect(status().isNotFound)
}

@Test
fun `Deactivated category is excluded from default list but included with status=ALL`() {
    val churchId = UUID.fromString(createChurch())
    val activeId = extractId(createCategoryAndReturnBody(churchId, "Active", "INCOME"))
    val toDeactivateId = extractId(createCategoryAndReturnBody(churchId, "Hidden", "INCOME"))

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, toDeactivateId)
            .param("version", "0"),
    ).andExpect(status().isOk)

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(activeId))

    mockMvc.perform(
        get("/api/v1/churches/{churchId}/transaction-categories", churchId)
            .param("status", "ALL"),
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(2))
}
```

Add imports: `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch`.

- [ ] **Step 3: Run tests to verify failure**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest.deactivate*' --tests='*TransactionCategoryTest.activate*' --tests='*TransactionCategoryIntegrationTest.PATCH*' --tests='*TransactionCategoryIntegrationTest.Deactivated*' --rerun-tasks
```

Expected: FAIL — methods and endpoints don't exist.

- [ ] **Step 4: Add `deactivate()` and `activate()` to service**

Add to `TransactionCategoryService.kt`:

```kotlin
fun deactivate(churchId: UUID, id: UUID, version: Long): TransactionCategoryResponse {
    val existing = findCategoryScopedToChurch(churchId, id)
    val updated = existing.copy(status = TransactionCategoryStatus.INACTIVE, version = version)
    return saveAndRespond(updated)
}

fun activate(churchId: UUID, id: UUID, version: Long): TransactionCategoryResponse {
    val existing = findCategoryScopedToChurch(churchId, id)
    val updated = existing.copy(status = TransactionCategoryStatus.ACTIVE, version = version)
    return saveAndRespond(updated)
}
```

Add import: `com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus`.

- [ ] **Step 5: Add PATCH endpoints to controller**

In `TransactionCategoryController.kt`, add import:

```kotlin
import org.springframework.web.bind.annotation.PatchMapping
```

Add the two endpoints:

```kotlin
@PatchMapping("/{id}/deactivate")
@Operation(
    summary = "Deactivate a transaction category",
    description = "Marks the category as INACTIVE. Requires the current `version` for optimistic locking.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "Category deactivated"),
        ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
        ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
    ],
)
fun deactivate(
    @Parameter(description = "Identifier of the church", required = true)
    @PathVariable churchId: UUID,
    @Parameter(description = "Identifier of the transaction category", required = true)
    @PathVariable id: UUID,
    @Parameter(description = "Current version for optimistic locking", required = true)
    @RequestParam version: Long,
): TransactionCategoryResponse = transactionCategoryService.deactivate(churchId, id, version)

@PatchMapping("/{id}/activate")
@Operation(
    summary = "Activate a transaction category",
    description = "Marks the category as ACTIVE. Requires the current `version` for optimistic locking.",
)
@ApiResponses(
    value = [
        ApiResponse(responseCode = "200", description = "Category activated"),
        ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
        ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
    ],
)
fun activate(
    @Parameter(description = "Identifier of the church", required = true)
    @PathVariable churchId: UUID,
    @Parameter(description = "Identifier of the transaction category", required = true)
    @PathVariable id: UUID,
    @Parameter(description = "Current version for optimistic locking", required = true)
    @RequestParam version: Long,
): TransactionCategoryResponse = transactionCategoryService.activate(churchId, id, version)
```

- [ ] **Step 6: Update the placeholder integration test from Task 9**

The Task 9 test `GET list returns only ACTIVE categories by default` contains a comment about updating it once deactivate exists. Find that test and update it to actually deactivate the second category, so it asserts that only the active one appears:

```kotlin
@Test
fun `GET list returns only ACTIVE categories by default`() {
    val churchId = UUID.fromString(createChurch())
    val activeId = extractId(createCategoryAndReturnBody(churchId, "Tithes", "INCOME"))
    val toDeactivateId = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

    mockMvc.perform(
        patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, toDeactivateId)
            .param("version", "0"),
    ).andExpect(status().isOk)

    mockMvc.perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(activeId))
}
```

(There's overlap with `Deactivated category is excluded from default list...` — that's fine, they're checking slightly different angles. If you find the duplication too much, delete this one and keep the new one.)

- [ ] **Step 7: Run tests**

```bash
./gradlew :church-finance-backend:test --tests='*TransactionCategoryTest*' --tests='*TransactionCategoryIntegrationTest*' --rerun-tasks
```

Expected: all PASS.

- [ ] **Step 8: Stage and commit (Calvin runs)**

```bash
git add church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryService.kt \
        church-finance-backend/src/main/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryController.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/service/TransactionCategoryTest.kt \
        church-finance-backend/src/test/kotlin/com/calvintech/churchfinance/administration/api/TransactionCategoryIntegrationTest.kt
git commit -m "feat: PATCH activate/deactivate for transaction categories

Soft-deactivation via PATCH ../{id}/deactivate and ../{id}/activate
with optimistic-lock version. Tenant-scoped 404 on both. Default
list endpoint now excludes INACTIVE categories."
```

---

## Task 12: Final full-suite sweep (ktlint + tests)

**Files:** None modified — this task confirms the whole build is green and clean.

- [ ] **Step 1: Run the formatter**

```bash
./gradlew :church-finance-backend:ktlintFormat
```

Expected: SUCCESS. If anything was reformatted, the diff should be minimal (whitespace / import ordering).

- [ ] **Step 2: Run the full check**

```bash
./gradlew :church-finance-backend:ktlintCheck :church-finance-backend:test --rerun-tasks
```

Expected: BUILD SUCCESSFUL with all tests passing. The test count should reflect every test added across Tasks 1–11 (~30 new tests).

- [ ] **Step 3: Eyeball the OpenAPI surface (optional, manual)**

If you want a sanity check on the docs:

```bash
./gradlew :church-finance-backend:bootRun --args='--spring.profiles.active=local'
```

Then visit `http://localhost:8080/swagger-ui.html` and confirm:
- All 6 transaction-category endpoints render
- The `status` field appears on responses
- `?status=` and `?type=` query params are documented on the list endpoint
- 404 / 409 responses are documented where appropriate

Stop the server when done (`Ctrl-C`).

- [ ] **Step 4: Stage any formatter changes and commit (Calvin runs)**

If `ktlintFormat` made changes:

```bash
git status                                 # confirm what changed
git add <list of files reformatted>
git commit -m "chore: ktlint format pass after CRUD completion"
```

If `ktlintFormat` made no changes, skip this step.

- [ ] **Step 5: Ready for PR**

Branch is now ready for a pull request to `main`. CodeRabbit will run on the PR (`.coderabbit.yaml` in the repo root).

---

## Self-review

### Spec coverage

Walked each section of the spec against the plan:

- **Goals:**
  - GET-by-id, list, PUT, PATCH activate/deactivate — Tasks 8, 9, 10, 11
  - `status` field — Task 2
  - Unique constraint swap — Task 1
  - Church-not-found 500→404 fix — Task 6
  - Church-aggregate-pattern parity — distributed across Tasks 2–11
- **API surface table** — covered Task by Task
- **Domain & persistence changes** — Tasks 2, 4, 5
- **Service-layer logic** — Tasks 6, 7, 8, 9, 10, 11
- **Error handling table** — Task 4
- **Testing approach** — every test case from the spec maps to a task (persistence: Task 1 + Task 5; service unit: Tasks 6, 7, 8, 9, 10, 11; integration: Tasks 3, 6, 7, 8, 9, 10, 11)
- **Implementation ordering** — plan order matches spec order

Spec's "Open items":
- Update existing create-201 integration test to assert on `status` — done in Task 3 ✓
- `OpenApiConfig` changes — none needed; annotations on new controller methods only ✓

### Placeholder scan

No TBD/TODO in the plan. One task (Task 9) deliberately defers part of a test to Task 11 (the "deactivated category excluded from default list" assertion), with that hand-off explicitly described in both tasks. That's an intentional cross-task dependency, not a placeholder.

### Type consistency

Cross-task type / method names checked:
- `TransactionCategoryStatus` (enum, ACTIVE/INACTIVE) — defined Task 2; referenced Tasks 2, 3, 9, 11
- `TransactionCategoryStatusFilter` (enum, ACTIVE/INACTIVE/ALL with `matches`) — defined Task 9; referenced Tasks 9
- `TransactionCategoryNotFoundException(id: UUID)` — defined Task 4; referenced Tasks 4, 8, 10, 11
- `TransactionCategoryNameConflictException(churchId: UUID, name: String, type: FinancialTransactionType)` — defined Task 4; referenced Tasks 4, 7, 10
- `UpdateTransactionCategoryRequest(name: String, version: Long)` — defined Task 10; referenced Task 10
- Service signatures (`get(churchId, id)`, `list(churchId, status, type)`, `update(churchId, id, req)`, `deactivate(churchId, id, version)`, `activate(churchId, id, version)`) — consistent across tasks
- Repository method signatures (`findByChurchIdAndNameAndTransactionType`, `findAllByChurchId`) — consistent

All consistent.
