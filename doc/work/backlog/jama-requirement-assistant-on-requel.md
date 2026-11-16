# Building the Jama Requirement Assistant on Requel

Sources: `SRSvF-1.pdf` (SRS v3.0, 10/28) and `Jama Vision Doc FINAL.docx` (Vision v2.1, 10/27).
Status: proposal. It is not tied to an issue yet, so it lives in `doc/work/backlog/`.

## Summary

The Vision and SRS describe a **Requirement Assistant** for Jama Connect. A Product Manager enters a rough
idea and the assistant breaks it into draft requirements. Analysts, Engineers and Test Engineers review the
drafts. The Product Manager approves them, and an Analyst publishes them to Jama Connect.

Requel already has most of the platform this needs:

- a Spring Boot backend and an Angular SPA
- a command pipeline with authorization, auditing and optimistic locking
- an AI assistant framework with provider adapters (OpenAI, Anthropic, Gemini, local models)
- an MCP server
- an IBIS-style discussion layer for attaching findings to entities

It does **not** have the workflow, the roles or the Jama integration, and those are the real work. A rough
split is **about 40% reuse, 60% new**. The reused parts are infrastructure. The new parts are the product.

Recommended approach: fork or branch the repo, keep the platform modules, and add a new requirement-workflow
slice (domain, JPA, commands, REST, Angular) plus a Jama Connect client module. Don't try to bend Requel's
Goal/Story model into the SRS workflow.

---

## 1. What the SRS and Vision require (condensed)

| Area | Source | Gist |
|---|---|---|
| Actors | SRS 04.1 | Product Manager, Analyst, Engineer, Test Engineer, Jama Connect (external system) |
| Idea intake and breakdown | UC-01..03, F-01 | PM submits text or a file; the AI proposes requirement drafts and clarification questions; the PM reviews the breakdown |
| Refine and compare | UC-04, UC-05, F-02/03/08 | Analyst refines drafts and compares a draft against another requirement (including ones fetched from Jama), then records a resolution |
| Three reviews | UC-06..08, F-05 | Analyst (quality), Engineer (feasibility), Test Engineer (testability) each record pass or fail |
| Approve and publish | UC-09, UC-10, F-06 | PM approves once all three reviews pass; an Analyst publishes to Jama Connect |
| Supplemental rules | SR-01..14 | Jama API, roles, 10 MB .txt/.docx/.pdf input, 60 s draft and 30 s assessment limits, "shall" statement standard, traceability, audit trail, no partial saves, concurrent edit protection, review status visibility, outage retry, TLS 1.2+, AI confidentiality |
| Business rules | BR-01..12 | AI output is advisory; any content change resets reviews; approval needs 3 passes; Jama owns the requirement after publish |
| Nice-to-have | F-07, F-08 | Consistent formatting; show related existing requirements next to suggestions |

---

## 2. What Requel gives us

| Requel asset | Where | How we use it |
|---|---|---|
| Spring Boot 3 / Java 17 monorepo, Flyway, MySQL | `modules/requel-app` | Application shell and persistence, unchanged |
| Domain / JPA module pairs | `project-domain`, `project-jpa`, … | Pattern for a new requirement module pair |
| Command pipeline: `POST /api/commands/{type}` with handler chain (`AuthorizingCommandHandler`, optimistic locking, audit) | `doc/architecture/AUTH_ARCH.md` | Every SRS action (submit idea, review, approve, publish) becomes a command. This covers SR-02, SR-08, SR-10 and BR-01/04/06/10/11 |
| Assistant framework | `assistant-api`, `assistant-core`, `assistant-ai` | Runs the AI steps (decompose, compare, review). See section 3 |
| AI provider adapters on Spring AI | `assistant-ai` (`ai-openai`, `ai-anthropic`, `ai-gemini`, `ai-ollama` profiles) | Provider choice is configuration, not code. Setup is in `doc/guides/AI_ASSISTANT_SETUP.md` |
| `DataHandlingGuard` | `assistant-ai` | Fails closed unless `externalProviderAllowed=true`. This is a direct hook for SR-14 (idea confidentiality) |
| JSON-schema-driven AI definitions | `assistant-ai/src/main/resources/ai/definitions/*.json`, `ai/schemas/` | Add prompts and output schemas for decomposition and the three review types without new Java plumbing |
| IBIS annotation layer (issues, positions, notes) | `annotation-domain`, `annotation-jpa` | Store AI findings, overlap comparisons and clarification questions, and attach them to a requirement |
| MCP server (`POST /api/mcp`, OAuth 2.1, PATs, audited, rate-limited) | `mcp-server` | Optional: lets an external agent drive the workflow with the same permissions as the UI |
| SSE streaming, Angular app shell, wizards, data tables, a11y work | `requel-angular` | UI scaffolding for the workflow screens |
| JAXB import/export | `utils-jaxb` | Not needed for the MVP; could back requirement export later |
| Docker, docker-compose, CI, e2e (Playwright) | repo root | Deployment and test harness |

---

## 3. Mapping SRS items to Requel

### 3.1 Features

| Feature | Requel starting point | New work |
|---|---|---|
| F-01 Idea decomposition | `assistant-ai` definitions and `OutputSchemas` | A `decompose-idea` definition and schema (output: candidate requirements plus open questions); idea intake command and UI |
| F-02 / F-08 Overlap detection and references | `corpus-relationships.json` (cross-entity relationship analysis) | A comparison definition for "draft vs. existing requirement"; a Jama fetch to supply the existing requirements |
| F-03 Drafting and refinement | `EditXCommand` pattern and `review-*` definitions | A `RefineRequirementCommand` that takes AI suggestions or direct edits |
| F-04 Ambiguity and completeness | `requirements-review.json` (already reports ambiguity and missing detail) | Tune the prompt to the SRS "shall"-statement standard (SR-06) |
| F-05 Review and editing | IBIS issues and positions | A first-class review record per department (see 4.2) and per-suggestion accept/reject |
| F-06 Jama integration | Nothing | New `jama-connect-client` module (see 4.3) |
| F-07 Consistent formatting | Prompt definitions | A format and template check, either as prompt rules or a deterministic validator |

### 3.2 Use cases

| UC | Command(s) to add | UI to add |
|---|---|---|
| UC-01 Submit idea | `SubmitIdeaCommand` (PM only); an `IdeaDecompositionAssistant` run | Idea form with text box and file upload |
| UC-02 Clarify | `AnswerClarificationCommand` (answer or "unknown") | Question and answer panel on the idea page |
| UC-03 Review breakdown | `ReviewBreakdownCommand` (keep, reject, revise) | Breakdown list with keep/drop checkboxes and a feedback box |
| UC-04 Refine draft | `RefineRequirementCommand` (draft only, Analyst only) | Edit view with side-by-side AI suggestion |
| UC-05 Resolve overlap | `CompareRequirementsCommand`, `RecordOverlapResolutionCommand` | Compare view with two panes, resolution form with justification |
| UC-06/07/08 Reviews | `SubmitReviewCommand(department, pass/fail, comments)`, with the role check per department | Review screen with an AI report and a pass/fail control |
| UC-09 Approve | `ApproveRequirementCommand`, `ReopenForReviewCommand` (PM only) | Approve button, enabled only with three passes |
| UC-10 Publish | `PublishRequirementCommand` (Analyst only) | Publish list, "No requirement ready" empty state, retry on failure |

### 3.3 Supplemental requirements and business rules

| Item | Approach |
|---|---|
| SR-01, SR-12, NFR-02-D (Jama API, outage retry, failed publish leaves state untouched) | Jama client module with a timeout, a retry-friendly error type and publish-then-commit ordering (see 4.3) |
| SR-02, BR-01/04/06/10/11, NFR-01..05-S (role checks) | New role classes plus `AuthorizableCommand.getAuthorizationRequirement()` on every command. Requel only has `SystemAdminUserRole` and `ProjectUserRole` today (see 4.1) |
| SR-03 (text, .txt/.docx/.pdf up to 10 MB) | Multipart upload endpoint plus text extraction (Apache Tika or POI/PDFBox) with a 10 MB limit. Verify that nothing equivalent exists before building it |
| SR-04, SR-05, NFR-01-P (60 s, 30 s, 180 s timeouts) | Per-assistant timeouts through the run handle; the 180 s cap is a hard `AssistantRun` timeout. Measure against the chosen model |
| SR-06 ("shall" statement, unique ID) | A requirement ID generator plus a prompt and schema constraint, plus a deterministic validator after generation |
| SR-07 (traceability) | Links from requirement to idea, revision, reviews and Jama item ID (new entity fields and a revision table) |
| SR-08 (audit trail) | Reuse the command audit; add a review/approval/publish audit row if the existing audit lacks role and outcome |
| SR-09, NFR-01-D (no partial saves) | Already the default with one transaction per command; add tests that force failures mid-command |
| SR-10 (concurrent editing) | Requel has optimistic locking (`108-edit-optlock-plan.md`); apply it to the requirement entity and surface the conflict in the UI |
| SR-11 (review status visibility) | Show the status and the three review results on every requirement view and list |
| SR-13 (TLS 1.2+) | Terminate TLS at the reverse proxy; configure the Jama client and the AI client to require it |
| SR-14 (confidentiality, approved AI only) | `DataHandlingGuard` plus an allow-list of providers; a project-level flag for `externalProviderAllowed`. For a strict posture, use the local Ollama profile |
| BR-02, BR-03 (drafts, advisory AI) | The AI assistants already write *findings*, not changes. Keep it that way: AI output creates suggestions that a user confirms through a command |
| BR-07, BR-08, BR-09 (one review per department, change resets reviews, three passes to approve) | Enforce in the domain model, not the UI: reviewing replaces the current review, saving content clears all three, and approve checks all three |
| BR-12 (Jama owns published requirements) | A terminal `PUBLISHED` status that rejects edit, review and approve commands |

---

## 4. Gaps we have to build

### 4.1 Roles and permissions

Requel's roles (`SystemAdminUserRole`, `ProjectUserRole`) are coarse, and the per-project model uses
stakeholders and permissions (`StakeholderPermission`). The SRS needs four named roles that gate each command.
Options:

- **(Recommended)** Add four `UserRole` subclasses, or a role enum on the stakeholder, and map each command to
  one through `AuthorizableCommand`. A user may hold more than one role (SR-02).
- Reuse stakeholder permission types, one per action. This is less new code but harder to read.

### 4.2 Requirement workflow model

Requel's Goal, Story and UseCase entities have no approval state or per-department reviews. Add to
`project-domain` / `project-jpa` (plus a Flyway migration):

- `Idea`: source text or file, owner, status, open clarification questions
- `Requirement`: ID, "shall" text, status (`DRAFT`, `IN_REVIEW`, `APPROVED`, `PUBLISHED`), idea link, revision
  history, optional Jama item ID, version for optimistic locking
- `RequirementReview`: department (`QUALITY`, `FEASIBILITY`, `TESTABILITY`), result (`NOT_REVIEWED`, `PASSED`,
  `FAILED`), reviewer, role, timestamp, comments
- `OverlapResolution`: the two requirements, the AI finding, the decision, the justification

Open design decision: the SRS's requirements could be the existing Goal or Story type with a status added.
That is faster, but it drags Requel's goal/actor/scenario relationships into a workflow that doesn't use them.
The proposal is a separate `Requirement` aggregate. Remember that a Flyway migration that has been applied can
never be edited (see `CLAUDE.md`), so settle the schema before the first migration lands.

### 4.3 Jama Connect client (new module)

- A new module (for example `jama-connect-client`) behind an interface in a domain module, with the HTTP
  implementation in a separate module. Domain code must never depend on the HTTP client.
- Read: list or search the existing requirements in a Jama project (feeds UC-05, F-02, F-08).
- Write: create an item from an approved requirement. Order the steps so a failure leaves our state untouched
  (NFR-02-D): call Jama first, and mark `PUBLISHED` only after a success response.
- Credentials: per-user or service token held in the existing keystore mechanism (`105-keystore-credentials.md`).
  Retrieval must honor the user's Jama permissions (Vision, "Assumptions and Dependencies").
- Tests: use a stub or WireMock server for CI. Don't depend on a live Jama instance.
- Check the Jama REST API's actual create-item and search capabilities early. The SRS says "supported API",
  and the available endpoints may constrain F-06.

### 4.4 AI definitions

Add definitions and schemas beside the existing ones in `assistant-ai/src/main/resources/ai/`:
`decompose-idea`, `compare-requirements`, `review-quality`, `review-feasibility`, `review-testability`.
The feasibility and testability prompts must carry the Vision's cautions:

- Feasibility: don't assume capabilities or integrations that don't exist.
- Testability: don't invent thresholds or expected behavior the stakeholders haven't specified.

### 4.5 Angular screens

Under `requel-angular/src/app/features/`, add `ideas/`, `requirements/` (list, detail with review status),
`reviews/`, `compare/` and `publish/`. Reuse the existing data table, wizard, form and SSE announcement
components. Hide or remove Requel's unused features (terms, dictionary, reports) from navigation for this
product.

---

## 5. Suggested build order

1. **Decide the forks:** product name and repo; whether to keep the legacy NLP modules (`nlp-jpa`,
   `dictionary-jpa`), which this product doesn't need and which pull in a large NLP data download; and the
   open decisions in section 7.
2. **Domain and roles:** `Requirement`, `Review`, `Idea` entities; the four roles; the state-machine rules
   BR-02, BR-07..BR-12 as domain tests.
3. **Commands and REST:** UC-04, UC-06..UC-09 first (no AI or Jama needed), fully testable offline.
4. **AI intake:** UC-01..UC-03 with the Anthropic or Ollama profile; file upload.
5. **Jama client and overlap:** UC-05 and UC-10 against a stub, then a real Jama sandbox.
6. **UI** alongside steps 3 to 5, then e2e for the main path (idea to published).
7. **Hardening:** SR-04/05 timing measurements, TLS, the confidentiality flag, outage and retry tests.

Follow the repo's lifecycle (`CLAUDE.md`): one GitHub issue, one plan in `doc/work/<release>/` and one PR per
ticket. Suggested tickets: roles, requirement model, review commands, approval and publish rules, Jama client,
idea intake and decomposition, overlap comparison, Angular workflow UI.

---

## 6. Running it today

```bash
mvn -pl modules/requel-app -am package -DskipTests
java -jar modules/requel-app/target/requel-app-<version>.jar \
  --spring.profiles.active=dev,ai-anthropic --server.port=8080 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password
```

Set `REQUEL_AI_API_KEY` first (see `doc/guides/AI_ASSISTANT_SETUP.md`). Log in at http://localhost:8080/ as
`admin` / `admin`. This runs stock Requel. The Jama workflow does not exist until the work above is done.

---

## 7. Open questions and document inconsistencies

SRS open issues still open:

- **OI-01:** must every requirement pass all three reviews, or are exceptions allowed? (BR-09 currently says
  all three.)
- **OI-02:** can a draft with an unanswered clarification question be reviewed and approved?
- **OI-03:** can several requirements be approved or published in one action?

Inconsistencies we found between the two documents, to resolve before coding:

1. **Publish removes the requirement.** UC-10 says the requirement is "removed from the system", but BR-12 and
   the UC-10 acceptance test say it stays but is locked. SR-07 needs the Jama item ID kept for traceability, so
   keeping it locked looks right.
2. **Timeouts.** SR-04 says 60 s for draft generation, NFR-01-P says the assistant times out at 180 s, and
   NFR-03-P says 60 s for action buttons. Pick one budget per operation.
3. **Backup versus transaction.** NFR-01-D ("save a backup before any action") overlaps SR-09 ("no partial
   saves"). A single database transaction per command satisfies SR-09; a literal backup per action is
   probably not intended.
4. **Use case dependencies.** UC-01 says "Extended by UC-02" while UC-02 says "Extends UC-02". UC-04 says it
   extends UC-05 and UC-05 says it is extended by UC-04.
5. **UC-06 and UC-08 lack the full step-by-step form** that UC-07 has (no postconditions or variations).
6. **UC-07-VAR-02** (failed review) says the user "determines the requirement is ready to be approved", which
   reads like a copy-paste from the pass variation.
7. **UC-09 acceptance** lists F-01 as the supporting feature; it probably should be F-05.
8. **Analyst "approve" wording.** UC-06 says the analyst "can approve the requirement or request changes"
   while BR-10 says only a PM approves. Read "approve" there as "pass the review".
9. **Section 03.2 (Glossary) and the Domain and Class diagrams** are empty or are links only. We would need
   those to settle the model in 4.2.

---

## 8. Risks

- **Scope:** the SRS is workflow-heavy. The AI part is the smaller share of the effort.
- **Jama API fit:** unknown until we try it; spike it first.
- **AI quality and cost:** the Vision flags misleading output as a cost to reviewers. Use structured output
  with schema validation, and keep a human confirmation on every change (BR-03).
- **Inherited weight:** Requel carries NLP models, a dictionary and a legacy domain we don't need. Decide early
  whether to prune it or ignore it.
- **Licensing:** Requel is GPLv3 (`LICENSE`, `COPYING.txt`). A derivative product inherits those terms, which
  matters if the product will be distributed or sold. Check with the instructor or the team before committing
  to a fork.
