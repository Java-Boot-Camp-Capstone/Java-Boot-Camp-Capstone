# Security Baseline (Capstone security requirement)

Owner: Brandon (DevOps / security gates) · Supports Lab 51 rubric "Security / Prod awareness" and brief §8.4 rule 6
("security findings are hard gates unless residual risk is accepted with owner and date").

Sources: Module 40 (Application Security Testing), Module 41 (Containerization), Module 43/44 (GitHub Actions, CD),
Module 49 (JWT/RBAC review), Module 51 (Security, CI/CD and Deployment).

## 1. Controls implemented

| # | Control | Where | Lecture rule it satisfies |
|---|---|---|---|
| 1 | **JWT resource server, deny-by-default** - anonymous `/api/**` → 401, wrong role → 403, AGENT/MANAGER → 200, anything unlisted → denied | `backend/.../security/SecurityConfig.java` | M49/M51 "Open endpoints fail the course" |
| 2 | Short-lived HS256 tokens (30 min), issuer + expiry validated, key ≥ 32 chars from env only (app refuses to start without it) | `SecurityConfig`, `JwtTokenService` | M51 secrets, M43 "never hardcode" |
| 3 | Login endpoint: BCrypt-hashed demo users, same error for bad user/bad password, password never logged, CR/LF stripped from logged usernames (CWE-117) | `AuthController`, `LoginRequest` | M40 secure coding |
| 4 | Angular is **not** the security boundary - UI only stores/forwards the token; 401 → logout | `frontend/src/app/auth/*` | Brief §2.3 |
| 5 | Actuator exposes **health only** (`/actuator/env`, `/beans`, `/heapdump` → denied); no stack traces/messages in error bodies | `application.yaml` | M51 DAST "actuator accidentally public" |
| 6 | No secrets in Git: passwords/JWT key come from `.env` (git-ignored) locally, GitHub Environment secrets in CI, k3s Secret in cluster | `.gitignore`, `docker-compose.yml`, `k8s/`, `scripts/k3s-deploy.sh` | M41 "password ENV in Dockerfile is an honor violation" |
| 7 | **Secret scan** of full Git history (gitleaks) - blocks the pipeline | `ci.yml` → `secret-scan` | M51 gate order |
| 8 | **Dependency scan** - npm lockfile and Java libraries inside the shipped jar, block at HIGH | `ci.yml` → `dependency-scan`, `images` | M40/M51 "libraries fail at High" |
| 9 | **SAST** - CodeQL `security-and-quality` for Java + TypeScript | `ci.yml` → `codeql` | M51 SAST |
| 10 | **Image scan before push** - block at CRITICAL | `ci.yml` → `images` | M51 "image scan → fail at Critical, scan BEFORE push" |
| 11 | **DAST** - OWASP ZAP baseline against the running stack (report artifact) + smoke negatives (401/400) | `ci.yml` → `images`, `scripts/smoke-test.sh` | M40/M51 DAST |
| 12 | Containers: multi-stage, pinned base tags, **non-root numeric UID** (10001 / 101), read-only root FS (backend), all capabilities dropped, no SA token, seccomp RuntimeDefault | `*/Dockerfile`, `k8s/base/*.yaml` | M41/M51 Dockerfile discipline |
| 13 | nginx: CSP, X-Frame-Options DENY, nosniff, Referrer-Policy, Permissions-Policy, version hidden | `frontend/nginx.conf.template` | ZAP findings |
| 14 | Deploy only **by digest**, deploy refuses floating tags, auto-rollback on failed rollout/smoke | `scripts/k3s-deploy.sh` | M51 "Immutable or it didn't ship" |
| 15 | Least-privilege `GITHUB_TOKEN` (read-only default; `packages: write` / `security-events: write` only on the jobs that need them); actions pinned to commit SHAs | `.github/workflows/*.yml` | M43 "Pin everything / least privilege" |
| 16 | Dependency upgrades: Spring Boot 3.3.5 (EOL, 9 CRITICAL) → 3.5.16 + Tomcat/Jackson/pgjdbc patch overrides; nginx 1.27 → 1.30 | `backend/pom.xml`, `frontend/Dockerfile` | M51 "fix, or owned exception" |

## 2. Demo accounts (synthetic)

| Username | Role | Password source |
|---|---|---|
| `agent` | AGENT | `APP_DEMO_AGENT_PASSWORD` (.env / GitHub secret / k3s Secret) |
| `manager` | MANAGER | `APP_DEMO_MANAGER_PASSWORD` |

Training-only token endpoint. In an enterprise deployment the corporate IdP (OIDC) issues tokens and the API only validates them.

## 3. Evidence (where reviewers look)

| Evidence | Location |
|---|---|
| 401 / 403 / 200 tests (10 cases) | `backend/src/test/.../security/AuthorizationTests.java`, Actions artifact `backend-test-reports` |
| Smoke incl. negative paths | Run summary "Smoke test" table (CI + k3s deploy) |
| Secret / dependency / image scan reports | Artifacts `security-gitleaks`, `security-dependency-scan`, `release-evidence/reports/` |
| SAST | Repo → Security → Code scanning |
| DAST | Artifact `release-evidence/zap/zap-report.html` |
| Digest identity | Run summary "Release manifest" + `release-evidence/reports/release-manifest.json` |

## 4. Residual risks (owned, dated)

| Risk | Severity | Owner | Due | Mitigating control |
|---|---|---|---|---|
| spring-webmvc 6.2.x CVE-2026-47884/47890 - fix only in Spring 7 (Boot 4) | CRITICAL | Backend team | 2026-11-30 | Deny-by-default JWT, training cluster only; time-boxed in `.trivyignore.yaml` |
| Angular 19.2 - 8 HIGH CVEs, fixes only in Angular 20.3+ | HIGH | Frontend team | 2026-11-30 | No SSR/hydration, CSP; time-boxed in `.trivyignore.yaml` |
| lz4-java 1.10.1 (transitive via kafka-clients) | HIGH | Backend team | 2026-11-30 | Kafka internal only |
| CodeQL alerts annotate but do not fail the job by themselves | Medium | Brandon | Before merge to main | Add branch-protection rule "Code scanning results: block on High" |
| ZAP baseline is report-only (warnings remain: cache headers, COEP) | Low | Brandon | 2026-11-30 | Review report; promote to blocking after triage |
| Static demo credentials / HS256 shared key (no IdP) | Medium | Backend team | Post-capstone | Secrets per environment, 30-min tokens; replace with OIDC IdP |
| No login rate limiting / lockout | Medium | Backend team | Post-capstone | Synthetic data, training cluster |
| `InteractionEventPublisher` uses its no-arg constructor → Kafka events are never sent | Functional | Backend team | Before demo | Add `@Autowired` to the KafkaTemplate constructor |
| Interactions stored in memory, not PostgreSQL (`InteractionRepository`) | Functional | Backend team | Before demo | Replace with JPA repository (table already migrated) |
| CUS-9999 returns 500 instead of 404 | Low | Backend team | Before demo | Add `@ResponseStatus(NOT_FOUND)` / `@ExceptionHandler` |

Exceptions in `.trivyignore.yaml` **expire** on their date - after that the pipeline blocks again until fixed or re-approved.
