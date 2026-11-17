# Local requirement workflow

The local workflow API lives at `/api/requirements?projectName=...`.
It persists requirements and department reviews in Flyway migration V41,
and reuses the existing `ideas` table and `IdeaStore` contract.

Implemented operations:

- List/get/create/edit/delete draft requirements, with optional source idea links.
- List/get/submit ideas, including the existing .txt/.md upload reader.
- Record QUALITY, FEASIBILITY and TESTABILITY pass/fail reviews.
- Approve only after all departments pass; reopen for fresh review.
- Clear reviews when requirement text changes.
- Require the current version for updates, reviews, approval, reopening and deletion.
- Restrict every record lookup to the named project and authenticated stakeholder.
- Record successful mutations in `command_audit_log` within the same transaction.
- Cascade requirement/review removal when a source idea's project is deleted.

The new REST slice uses transactional controller methods with the shared stakeholder
authorization checker and audit entity. It is not registered in CommandGateway/MCP.

## Permission mapping

Until dedicated workflow roles are introduced, use existing project permissions:

| Action | Permission |
| --- | --- |
| Read | Project stakeholder |
| Submit idea / create or edit requirement | Project Edit |
| Delete requirement | Project Delete |
| Analyst review | Goal Edit or Story Edit |
| Engineer review | UseCase Edit or Actor Edit |
| Test Engineer review | Scenario Edit |
| Approve / reopen / any department review | Project Grant |

The frontend's Idea/Requirement edit/delete controls map to Project permissions.

## Remaining integrations

Jama search/publishing and workflow-specific AI clarification, breakdown and overlap
comparison are not implemented. Their endpoints return HTTP 503 explicitly and do
not claim to generate results or publish anything. The older AI-assisted project
creation flow remains a separate feature. Dedicated workflow role administration,
gateway/MCP registration, annotation/source adapters and SSE subscriptions for the
new Requirement type remain future work.

Core state tests: `RequirementWorkflowTest`.
Project access tests: `RequirementControllerTest`.
