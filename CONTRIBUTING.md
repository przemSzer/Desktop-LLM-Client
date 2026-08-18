# Contributing

## Build and test

You need JDK 26. From the repo root:

```bash
./mvnw clean test
```

Run the UI:

```bash
./mvnw javafx:run -pl ui
```

## Code style

- Java 26, Maven multi-module (`core`, `ui`)
- Tests: JUnit 5. Mockito in BDD style (`given` / `then`, `@Captor`)
- Prefer clear names over comments
- Keep UI (JavaFX) out of `core`

## Pull requests

- Keep changes focused; describe why, not only what
- Include tests for behaviour changes in `core` and ViewModels
- Do not commit secrets, `~/.dlc` files, `.cursor/`, `.run/`, or profiler dumps

## Future hardening (not required for a first PR)

- Stronger API-key storage (OS keychain / encryption)
- Safer defaults for local command / MCP tools
