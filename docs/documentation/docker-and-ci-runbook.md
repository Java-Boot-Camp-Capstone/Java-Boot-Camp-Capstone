# Docker & GitHub Actions - how it works

Owner: Brandon (Docker / CI-CD). To just **run** the app, see the [README](../../README.md).

Delivery path (from [architecture.md](architecture.md)):
**GitHub → GitHub Actions → tests and security checks → Docker image → k3s deployment**

## Docker

| File | What it does |
|---|---|
| `backend/Dockerfile` | Stage 1 compiles the jar with Maven (JDK 21). Stage 2 runs it on a small Java 21 image as a non-root user. |
| `frontend/Dockerfile` | Stage 1 builds Angular (Node 22). Stage 2 serves it with nginx as a non-root user. |
| `frontend/nginx.conf.template` | Serves the Angular app, forwards `/api/*` to the backend, adds security headers, `/healthz` for health checks. |
| `docker-compose.yml` | Runs the whole architecture locally: PostgreSQL, Kafka, backend, frontend. Waits for each to be healthy. |
| `.env.example` → `.env` | Local demo passwords. `.env` is git-ignored; the start scripts create it automatically. |

Passwords and the JWT signing key are **never** in the code or images: Compose reads them from `.env`,
GitHub Actions from repository/environment secrets, k3s from a Kubernetes Secret.

## CI workflow (`.github/workflows/ci.yml`) - target under 2 minutes

```
            ┌─ backend job:  mvn verify ─> image from that jar  ─> Trivy scan ─> push (main) ─┐
 push / PR ─┤                                                                                  ├─> release: tag + record digests (main)
            └─ frontend job: ng test + ng build ─> image from that build ─> Trivy scan ─> push ┘
```

| Why it is fast | How |
|---|---|
| Backend and frontend run **in parallel** | two independent jobs |
| Code is built **once** | CI uses the jar/dist it just tested as the image contents (`build-contexts`) instead of compiling again inside Docker |
| Dependencies are cached | Maven cache, `node_modules` cache, Docker layer cache |
| Scanner database cached | Trivy's database (~3 GB) is downloaded once a week, then reused - scans take seconds |
| Slow scan kept separate | CodeQL runs in its own workflow (`codeql.yml`), not in the timed CI run |

**Security checks that block CI:** Trivy image scan fails on any CRITICAL vulnerability, and on HIGH ones in the
backend's Java libraries. Pull requests also run GitHub dependency review (blocks new HIGH-risk dependencies).
Accepted findings live in `.trivyignore.yaml`, each with an owner and an expiry date.

**Images:** `ghcr.io/java-boot-camp-capstone/northstar-crm-backend` and `…-frontend`, tagged `sha-<commit>`;
after both jobs pass, the `release` job adds the branch tag (same digest) and writes a release manifest
(commit, run URL, both digests) to the run summary.

**GITHUB_TOKEN:** the organisation locks the repo's "Workflow permissions" setting (greyed out), so the workflow
declares permissions itself: read-only by default, `packages: write` only on jobs that push images,
`security-events: write` only for CodeQL.

**Run it by hand (presentation day):** Actions → **CI** → *Run workflow* → pick the branch. Tick *publish* to push images.

## Deploy workflow (`.github/workflows/deploy.yml`)
Actions → **Deploy to k3s** → *Run workflow* → environment + image tag (`main` = latest tested main build).
It looks up the image digests, deploys them to k3s, waits for the pods, runs `scripts/smoke-test.sh`,
and rolls back automatically if anything fails. Setup (kubeconfig, secrets): [k3s-deployment.md](k3s-deployment.md).

## Evidence for the presentation
- CI run page: green jobs, run time, release manifest (image digests) in the summary
- Artifacts: `backend-test-reports`, `release-manifest`
- Deploy run summary: smoke-test table, previous vs new image, rollback command
- Security tab → Code scanning (CodeQL)

## Known gaps (owned by other sections)
| Item | Owner |
|---|---|
| Kafka events never sent - `InteractionEventPublisher` is created with its no-arg constructor, so it has no KafkaTemplate (broker address itself is fixed) | Backend |
| Interactions are kept in memory, not saved to PostgreSQL (`InteractionRepository`) | Backend |
| Unknown customer (CUS-9999) returns 500 instead of 404 | Backend |
| Angular 19 / spring-webmvc vulnerabilities need major upgrades - accepted until 2026-11-30 | Frontend / Backend |
