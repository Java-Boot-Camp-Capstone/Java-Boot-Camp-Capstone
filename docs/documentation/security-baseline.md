# Security

Scope: the security base for the capstone (API access control, secrets, CI security checks, containers).

## What is in place

| Area | Control | Where |
|---|---|---|
| API access | Every `/api/**` call needs a valid JWT. No token → **401**, wrong role → **403**, `agent`/`manager` → allowed. Endpoints not explicitly listed are denied. | `backend/.../security/SecurityConfig.java` |
| Login | `POST /api/auth/login` issues a 30-minute token. Passwords are BCrypt-hashed, never logged; same error for wrong user or wrong password. | `AuthController`, `JwtTokenService` |
| Angular | Only stores the token and sends it on each request - the API enforces security, not the UI. Expired token → back to login. | `frontend/src/app/auth/` |
| Health endpoint | Only `/actuator/health` is exposed; no stack traces in error responses. | `application.yaml` |
| Secrets | No passwords or keys in Git or images: `.env` (git-ignored) locally, GitHub secrets in CI, Kubernetes Secret in k3s. The app refuses to start without them. | `.env.example`, `docker-compose.yml`, `scripts/k3s-deploy.sh` |
| CI security checks | Trivy image scan (blocks CRITICAL; HIGH in Java libraries), dependency review on pull requests, CodeQL code scanning. | `.github/workflows/ci.yml`, `codeql.yml` |
| Containers | Multi-stage builds, non-root users, no secrets baked in; in k3s also read-only filesystem (backend), no extra Linux capabilities. | `*/Dockerfile`, `k8s/base/` |
| Web headers | Content-Security-Policy, X-Frame-Options, nosniff, Referrer-Policy; nginx version hidden. | `frontend/nginx.conf.template` |
| Deployments | Only digest-pinned images that passed CI; automatic rollback on failure. | `deploy.yml`, `scripts/k3s-deploy.sh` |
| Dependencies | Spring Boot 3.3.5 (end-of-life, 9 critical CVEs) upgraded to 3.5.16 with patched Tomcat / Jackson / PostgreSQL driver. | `backend/pom.xml` |

## Proof (tests)
- `AuthorizationTests` (10 tests): 401 without token, 403 wrong role, 200/201 with agent token, forged/tampered token rejected, bad password rejected.
- `scripts/smoke-test.sh` (14 checks) runs after every k3s deploy - includes the 401 checks against the live app.

## Demo accounts (synthetic)
`agent` (AGENT) and `manager` (MANAGER); passwords come from `APP_DEMO_AGENT_PASSWORD` / `APP_DEMO_MANAGER_PASSWORD`.
This simple login endpoint is for the training environment; a real deployment would use the company's identity provider.

## Accepted risks (owner + date)

| Risk | Severity | Owner | Until | Why accepted |
|---|---|---|---|---|
| spring-webmvc CVE-2026-47884 / 47890 - fix only in Spring Boot 4 | Critical | Backend | 2026-11-30 | Major upgrade; API requires JWT, synthetic data only |
| Angular 19 - 7 HIGH CVEs, fixed only in Angular 20.3+ | High | Frontend | 2026-11-30 | Major upgrade (`ng update`) |
| lz4-java (comes with Kafka client) | High | Backend | 2026-11-30 | Internal Kafka only |
| No login rate limiting | Medium | Backend | after capstone | Training environment |

These are listed in `.trivyignore.yaml`; after the date, CI blocks again until fixed or re-approved.
