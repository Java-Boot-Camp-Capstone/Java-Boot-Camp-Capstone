# Demo Script — Northstar CRM Final Defense

**Target demo time:** 10 minutes  
**Goal:** Demonstrate a customer interaction from the Angular UI through the backend to PostgreSQL, explain the event flow, and show an authentication failure case.

**Fixtures:** `CUS-1001` (Amina), `CUS-1002` (Ravi), `CUS-9999` (not found, if supported), `lab-request-001`


If one person performs multiple roles, that's fine. The roles simply separate the actions from the explanation.

## 1. Opening and business goal — 0:00–0:45

**Team Member 1 says:**

“Northstar CRM gives users a way to find customer records and manage their interactions. Today, we'll demonstrate a customer interaction being created, persisted, and verified, then show how the system handles an unauthorized request.”

**Action:** Display the application and identify the main workflow.

## 2. Find a customer — 0:45–1:45

**Team Member 2 does:**
1. Open the customer search or customer list.
2. Search for `CUS-1001`.
3. Open Amina's record.

**Team Member 2 says:**

“We're starting with a known customer. The goal is to show how a user reaches the correct customer record before recording an interaction.”

**Pass signal:** The correct customer profile opens.

## 3. Show the interaction history — 1:45–2:45

**Team Member 3 does:**
1. Locate the interactions or timeline section.
2. Show the existing entries.
3. Briefly identify the fields displayed, such as interaction type, summary, or timestamp, if present.

**Team Member 3 says:**

“This view gives the user context about previous interactions before recording a new one.”

**Pass signal:** The interaction history renders correctly.

## 4. Create a new interaction — 2:45–4:15

**Team Member 4 does:**
1. Start the create-interaction workflow.
2. Enter a short, synthetic note.
3. Associate it with `CUS-1001`.
4. Submit the interaction.

Example summary: `Resolved login question`

Use the interaction type and fields supported by the actual application.

**Team Member 4 says:**

“Now we're creating a new interaction for Amina. The important part is not just accepting the form input; it's ensuring that the interaction is associated with the correct customer and successfully processed.”

**Pass signal:** The UI reports success and the new interaction appears in the history, if the UI refreshes automatically.

## 6. Verify persistence in PostgreSQL — 4:15–5:30

**Team Member 1 does:**
1. Use the prepared SQL query or approved database inspection method.
2. Locate the newly created interaction.
3. Verify its customer association and summary.

**Team Member 1 says:**

“This is the persistence check. A success message in the UI isn't enough by itself. We verify that the interaction was actually saved in PostgreSQL.”

**Pass signal:** The corresponding database row exists and matches the submitted interaction.

**Important:** Prepare and test the SQL query before the presentation. Use the actual table and column names from the project; don't guess them during the demo.

## 7. Explain the event flow — 5:30–6:30

**Team Member 2 does:**
1. Show the relevant application log or Kafka consumer output, if available.
2. Identify the event associated with the operation.
3. Show the correlation ID `lab-request-001` if it is present in the relevant output.

**Team Member 2 says:**

“Kafka supports asynchronous event-driven communication where the application publishes events for downstream consumers. Here, we're checking the event evidence to see what our implementation actually publishes and processes.”

**Pass signal:** Verified output shows the expected event and, where implemented, its correlation ID.


## 8. Show delivery evidence — 6:30–7:45

**Team Member 3 does:**
- Show the relevant successful CI pipeline run.
- Identify the build or container image digest if one is recorded.

**Team Member 3 says:**

“The application also has a delivery process. This evidence shows the build and validation results for the version we're discussing. The image digest identifies a specific image artifact.”

**Pass signal:** The evidence is accessible and corresponds to the intended project version.

## 9. Demonstrate an authentication failure — 7:45–9:00

**Team Member 4 does:**
1. Call a protected API endpoint without an authentication token.
2. Inspect the HTTP response.
3. If the application supports role-based authorization and time permits, test an authenticated user with insufficient permissions separately.

**Team Member 4 says:**

“We've shown the successful workflow. Now we're testing the security boundary. The API—not just the Angular interface—must enforce access rules.”

**Pass signals:**
- Missing or invalid authentication produces `401 Unauthorized` when that is the application's expected behavior.
- An authenticated user lacking the required permission produces `403 Forbidden` when that case is implemented and tested.

These are different cases. Don't substitute one for the other or claim a response your application doesn't return.

## 11. Close — 9:00–10:00

**Team Member 1 says:**

“We've followed a customer interaction through the user interface and verified its persistence. We've also reviewed the event and delivery evidence available for this implementation and tested an authentication failure case.”

Then briefly state:

- **Known limitation:** One concrete limitation that is actually present in the project.
- **Next step:** One measurable improvement the team would prioritize with another week.

**Pass signal:** The demo ends with a clear summary and an honest statement of what remains.

---



