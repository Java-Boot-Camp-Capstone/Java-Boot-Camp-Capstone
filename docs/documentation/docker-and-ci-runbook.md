# Docker & GitHub Actions Runbook

Owner: Brandon (DevOps / CI-CD) · Supports Lab 51 (Release) and the defense "release story".
Related: [security-baseline.md](security-baseline.md) · [k3s-deployment.md](k3s-deployment.md)

## Repository layout (merge requirement)

All branches must use **root-level** `backend/` and `frontend/` (brief §5). Ben's draft lived under
`capstone/lab49-crm/backend` and `capstone/frontend`; it was moved here as-is (plus the fixes listed below).
CI's `detect` job looks for `backend/pom.xml` and `frontend/package.json` - other layouts are silently skipped.

## Files

| File | Purpose |
|---|---|
| `backend/Dockerfile` | Multi-stage Maven/JDK 21 → JRE 21 Alpine, `apk upgrade`, non-root UID 10001, HEALTHCHECK on readiness |
| `frontend/Dockerfile` | Multi-stage Node 22 `ng build` → nginx-unprivileged 1.30 (UID 101, port 8080) |
| `frontend/nginx.conf.template` | SPA fallback, `/api/` proxy to `${BACKEND_URL}` (forwards real host/proto), `/healthz`, CSP + security headers |
| `frontend/proxy.conf.json` | `ng serve` proxies `/api` → `localhost:8080` (same relative URLs as Docker/k3s) |
| `docker-compose.yml` | Local full stack: PostgreSQL 16, Kafka 3.8 (KRaft), backend, frontend - refuses to start without secrets |
| `.env.example` | Synthetic local values; copy to `.env` (git-ignored) |
| `.trivyignore.yaml` | Time-boxed, owned scan exceptions (expire → gate blocks again) |
| `.github/workflows/ci.yml` | CI/CD pipeline (below) |
| `.github/workflows/demo.yml` | One-click demo from the Actions tab (optional public URL) |
| `k8s/base/` | k3s manifests (Kustomize) |
| `scripts/smoke-test.sh` | 14-check smoke test (happy path + 401/400 negatives) - local, CI, k3s, demo |
| `scripts/k3s-deploy.sh` | Digest-pinned deploy + smoke + auto-rollback |

## Run locally

```bash
cp .env.example .env            # synthetic values; never commit .env
docker compose up --build -d --wait
AGENT_PASSWORD=agent-local-dev ./scripts/smoke-test.sh
```

| URL | What |
|---|---|
| http://localhost:4200 | Angular UI - log in as `agent` / `agent-local-dev` |
| http://localhost:4200/api/... | API through nginx (same origin) |
| http://localhost:8080/actuator/health | Backend health (only health is exposed) |

Stop: `docker compose down` (add `-v` to wipe the database volume).

**IntelliJ (backend without Docker):** Run config → Environment variables:
`SPRING_DATASOURCE_PASSWORD=crm-local-dev;APP_JWT_SECRET=local-dev-only-change-me-0123456789abcdef;APP_DEMO_AGENT_PASSWORD=agent-local-dev;APP_DEMO_MANAGER_PASSWORD=manager-local-dev`
(start Postgres/Kafka with `docker compose up -d postgres kafka`). **Frontend:** `cd frontend && npm start` (proxy handles `/api`).

## Pipeline (`ci.yml`)

| Job | Runs on | Gate |
|---|---|---|
| Detect components | every run | Skips jobs cleanly if `backend/` or `frontend/` is missing |
| Secret scan | every run | gitleaks over full Git history - **blocks** |
| Backend - Maven verify | push / PR | `mvn -B verify` incl. `AuthorizationTests` (401/403/200) - **blocks** |
| Frontend - Angular test + build | push / PR | `npm ci`, `ng test` (headless), `ng build --configuration production` - **blocks** |
| Dependency scan | push / PR | Trivy on `package-lock.json`, HIGH+ - **blocks** |
| Dependency review | PR only | New HIGH+ vulnerable deps in the PR diff - **blocks** |
| CodeQL SAST | push / PR | Java + TypeScript → Security tab |
| Images | after all gates | Build once → Trivy image scan (CRITICAL / Java libs HIGH - **blocks**) → compose e2e smoke → ZAP DAST (report) → push to GHCR (main/Development or manual) → release manifest with digests |
| Deploy to k3s | manual (or auto on Development) | GitHub Environment approval → digest deploy → smoke → auto-rollback |

All actions are pinned to commit SHAs (Node 24 runtimes) and every job has least-privilege `permissions:`.

### GITHUB_TOKEN permissions (why the settings page is greyed out)
The org controls the default token permission, so the repo setting is locked. That's fine - the workflow
declares what it needs per job: `contents: read` globally, `packages: write` only on `images` (GHCR push),
`security-events: write` only on `codeql`, `pull-requests: write` only on dependency review. The first GHCR push
requires the org to allow package creation (Org → Settings → Packages); ask the org owner if push fails with `denied`.

## Run from the Actions tab (presentation-day backup)

| Goal | How |
|---|---|
| Re-run the full pipeline on any branch | Actions → **CI** → *Run workflow* → pick branch (tick *publish* to push images) |
| Run the whole app without a laptop | Actions → **Demo - run full CRM stack** → *Run workflow* |
| …and share it in a browser | same, tick **public_url** + pick minutes → open the URL shown in the run summary / annotation |

Public URL mode needs repo secrets `DEMO_AGENT_PASSWORD` and `DEMO_MANAGER_PASSWORD` (Settings → Secrets and variables
→ Actions). Synthetic data only; JWT still enforced; tunnel closes when the job ends or is cancelled.

## Evidence for the defense

- Actions run URL (green run + one deliberately failed run that was fixed through code)
- Run summary: smoke-test table + release manifest (git SHA, image digests, run URL)
- Artifact `release-evidence` (Trivy reports, ZAP report, release-manifest.json), `backend-test-reports`, `frontend-coverage`, `security-*`
- Security tab → Code scanning alerts (triaged)
- k3s deploy summary (previous vs new digest, rollback command)

## Known gaps / residual risks / In Progress

| Item | Owner | Note |
|---|---|---|
| Kafka events never sent - `InteractionEventPublisher` picks its no-arg constructor | Backend team | Bootstrap address is fixed; add `@Autowired` on the KafkaTemplate constructor |
| Interactions stored in memory, not PostgreSQL | Backend team | Replace `InteractionRepository` map with JPA |
| CUS-9999 → 500 instead of 404 | Backend team | Map `UnknownCustomerException` to 404 |
| Angular 19 / spring-webmvc CVEs (time-boxed exceptions, expire 2026-11-30) | Frontend / Backend | See security-baseline.md |
| k3s cluster access (kubeconfig, network) | Brandon + instructor | See k3s-deployment.md |
