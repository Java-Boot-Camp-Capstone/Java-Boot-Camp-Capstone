# Northstar CRM Capstone Backlog

## CAP-12 — Persist Customer Interactions
**Goal:** Store customer interactions in PostgreSQL through the Spring Boot API.

**Acceptance criteria:**
- A customer interaction can be created through a REST endpoint.
- Each interaction is associated with an existing customer.
- PostgreSQL stores the interaction data.
- Invalid requests return an appropriate error.
- Tests verify successful creation and failure cases.

## CAP-13 — Customer List and Detail UI
**Goal:** Allow users to view customers in the Angular application.

**Acceptance criteria:**
- The UI displays customers returned by the Spring Boot API.
- A user can view an individual customer's details.
- Loading and error states are handled.

## CAP-14 — JWT Login and API Protection
**Goal:** Protect customer and interaction endpoints.

**Acceptance criteria:**
- The Angular application sends a JWT with authenticated requests.
- Spring Boot validates the token.
- Unauthorized requests are rejected.
- Tests cover authorized and unauthorized requests.

## CAP-15 — Automated Pull Request Checks
**Goal:** Verify changes before they are merged.

**Acceptance criteria:**
- GitHub Actions runs Angular tests and builds.
- Maven verification runs for the Spring Boot API.
- Required security checks run.
- Failed checks prevent the pull request from passing its required gates.

## CAP-16 — Deploy and Smoke-Test on k3s
**Goal:** Validate a tested application image in the k3s environment.

**Acceptance criteria:**
- The deployment uses a recorded image digest.
- The application starts successfully.
- A smoke test verifies the API is responding.
- Deployment requires the appropriate environment approval.

## CAP-17 — Publish Customer Interaction Events
**Goal:** Publish versioned Kafka events when customer interactions are created.

**Acceptance criteria:**
- Creating an interaction produces the expected Kafka event.
- The event follows the agreed schema and topic naming convention.
- Tests verify event publication.
- Failures can be investigated using logs and correlation IDs.