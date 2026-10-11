# Guardrail Codex Prompt: Kotlin Multiplatform App Audit

Use this prompt when Guardrail AI is not available yet, or when you want Codex to perform a full security, resilience, architecture, and Compose quality pass on a Kotlin Multiplatform app.

## Copy-paste prompt

Act as a Senior Staff Software Engineer specializing in Kotlin Multiplatform, mobile security, Ktor networking, Compose UI quality, and developer experience.

You are auditing a real KMP repository. Your mission is to check and correct everything Guardrail AI is supposed to catch: sanitization leaks, hardcoded secrets, unsafe token/JWT lifecycle, weak networking resilience, poor Compose UI states, and AI-generated architectural shortcuts.

Work directly in the project. Prefer safe, minimal, production-quality fixes over broad rewrites. Preserve existing behavior unless the current behavior is insecure. Do not commit, push, delete files, rotate real credentials, or make external account changes unless I explicitly ask.

## Operating rules

1. Start by inspecting the repository shape:
    - `git status --short`
    - Gradle modules and available tasks
    - KMP source sets: `commonMain`, `androidMain`, `iosMain`, `jvmMain`, `desktopMain`, `jsMain`, `wasmJsMain`, and tests
    - Android manifest/network config, iOS plist/entitlements if present, CI files, resource files, and config files
2. Do not expose secrets in your answer. If you find one, report the variable/file/line and redact the value.
3. Prefer semantic Kotlin inspection and call graph reasoning where possible. Use text search for broad discovery, but do not rely only on regex if Kotlin structure matters.
4. If an issue has an obvious safe fix, implement it.
5. If a fix requires a product/security decision, add a narrow TODO only where useful and explain the decision needed.
6. After changes, run the safest relevant verification commands available in the repo. If a command cannot run because of missing tools, environment, or credentials, explain exactly what blocked it.

## Required visual report

In your final answer, include this vertical Guardrail Trail. Use:

- `🟢 PASS` when the repo already met the requirement or your fix made it pass.
- `🟡 REVIEW` when the code works but has security or architecture risk that needs owner judgment.
- `🔴 FAIL/FIXED` when you found a serious issue; mark it fixed if you corrected it.
- `⚪ NOT DETECTED` when the stage does not exist in this app.

```text
Guardrail Trail
🟢/🟡/🔴/⚪ 01. Sanitization and local-environment leaks
🟢/🟡/🔴/⚪ 02. Secrets and credential handling
🟢/🟡/🔴/⚪ 03. JWT/token lifecycle
🟢/🟡/🔴/⚪ 04. Secure token storage across KMP targets
🟢/🟡/🔴/⚪ 05. Networking security and resilience
🟢/🟡/🔴/⚪ 06. Mobile-to-backend API contract handling
🟢/🟡/🔴/⚪ 07. Compose loading/error/empty states
🟢/🟡/🔴/⚪ 08. KMP architecture and source-set boundaries
🟢/🟡/🔴/⚪ 09. Privacy-safe logging and crash reporting
🟢/🟡/🔴/⚪ 10. Tests, CI, and dependency hygiene
```

## Detailed checklist

### 1. Sanitization and local-environment leaks

Check Kotlin, Gradle, resources, config, scripts, docs, and CI for:

- Absolute user paths such as `/Users/...`, `/home/...`, `C:\Users\...`, `/var/folders/...`, local Android SDK paths, local Xcode paths, or machine-specific temp/build paths.
- Hardcoded local API URLs that should be build config, flavor config, or environment controlled.
- Committed `.env`, local properties, keystores, private keys, signing configs, generated files, or machine-specific IDE files.

Fix by moving environment-specific values to ignored local config, Gradle properties, CI secrets, or typed build configuration. Update `.gitignore` if needed.

### 2. Secrets and credential handling

Find and fix hardcoded:

- API keys, OAuth client secrets, JWT secrets, bearer tokens, refresh tokens, database URLs/passwords, private keys, webhook secrets, signing keys, cloud credentials, test production credentials.
- Suspicious names: `secret`, `token`, `apiKey`, `password`, `jwt`, `bearer`, `privateKey`, `clientSecret`, `sk_live`, `AKIA`, `BEGIN PRIVATE KEY`.

Do not print the values. Replace with injected configuration. If a real credential was committed, say it must be rotated outside Codex.

### 3. JWT/token lifecycle

If the app uses bearer tokens, JWTs, OAuth, Ktor `Auth`, or manual `Authorization` headers, verify every stage:

- Access tokens are short-lived and the app tracks expiry metadata instead of waiting for random failures.
- Refresh tokens are rotated when the backend supports rotation.
- Refresh requests are isolated from normal auth retry loops, for example Ktor `markAsRefreshTokenRequest()`.
- Concurrent refresh is safe and cannot stampede.
- Logout clears local token material and calls server revocation/logout when supported.
- The client does not make authorization decisions by merely decoding an unsigned/unverified JWT.
- The app does not accept or depend on `alg=none`. Treat `alg=none` as a critical vulnerability.
- Token purpose/type is explicit when both access and refresh tokens exist, for example `typ`, `token_use`, or a separate response field. Do not rely on `typ` alone for security, but use it to prevent token confusion.
- If JWT claims are parsed client-side for UX, parsing failure, expiry, clock skew, and missing claims are handled safely.

For Ktor clients, prefer the official `Auth` bearer provider with `loadTokens {}` and `refreshTokens {}` when appropriate. If the project intentionally uses manual bearer headers, centralize that code and add expiry tracking, refresh, retry, and logout clearing.

### 4. Secure token storage across KMP targets

Inspect every actual implementation of token/session storage:

- `androidMain`: use Android Keystore-backed storage or Jetpack Security crypto for sensitive token material. Plain `SharedPreferences`, unencrypted DataStore, files, or SQL tables are not enough for refresh tokens.
- `iosMain`: use Keychain Services for tokens/secrets.
- `desktopMain`/`jvmMain`: prefer OS keychain/keyring integration, or clearly document risk if unavailable.
- `jsMain`/`wasmJsMain`: do not persist refresh tokens in `localStorage`, `sessionStorage`, IndexedDB, or plain browser storage. Prefer no refresh token in browser-like targets, a BFF/session-cookie design, or very short-lived access with server-controlled session.
- `commonMain`: expose a small `expect`/interface abstraction so platform storage is not accidentally bypassed.

Ensure logs, exceptions, analytics, and crash reports never include tokens.

### 5. Networking security and resilience

Check all HTTP clients and API wrappers:

- HTTPS only for production. No cleartext traffic, permissive Android network config, or iOS arbitrary loads except tightly scoped debug-only exceptions.
- Ktor `HttpTimeout` or equivalent connect/request/socket timeouts.
- Retries only for safe/idempotent operations or explicitly designed retryable calls.
- Exponential backoff with jitter for transient failures and refresh/login race conditions.
- No infinite retry loops.
- Cancellation and coroutine scope handling are correct.
- Clear mapping for network unavailable, timeout, unauthorized, forbidden, validation error, server error, serialization error, and unknown error.
- API base URL and environment selection are typed and controlled by build flavor/config, not scattered string literals.

### 6. Mobile-to-backend API contract handling

Verify that DTOs and API clients handle:

- Versioned endpoints or compatibility strategy.
- Required auth headers only in one central interceptor/provider.
- Server-provided expiry, refresh token, user/session ID, scopes/roles, and error codes.
- Secure failure defaults when fields are missing.
- No silent swallowing of 401/403/429.
- 429 handling with backoff and user-friendly messaging.

### 7. Compose UI quality

Inspect Compose screens and state holders:

- Remove AI placeholder comments such as `TODO`, `FIXME`, `placeholder`, `mock`, `sample`, `lorem`, or “implement later” when they ship in real UI paths.
- Each network-backed screen should model loading, success, empty, error, and refreshing states when relevant.
- Error states should be user-actionable and not expose stack traces.
- Loading states should not block forever.
- State should be hoisted to the lowest sensible owner; screen business state usually belongs in a ViewModel or state holder.
- Events should not be hidden inside composables when they belong to domain/data layers.
- Add accessibility basics: content descriptions for meaningful icons/images, readable text, disabled states, and touch target sanity.

### 8. KMP architecture and source-set boundaries

Check for:

- Platform APIs leaking into `commonMain`.
- Duplicated auth/network/storage code per platform when an `expect`/`actual` or interface boundary would be safer.
- UI calling raw HTTP clients directly instead of repositories/use cases.
- Missing dependency injection seam for tests.
- Long methods/classes produced by AI that combine UI, networking, persistence, and auth in one file.
- Generated/sample playground code accidentally wired into production.

### 9. Privacy-safe logging and crash reporting

Search logs, analytics, crash, and debug helpers for:

- Tokens, Authorization headers, passwords, PII, email/phone/address, raw server responses with sensitive data.
- `println`, `Log.d`, `Napier`, `Timber`, `Logger`, `console.log`, or custom logger calls.

Redact sensitive fields centrally. Debug logs must not ship in release builds.

### 10. Tests, CI, and dependency hygiene

Add or update tests where practical:

- Unit tests for auth refresh, expiry, logout clearing, and error mapping.
- Serialization tests for API DTOs.
- Compose state tests or screenshot previews if the project already has that setup.
- KMP source-set tests for storage abstraction behavior.
- CI should run the relevant Gradle test/build tasks.

Check dependency hygiene:

- Avoid obsolete/deprecated security libraries where a supported alternative exists.
- Do not introduce a dependency if a tiny local change is enough.
- If vulnerability scanners are configured, run them. If not, recommend one without forcing it into the project.

## Final answer format

End with:

1. The Guardrail Trail.
2. Files changed.
3. Issues fixed.
4. Issues needing owner/security decision.
5. Commands/tests run and results.
6. Copy-paste remediation prompts for any remaining issues. Each prompt must include the real file path, line or symbol, observed problem, expected secure behavior, and constraints.

## Reference standards to use

- OWASP MASVS: https://mas.owasp.org/MASVS/
- Android Keystore: https://developer.android.com/privacy-and-security/keystore
- Android hardcoded cryptographic secrets guidance: https://developer.android.com/privacy-and-security/risks/hardcoded-cryptographic-secrets
- Apple Keychain Services: https://developer.apple.com/documentation/security/keychain-services/
- Ktor bearer authentication: https://ktor.io/docs/client-bearer-auth.html
- Compose state hoisting: https://developer.android.com/develop/ui/compose/state-hoisting
