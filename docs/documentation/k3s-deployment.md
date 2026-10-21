# k3s Deployment

`deploy.yml` deploys the images CI already built, tested and scanned - pinned by digest, never rebuilt -
to the k3s cluster, then smoke-tests and rolls back automatically if anything fails.
Manifests: `k8s/base/` · script: `scripts/k3s-deploy.sh` · environments: [environment-strategy.md](environment-strategy.md)

## One-time setup (repo admin)

**1. GitHub Environments** - Settings → Environments → create `dev`, `test`, `staging`, `production`.

| Environment | k3s namespace | Suggested protection |
|---|---|---|
| `dev` | `crm-dev` | none |
| `test` | `crm-test` | none |
| `staging` | `crm-stage` | 1 required reviewer |
| `production` | `crm-prod` | required reviewers, `main` only |

(Set an environment variable `K8S_NAMESPACE` to use a different namespace.)

**2. Secrets on each environment** (names only - values never go in the repo):

| Secret | Value |
|---|---|
| `KUBECONFIG_B64` | the cluster's kubeconfig, base64-encoded (step 3) |
| `POSTGRES_PASSWORD` | database password for that environment |
| `APP_JWT_SECRET` | 32+ random characters (`openssl rand -base64 48`) |
| `APP_DEMO_AGENT_PASSWORD`, `APP_DEMO_MANAGER_PASSWORD` | demo logins |
| `GHCR_PULL_TOKEN` (optional) | token with `read:packages`, only if the container images are private |

**3. kubeconfig → secret.** On the k3s server the file is `/etc/rancher/k3s/k3s.yaml`.
Change `127.0.0.1` to the server's address, then (Git Bash / macOS / Linux):
```bash
base64 -w0 k3s.yaml        # macOS: base64 -i k3s.yaml
```
Paste the output into `KUBECONFIG_B64`, then delete the local copy. Never commit a kubeconfig (`.gitignore` blocks it).

**4. Network.** GitHub's runners are on the internet, so the k3s API (port 6443) must be reachable from there.
If the lab cluster is private, the instructor needs to provide a self-hosted runner inside that network.

**5. Image access.** Make the two GHCR packages public (org → Packages → package → Settings), or set `GHCR_PULL_TOKEN`.

## Deploy
- **Manual:** Actions → **Deploy to k3s** → *Run workflow* → environment → image tag `main` (or `sha-<commit>`).
- **Release:** push a tag like `v1.0.0` → deploys that commit's images to `staging`.

The job: checks cluster access → creates the `crm-secrets` Secret → applies `k8s/base` with both images pinned
`@sha256:` (refuses floating tags) → waits for PostgreSQL, Kafka, backend, frontend → runs the smoke test
(login works, data loads, requests without a token get 401) → on any failure, `kubectl rollout undo`.

## Manual rollback
```bash
kubectl -n crm-dev rollout undo deploy/crm-backend
kubectl -n crm-dev rollout status deploy/crm-backend
```

## Tested
Rehearsed against a local k3s v1.34 cluster: deploy → all pods ready, smoke test 14/14; deploying a deliberately
broken backend image → detected, old pod kept serving, automatic rollback to the previous digest.
