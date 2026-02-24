# Git Conventions

## Branches

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/short-description` | `feature/financial-transaction-domain` |
| Bug fix | `fix/short-description` | `fix/account-balance-calculation` |
| Chore | `chore/short-description` | `chore/update-dependencies` |
| Docs | `docs/short-description` | `docs/domain-model` |

Keep descriptions short, lowercase, hyphen-separated.

---

## Commit Messages

Follows [Conventional Commits](https://www.conventionalcommits.org/).

```
<type>: <short description>
```

- Lowercase, no full stop
- Present tense — describe what the commit *does*, not what you *did*
- Keep the description concise

### Types

| Type | When to use |
|------|-------------|
| `feat` | New functionality |
| `fix` | Bug fix |
| `chore` | Maintenance, dependencies, tooling |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `refactor` | Code change that isn't a fix or feature |

### Examples

```
feat: add financial transaction domain model
fix: correct account balance calculation
chore: upgrade spring boot to 4.0.3
docs: update domain model aggregates
test: add unit tests for gift aid submission
refactor: extract transaction categorisation logic
```

---

## Pull Requests

- All changes merged to main via PR, no direct commits to main
- PR title follows conventional commit format
- Description should cover **what** and **why**, not how
- CI checks must pass before merging

---