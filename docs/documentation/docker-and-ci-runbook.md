# Docker & GitHub Actions Runbook

Owner: Brandon (DevOps / CI-CD) · Supports Lab 51 (Release) and the defense "release story".

## Files

| File | Purpose |
|---|---|
| `backend/Dockerfile` | Multi-stage: Maven + JDK 21 build → JRE 21 Alpine runtime, non-root user |
| `frontend/Dockerfile` | Multi-stage: Node 22 `ng build` → unprivileged nginx (port 8080) |
| `frontend/nginx.conf.template` | SPA fallback, `/api/` proxy to `${BACKEND_URL}`, `/healthz` probe, security headers |
| `docker-compose.yml` | Local full stack: PostgreSQL 16, Kafka 3.8 (KRaft), backend, frontend |
| `.env.example` | Synthetic local values; copy to `.env` (git-ignored) |
| `.github/workflows/ci.yml` | CI/CD pipeline (below) |

## Run locally

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps            # postgres + kafka should be (healthy)
```

| URL | What |
|---|---|
| http://localhost:4200 | Angular UI |
| http://localhost:4200/api/... | API through nginx (same origin) |
| http://localhost:8080/api/... | API direct |
| http://localhost:4200/healthz | Frontend liveness |

Smoke test (CAP-12, correlation `lab-request-001`):

```bash
curl -s localhost:4200/api/customers
curl -s -X POST localhost:4200/api/v1/interactions \
  -H 'Content-Type: application/json' -H 'X-Correlation-Id: lab-request-001' \
  -d '{"customerId":"CUS-1001","interactionType":"CALL","summary":"smoke","correlationId":"lab-request-001"}'
```

Stop: `docker compose down` (add `-v` to wipe the database volume).

Compose overrides `application.yaml` using Spring env vars (`SPRING_DATASOURCE_*`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`), so no secrets are baked into images.

## Pipeline (`ci.yml`)

| Job | Runs on | Gate |
|---|---|---|
| Detect components | every run | Skips jobs cleanly if `backend/` or `frontend/` is not on the branch yet |
| Backend - Maven verify | push / PR | `mvn -B verify` against a PostgreSQL service container; uploads surefire reports |
| Frontend - Angular test + build | push / PR | `npm ci`, `ng test` (headless Chrome + coverage), `ng build --configuration production` |
| CodeQL SAST | push / PR | Java + TypeScript `security-and-quality` queries → Security tab |
| Docker image | after tests pass | Builds both images, Trivy scan, records digest in job summary + artifact |

Images are **pushed to GHCR only from `main` / `Development`** (or a manual run):
`ghcr.io/java-boot-camp-capstone/northstar-crm-backend` and `...-frontend`, tagged `sha-<commit>` and branch name.
Deployment must reference the recorded **digest** (`image@sha256:...`), never rebuild ("package once").

## Evidence for the defense

- Actions run URL (green run + one deliberately failed run that was fixed through code)
- `image-evidence-*` artifacts (digest + Trivy report)
- `backend-test-reports`, `frontend-coverage` artifacts
- Security tab → Code scanning alerts (triaged)

## Known gaps / residual risks / In Progress

| Item | Owner | Note |
|---|---|---|
| Trivy is report-only | Brandon | Set `exit-code: "1"` to make HIGH/CRITICAL a hard gate |
| No Actuator in backend `pom.xml` | Backend team | Add `spring-boot-starter-actuator` for `/actuator/health` probes |
| k3s deploy job not yet added | Brandon | Needs cluster + `KUBECONFIG` secret in a protected `staging` environment |
