Run ktlint check on the project. If there are failures, run ktlint format to auto-fix them. If any issues remain after formatting (cannot be auto-corrected), report them and fix manually.

```bash
./gradlew ktlintCheck
```

If that fails:

```bash
./gradlew ktlintFormat
```

Then re-run ktlintCheck to confirm all issues are resolved.