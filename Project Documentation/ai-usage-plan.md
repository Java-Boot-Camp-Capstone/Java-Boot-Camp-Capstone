# AI usage plan

## Allowed
- Draft boilerplate code and explain unfamiliar code.

- Suggest unit tests and test cases for review.

- Draft documentation and architecture diagrams.

- Create Terraform and Ansible configuration sketches.

- Help troubleshoot errors and explain possible solutions.

## Must verify
- [ ] Angular: Confirm changes compile, match API DTOs, and handle authentication and authorization correctly.
- [ ] Flyway/SQL: Review migrations for PostgreSQL correctness, appropriate constraints, and safe schema changes.
- [ ] Security: Confirm no secrets or sensitive data are introduced.
- [ ] Testing: Run relevant tests and builds. Do not assume AI-generated code works without verification.
- [ ] Review: Inspect the full diff before committing or merging AI-assisted changes.

## Forbidden
- Committing secrets, credentials, tokens, or kubeconfig files.
- Sharing real customer data or other sensitive information with AI tools.
- Applying Terraform or Ansible changes to production without explicit authorization.
- Copying proprietary third-party code without permission.
- Merging unreviewed AI-generated code or claiming tests passed when they have not been run.

## Human Responsibility 
Developers are accountable for all AI-assisted contributions. AI suggestions must be reviewed, tested, and approved through the team's normal Git workflow before merging.