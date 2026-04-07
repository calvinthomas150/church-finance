# Church Finance — API Integration Specification

This document is the canonical reference for clients integrating with the Church
Finance HTTP API. It describes conventions that apply across all endpoints; the
machine-readable contract for individual operations lives in the OpenAPI
specification served at runtime.

## 1. Discovery

| Resource         | URL                  |
| ---------------- | -------------------- |
| OpenAPI document | `/v3/api-docs`       |
| Swagger UI       | `/swagger-ui.html`   |

The OpenAPI document is generated from the Spring controllers and is the source
of truth for request/response schemas, status codes, and validation rules. This
markdown document is intentionally narrative and explains conventions, not
individual endpoints.

## 2. Versioning

All endpoints are namespaced under `/api/v{n}` (currently `/api/v1`). Breaking
changes require a new major version path. Backwards-compatible additions
(new endpoints, new optional fields) may be made within an existing version.

## 3. Grouping & Tagging Convention

The API is organised in two layers:

1. **Groups (per aggregate)** — top-level switcher in Swagger UI. Each DDD
   aggregate that exposes endpoints is registered as a `GroupedOpenApi` bean
   in `shared/api/OpenApiConfig.kt`. Selecting a group in the Swagger UI
   "Select a definition" dropdown narrows the view to that aggregate, and the
   group is also available as a standalone OpenAPI document at
   `/v3/api-docs/<group>` for client codegen.
2. **Tags (per resource within an aggregate)** — flat resource names like
   `Churches`, `Transaction Categories`. Because the group already conveys the
   aggregate, tags do not repeat the aggregate name as a prefix.

A special `all` group exposes every operation across every aggregate and is
useful for browsing the full surface area in one place.

Current groups:

| Group            | Aggregate          | Tags inside the group                |
| ---------------- | ------------------ | ------------------------------------ |
| `all`            | (everything)       | (all tags)                           |
| `administration` | `administration`   | `Churches`, `Transaction Categories` |

When a new aggregate exposes its first endpoint:

1. Add a `GroupedOpenApi` bean for it in `OpenApiConfig.kt` (one line, matched
   by package pattern).
2. Tag controllers within that aggregate with flat resource names — do not
   include the aggregate name in the tag.
3. Update the table above.

## 4. Identifiers

- All public identifiers in API payloads are `UUID` (string, RFC 4122 form).
- Internally the domain uses ULIDs; the persistence and API boundaries convert
  between ULID and UUID. Clients should treat ids as opaque strings.

## 5. Multi-Tenancy

Most resources are scoped to a church. Where a resource lives under a parent
church, the church id appears in the URL path. The `User` aggregate is the
only resource that is not church-scoped.

## 6. Optimistic Locking

Mutable aggregates expose a `version: long` field on every read response.
Update and state-transition endpoints require this version, either:

- as a field in the request body (PUT updates), or
- as a `version` query parameter (PATCH state transitions such as
  activate/deactivate).

If the supplied version does not match the current persisted version, the
server responds with `409 Conflict`. Clients should re-read the resource and
retry.

## 7. HTTP Conventions

| Method   | Usage                                                              |
| -------- | ------------------------------------------------------------------ |
| `GET`    | Retrieve a resource. Safe and idempotent.                          |
| `POST`   | Create a new resource. Returns `201 Created` with the new entity.  |
| `PUT`    | Full replacement of mutable fields. Idempotent.                    |
| `PATCH`  | State transitions and partial updates (e.g. activate/deactivate).  |
| `DELETE` | Reserved — most domain resources are deactivated, not deleted.     |

### Status codes

| Code  | Meaning                                                      |
| ----- | ------------------------------------------------------------ |
| `200` | Successful read/update                                       |
| `201` | Successful create                                            |
| `400` | Request body or parameters failed validation                 |
| `404` | Resource not found                                           |
| `409` | Optimistic locking conflict or domain invariant violation    |
| `500` | Unhandled server error                                       |

## 8. Validation

Request bodies are validated with Jakarta Bean Validation. Strings declared as
required must be non-blank (`@NotBlank`) — empty strings and whitespace-only
strings are rejected. Validation failures produce `400 Bad Request`.

## 9. Money & Numeric Types

All monetary amounts are serialised as JSON numbers backed by `BigDecimal` on
the server. Clients **must** preserve precision (do not round-trip through
floating point) and **must** compare amounts using a decimal-aware comparison.

## 10. Authentication

To be defined. The API will adopt a bearer-token scheme; this section will be
expanded once the auth aggregate is wired into the controllers.

## 11. Adding New Endpoints

When introducing a new controller:

1. Place it under `<aggregate>/api/` following the layered structure.
2. If this is the first endpoint in the aggregate, add a `GroupedOpenApi`
   bean for it in `shared/api/OpenApiConfig.kt`.
3. Annotate the class with `@Tag(name = "<Resource>", description = "…")` —
   flat resource name, no aggregate prefix.
4. Annotate each handler with `@Operation(summary, description)` and an
   `@ApiResponses` block enumerating the expected status codes.
5. Annotate path/query parameters with `@Parameter` so the generated docs are
   self-explanatory.
6. If the operation participates in optimistic locking, document the
   `version` requirement in the operation description.
7. Update this specification if a new convention is introduced or a new
   group/tag is added to the table in §3.
