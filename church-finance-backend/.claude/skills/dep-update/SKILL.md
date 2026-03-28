---
name: dep-update
description: Check for dependency updates and bump versions in build.gradle.kts
disable-model-invocation: true
---

# Dependency Update

Check for newer dependency versions and optionally bump them.

## Steps

1. Run the dependency updates check:
```bash
./gradlew dependencyUpdates
```

2. Review the output and present a summary table of available updates to the user, separating:
   - **Plugin updates** (declared in `plugins {}` block)
   - **Explicit dependency updates** (dependencies with hardcoded versions in `build.gradle.kts`)
   - **BOM-managed updates** (versions controlled by Spring's `io.spring.dependency-management` — these update when the Spring Boot version is bumped, not independently)

3. Ask the user which updates to apply.

4. Apply only the requested updates to `build.gradle.kts`.

5. Run `./gradlew build` to verify nothing is broken.