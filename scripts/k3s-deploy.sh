#!/usr/bin/env bash
# Deploy Northstar CRM to k3s by IMAGE DIGEST, smoke test it, and roll back automatically on failure.
#
# Required env:
#   KUBECONFIG                 path to a kubeconfig for the k3s cluster (never commit it)
#   K8S_NAMESPACE              e.g. crm-dev | crm-stage | crm-prod
#   BACKEND_IMAGE              ghcr.io/<owner>/northstar-crm-backend@sha256:...
#   FRONTEND_IMAGE             ghcr.io/<owner>/northstar-crm-frontend@sha256:...
#   POSTGRES_PASSWORD, APP_JWT_SECRET, APP_DEMO_AGENT_PASSWORD, APP_DEMO_MANAGER_PASSWORD
# Optional:
#   GHCR_PULL_USER / GHCR_PULL_TOKEN   create an imagePullSecret (needed only if GHCR packages are private)
#   ROLLOUT_TIMEOUT (default 300s)
set -euo pipefail

: "${K8S_NAMESPACE:?}" "${BACKEND_IMAGE:?}" "${FRONTEND_IMAGE:?}"
: "${POSTGRES_PASSWORD:?}" "${APP_JWT_SECRET:?}" "${APP_DEMO_AGENT_PASSWORD:?}" "${APP_DEMO_MANAGER_PASSWORD:?}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NS="$K8S_NAMESPACE"
summary() { [ -n "${GITHUB_STEP_SUMMARY:-}" ] && echo "$*" >> "$GITHUB_STEP_SUMMARY" || true; }

for img in "$BACKEND_IMAGE" "$FRONTEND_IMAGE"; do
  [[ "$img" == *@sha256:* ]] || { echo "Refusing to deploy a floating tag: $img (digest required)"; exit 1; }
done

echo "::group::Pre-flight (context, access, namespace)"
kubectl version --output=yaml | grep -E 'gitVersion' || true
echo "context: $(kubectl config current-context)"
kubectl get nodes -o wide
kubectl get namespace "$NS" >/dev/null 2>&1 || kubectl create namespace "$NS"
kubectl auth can-i create deployments -n "$NS"
echo "::endgroup::"

echo "::group::Secrets (values from GitHub Environment secrets - never printed)"
kubectl -n "$NS" create secret generic crm-secrets \
  --from-literal=POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  --from-literal=APP_JWT_SECRET="$APP_JWT_SECRET" \
  --from-literal=APP_DEMO_AGENT_PASSWORD="$APP_DEMO_AGENT_PASSWORD" \
  --from-literal=APP_DEMO_MANAGER_PASSWORD="$APP_DEMO_MANAGER_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "crm-secrets applied"
if [ -n "${GHCR_PULL_TOKEN:-}" ]; then
  kubectl -n "$NS" create secret docker-registry ghcr-pull \
    --docker-server=ghcr.io --docker-username="${GHCR_PULL_USER:-github}" --docker-password="$GHCR_PULL_TOKEN" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  kubectl -n "$NS" patch serviceaccount default -p '{"imagePullSecrets":[{"name":"ghcr-pull"}]}' >/dev/null
  echo "ghcr-pull imagePullSecret applied"
fi
echo "::endgroup::"

# Remember what is running now, so the evidence shows what a rollback returns to.
prev_backend=$(kubectl -n "$NS" get deploy crm-backend -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "none")
prev_frontend=$(kubectl -n "$NS" get deploy crm-frontend -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || echo "none")

echo "::group::Apply manifests pinned to digests"
OVERLAY="$(mktemp -d "$REPO_ROOT/k8s/.deploy-XXXXXX")"
trap 'rm -rf "$OVERLAY"' EXIT
cat > "$OVERLAY/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: ${NS}
resources:
  - ../base
images:
  - name: northstar-crm-backend
    newName: ${BACKEND_IMAGE%@*}
    digest: ${BACKEND_IMAGE#*@}
  - name: northstar-crm-frontend
    newName: ${FRONTEND_IMAGE%@*}
    digest: ${FRONTEND_IMAGE#*@}
EOF
kubectl kustomize "$OVERLAY" > "$OVERLAY/rendered.yaml"
for img in "$BACKEND_IMAGE" "$FRONTEND_IMAGE"; do
  grep -q "image: ${img}\$" "$OVERLAY/rendered.yaml" || { echo "::error::rendered manifests are not pinned to $img"; exit 1; }
done
echo "verified: rendered manifests pin both app images by digest"
kubectl apply -f "$OVERLAY/rendered.yaml"
echo "::endgroup::"

rollback() {
  echo "::error::$1 - rolling back to the previous digest"
  summary "### Rollback triggered: $1"
  for d in crm-backend crm-frontend; do
    if [ "$(kubectl -n "$NS" rollout history deploy/$d 2>/dev/null | grep -cE '^[0-9]+')" -gt 1 ]; then
      kubectl -n "$NS" rollout undo deploy/$d
      kubectl -n "$NS" rollout status deploy/$d --timeout="$ROLLOUT_TIMEOUT" || true
    else
      echo "$d has no previous revision (first deploy) - nothing to roll back to"
    fi
  done
  kubectl -n "$NS" get pods -o wide
  summary "Restored backend: \`$(kubectl -n "$NS" get deploy crm-backend -o jsonpath='{.spec.template.spec.containers[0].image}')\`"
  exit 1
}

echo "::group::Wait for rollout"
kubectl -n "$NS" rollout status statefulset/crm-postgres --timeout="$ROLLOUT_TIMEOUT"
for d in crm-kafka crm-backend crm-frontend; do
  kubectl -n "$NS" rollout status deploy/$d --timeout="$ROLLOUT_TIMEOUT" || {
    kubectl -n "$NS" get pods -o wide; kubectl -n "$NS" logs deploy/$d --tail=80 || true
    rollback "rollout of $d did not become Ready"; }
done
kubectl -n "$NS" get pods -o wide
echo "::endgroup::"

echo "::group::Smoke test through port-forward"
kubectl -n "$NS" port-forward svc/crm-frontend 14200:80 >/tmp/pf-ui.log 2>&1 & PF1=$!
kubectl -n "$NS" port-forward svc/crm-backend 18080:8080 >/tmp/pf-api.log 2>&1 & PF2=$!
sleep 5
set +e
UI_URL=http://localhost:14200 API_URL=http://localhost:18080 AGENT_PASSWORD="$APP_DEMO_AGENT_PASSWORD" \
  SMOKE_LABEL="k3s ${NS}" "$REPO_ROOT/scripts/smoke-test.sh"
smoke=$?
set -e
kill $PF1 $PF2 2>/dev/null || true
echo "::endgroup::"
[ "$smoke" -eq 0 ] || rollback "post-deploy smoke test failed"

summary "### Deployed to k3s namespace \`${NS}\`"
summary "| | Previous | Now running |"
summary "|---|---|---|"
summary "| backend | \`${prev_backend}\` | \`${BACKEND_IMAGE}\` |"
summary "| frontend | \`${prev_frontend}\` | \`${FRONTEND_IMAGE}\` |"
summary ""
summary "Manual rollback: \`kubectl -n ${NS} rollout undo deploy/crm-backend && kubectl -n ${NS} rollout status deploy/crm-backend\`"
echo "Deploy OK"
