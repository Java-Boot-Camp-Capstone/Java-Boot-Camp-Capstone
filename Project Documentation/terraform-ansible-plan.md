# Terraform / Ansible Plan — Lab 48

## Terraform
- **Scope (non-production):** Define infrastructure resources required to support the Northstar CRM capstone and its k3s environment, if infrastructure provisioning is needed.
- **Project / namespace names:** Use instructor-approved Kubernetes namespace and resource names.
- **State backend:** Use an approved remote state backend with access controls and encryption if required. Do not commit backend credentials or state files to Git.
- **Validation:** Review configuration and run `terraform fmt` and `terraform validate` before any approved plan or apply.
- **Forbidden:** Publicly exposed databases, committed `*.tfstate` files, committed credentials, and unapproved changes to shared or production infrastructure.

## Ansible
- **Playbooks:** If required, create playbooks for approved non-production host configuration and software setup supporting the k3s environment. If the shared k3s cluster already provides the required infrastructure, document Ansible as not currently needed.
- **Idempotence notes:** Playbooks should be idempotent, meaning repeated runs leave systems in the intended state without unnecessary changes.
- **Validation:** Review playbooks before execution and test them in a non-production environment where available.
- **Secrets:** Use Ansible Vault or approved CI secrets for sensitive values. Never store plaintext secrets in the repository.
- **Approval:** Require review and explicit authorization before applying changes to shared infrastructure.


