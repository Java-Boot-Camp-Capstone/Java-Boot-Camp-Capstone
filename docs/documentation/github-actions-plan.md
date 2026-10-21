# GitHub Actions Plan

| Trigger | Jobs | Evidence |
|---|---|---|
| `pull_request` | Angular `npm ci`, tests, and `ng build`; Maven `verify`; security checks | GitHub Actions logs and test results |
| `push` to `main` | Run checks, package the application, build the Docker image once, and record its digest/checksum | Successful workflow run and image digest artifact |
| Release tag / approved environment | Deploy the previously built image to k3s, subject to environment approval | Deployment workflow logs and environment approval record |

## Notes
- **Package once:** Deployment must use the previously built image rather than silently rebuilding it.
- **Angular is in scope:** The frontend build and tests are part of full-stack CI.
- **Security:** Run the required security checks before packaging or deployment.
- **Secrets:** Store credentials in GitHub Actions secrets or approved environment-level secrets. Never commit them to the repository.
- **Deployment target:** k3s is the target in the current architecture plan.
