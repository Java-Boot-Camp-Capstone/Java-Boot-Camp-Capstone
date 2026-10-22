# Northstar CRM - Customer Management Platform (Capstone)

Service agents search customers and record interactions.
**Angular** UI → **Spring Boot** REST API (JWT login) → **PostgreSQL**, with **Kafka** for events.
Everything runs in **Docker** - you do **not** need Java, Maven, Node or Angular installed to run it.

---

## Run the app (3 steps)

### Windows
Open a terminal in the project folder (IntelliJ/VS Code: **Terminal → New Terminal**, or PowerShell) and run:

| Step | Command | What happens |
|---|---|---|
| 1. Install (once) | `.\scripts\windows\install.cmd` | Installs Git + Docker Desktop if missing, starts Docker, builds the app (first time ~5 min) |
| 2. Start | `.\scripts\windows\start.cmd` | Starts everything, runs a quick check, opens **http://localhost:4200**, then returns to the prompt |
| 3. Stop | `.\scripts\windows\stop.cmd` | Stops everything (data kept). `.\scripts\windows\stop.cmd -Reset` also wipes the database |

The scripts finish on their own - the app keeps running in Docker in the background until you run **stop**.
(VS Code *Code Runner* users: if it ever says "Code is already running", press **Ctrl+Alt+M** to stop it.)

### macOS / Linux
```bash
cp .env.example .env                  # once
docker compose up -d --build --wait   # start  -> http://localhost:4200
docker compose down                   # stop   (add -v to wipe the database)
```

### Log in
| Username | Password | Role |
|---|---|---|
| `agent` | `agent-local-dev` | Service agent |
| `manager` | `manager-local-dev` | Manager |

These are local demo passwords from `.env` (created automatically from `.env.example`; never committed).

**Demo journey:** log in → Customers (CUS-1001 Amina Khan) → Interactions → New → record a call.

---

## What is running

| Container | Port | Purpose |
|---|---|---|
| `frontend` | http://localhost:4200 | Angular app (served by nginx, which also forwards `/api` to the backend) |
| `backend` | http://localhost:8080 | Spring Boot REST API - health: http://localhost:8080/actuator/health |
| `postgres` | 5432 | Database (tables created automatically by Flyway) |
| `kafka` | 9092 | Event messaging |

## Troubleshooting

| Problem | Fix |
|---|---|
| "Docker is not running" | Open **Docker Desktop**, wait for *Engine running*, run start again |
| Port 4200/8080/5432 already in use | Stop the other program (or another copy of this app), run start again |
| Start says a container is unhealthy | `docker compose logs backend` shows why |
| Want a clean database | `.\scripts\windows\stop.cmd -Reset` (or `docker compose down -v`), then start |
| Pulled new code | Just run start again - it rebuilds what changed |

---

## GitHub Actions (CI/CD)

| Workflow | When it runs | What it does | Time |
|---|---|---|---|
| **CI** | Every push / pull request, or *Actions → CI → Run workflow* | Backend tests + frontend tests in parallel → Docker images built from the tested code → security scan → push images (main) | ~1.5-2 min* |
| **Deploy to k3s** | *Actions → Deploy to k3s → Run workflow*, or a `v*` tag | Deploys the tested images by digest, smoke-tests, rolls back on failure | ~1-2 min |
| **CodeQL** | Pull requests, main/Development, weekly | Static code security scan → Security tab (kept separate so CI stays fast) | ~3-4 min |

\* The first run of each week also downloads the security-scanner database (adds ~1-2 min).
Before a presentation, run CI once so the caches are warm.

Details: [docs/documentation/docker-and-ci-runbook.md](docs/documentation/docker-and-ci-runbook.md) ·
k3s setup: [docs/documentation/k3s-deployment.md](docs/documentation/k3s-deployment.md) ·
Security: [docs/documentation/security-baseline.md](docs/documentation/security-baseline.md)

## Project layout

```
backend/     Spring Boot API (Java 21, Maven)       frontend/   Angular app
k8s/         k3s deployment manifests               scripts/    install / start / stop, deploy, smoke test
docs/        architecture, plans, runbooks          defense/    presentation material
.github/     CI, Deploy, CodeQL workflows           docker-compose.yml  local full stack
```

## Working on the code (optional)
Running the app needs only Docker. To edit and run code in IntelliJ/VS Code you also need JDK 21 and Node 22
(Windows: `scripts\windows\install.cmd -DevTools`). Backend tests: `cd backend && mvn verify` ·
Frontend: `cd frontend && npm ci && npm start` (the dev server forwards `/api` to `localhost:8080`).
