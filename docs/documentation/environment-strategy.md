# Environment Strategy

| Environment | Proposed k3s namespace | Purpose |
|---|---|---|
| Development | `crm-dev` | Development and initial testing |
| Test | `crm-test` | Automated and integration testing |
| Staging | `crm-stage` | Validate changes before production |
| Production | `crm-prod` | Production deployment; requires explicit approval |

## Configuration Promotion
- Promote the same tested application image through environments rather than rebuilding it for each environment.
- Keep environment-specific configuration separate from application code.
- Use Kubernetes ConfigMaps for non-sensitive configuration and Kubernetes Secrets or an approved secret-management system for sensitive values.
- Never commit credentials, tokens, kubeconfig files, or other secrets to Git.

## Approval Gates
- Run required tests and security checks before promoting an image.
- Require review and approval before deploying to staging or production.
- Do not perform production deployments or infrastructure changes as part of planning.

## Current Status
These namespaces and promotion rules are proposed for future deployments of the Northstar CRM. 