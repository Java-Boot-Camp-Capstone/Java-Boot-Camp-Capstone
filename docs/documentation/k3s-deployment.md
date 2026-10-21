# k3s Deployment (kubeconfig path)

The `deploy` job in `.github/workflows/ci.yml` deploys the **exact images CI built and scanned** to k3s, pinned
by digest, then smoke-tests and rolls back automatically if anything fails. Manifests: `k8s/base/`.
Script (also runnable from a laptop): `scripts/k3s-deploy.sh`.

## One-time setup (repo admin / instructor)

### 1. Create GitHub Environments
Settings → Environments → **New environment**: `dev`, `staging`, `production`.

| Environment | Namespace (default) | Protection rule (Module 51) |
|---|---|---|
| `dev` | `crm-dev` | none (or 1 reviewer) |
| `staging` | `crm-stage` | 1 required reviewer |
| `production` | `crm-prod` | 2 required reviewers + 5-min wait timer, branch = `main` |

Override the namespace with an environment **variable** `K8S_NAMESPACE` if the instructor assigned a different one.

### 2. Add environment secrets (names only - never commit values)

| Secret | What |
|---|---|
| `KUBECONFIG_B64` | base64 of the k3s kubeconfig (see below) |
| `POSTGRES_PASSWORD` | DB password for that environment |
| `APP_JWT_SECRET` | ≥ 32 random chars (`openssl rand -base64 48`) |
| `APP_DEMO_AGENT_PASSWORD` / `APP_DEMO_MANAGER_PASSWORD` | demo logins |
| `GHCR_PULL_TOKEN` *(optional)* | PAT with `read:packages` - only if the GHCR packages stay private |

Use **different values per environment** (Module 51: secrets never cross the staging/production boundary).

### 3. Get the kubeconfig into a secret
On the k3s server the file is `/etc/rancher/k3s/k3s.yaml`. Replace `127.0.0.1` with the server's reachable address, then:

```bash
# Git Bash / Linux / macOS
base64 -w0 k3s.yaml > kubeconfig.b64        # macOS: base64 -i k3s.yaml
# paste the contents into the KUBECONFIG_B64 secret, then delete both local copies
```

Prefer a **namespace-scoped ServiceAccount kubeconfig** over the cluster-admin `k3s.yaml` (least privilege).
Never commit a kubeconfig (`.gitignore` blocks `kubeconfig*`).

### 4. Network reachability
GitHub-hosted runners run on the public internet. If the k3s API (port 6443) is **not** publicly reachable
(typical for a lab), register a self-hosted runner inside that network and set repo variable `K3S_RUNNER=self-hosted`.

### 5. Image pull access
Images are pushed to `ghcr.io/java-boot-camp-capstone/northstar-crm-{backend,frontend}`. Either make the two
packages **public** (org → Packages → package → Settings → visibility) or set `GHCR_PULL_TOKEN`.

## Running a deploy

- **Manual (presentation day):** Actions → **CI** → *Run workflow* → branch `Development` → tick **deploy** → choose environment.
- **Automatic:** set repo variable `K3S_AUTO_DEPLOY=true` → every push to `Development` deploys to `dev`.

What the job does (see run log + summary):
1. Pre-flight: kubectl/server version, current context, nodes, `auth can-i`
2. Applies `crm-secrets` from environment secrets (values masked)
3. Renders `k8s/base` with the digests and **refuses to apply** unless both images are pinned `@sha256:`
4. `rollout status` for Postgres, Kafka, backend, frontend (new pod must be Ready before old one stops)
5. Smoke test through `kubectl port-forward` (14 checks incl. 401 negatives)
6. On any failure → `kubectl rollout undo` to the previous digest, job fails red

## Manual rollback

```bash
kubectl -n crm-dev rollout history deploy/crm-backend
kubectl -n crm-dev rollout undo deploy/crm-backend          # or --to-revision=N
kubectl -n crm-dev rollout status deploy/crm-backend
UI_URL=... API_URL=... AGENT_PASSWORD=... ./scripts/smoke-test.sh
```

## Rehearsal evidence (local k3s v1.34.12 in Docker, 2026-10-21)

| Step | Result |
|---|---|
| Deploy good digests | all pods Ready, smoke **14/14 PASS** |
| Deploy deliberately broken backend digest | `CrashLoopBackOff` detected by rollout timeout; old pod kept serving (`maxUnavailable: 0`) |
| Auto-rollback | `deployment.apps/crm-backend rolled back` → previous digest restored and Ready |

Bugs that rehearsal caught and that are fixed here: base kustomization renaming images (digest pin never matched → `:latest`
pulled), and the backend image using a named user that `runAsNonRoot` cannot verify (now numeric UID 10001).
