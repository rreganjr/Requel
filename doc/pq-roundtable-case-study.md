# PlatformQ Roundtable — a Requel case study

A working record of what happened when two real Jira tickets were read as requirements, modelled as
Requel entities, and analysed. It exists to answer one question with evidence rather than intuition:
**what does a good requirements assistant actually need to notice, and what does Requel's model need
in order to hold the answer?**

Its conclusions drive `doc/ai-per-type-assistants-epic.md`. Where that spec asserts something about
per-type vocabularies, context providers or corpus scope, this document is why.

## Provenance and a rule

The source material is two real PlatformQ Jira tickets (an epic and one child) describing
`roundtable.medlive.com`, an internal tool that relays a Zoom webinar panel to a passive gated
audience via Amazon IVS. They were chosen because they are *well written* — anything wrong with them
is wrong with the format, not the author.

**Rule for anything derived from this:** the taxonomy and the findings below are portable and belong
in this repository. The source text does not. Any fixture project checked in must be synthetic or
thoroughly anonymised; this document paraphrases and quotes only what is needed to make a finding
legible.

## 1. What the source contained

The epic carried: a problem statement, a "why this matters" paragraph, a resources list, an
architecture summary, a note about intentional behaviour, a list of repo scripts, and six acceptance
criteria. The child ticket carried the same shape with seven acceptance criteria, several of them
procedural ("`npm run verify` passes on a clean checkout").

## 2. What came out, by entity type

**Goals** came almost entirely from the acceptance criteria — seven of them, one per AC. Ownership,
operator independence, two engineers able to run an event, admin gated on an Entra app role, a CI
pipeline blocking five named checks, alarms routed with documented first responses, and every review
finding dispositioned.

**Actors** came only from the description: Operator (creates rooms, rotates stream keys, force-stops
streams, ends rooms, downloads recordings, archives), Viewer (passive, gated page, never joins the
Zoom meeting), Panelist (in the Zoom webinar, sees the livestream indicator), Zoom Host/Co-host
(distinct — a permissions matrix is referenced), Conduit Engineer (deploys, responds to alarms).
External systems — Zoom, IVS, MediaConvert, Entra, CloudFront, DynamoDB — participate in every flow
and have no obvious home. See the open question in §7.

**Use cases** — applying the test "a collection of flows for a task, which can succeed or fail and
may vary":

| Use case | Flows beyond the happy path |
| --- | --- |
| Run a session | stream never starts; drops mid-session; operator force-stops and restarts; room ended while stream live |
| Watch as a viewer | unauthorized rejected; arrives before start; arrives after end; playback authorization expires mid-watch |
| Deliver the recording | remux fails; job claimed twice; reconciliation recovers an orphan |
| Sign in to the admin console | has app role; tenant member without role; outside tenant |
| Archive a room | IAM denies delete on one of two resource types; already-archived room |
| Respond to an alarm | the only home for "documented first response" |

**Scenarios.** One was already written and unrecognised: *create room → view as viewer → rotate
stream key → force-stop stream → end room → download MP4*, sitting inside a single acceptance-
criterion checkbox. The ordering carries an unstated precondition — rotating a stream key mid-stream
is a different operation from rotating one before a stream starts.

The valuable scenarios are the absent ones. A **force-stop** control exists, so a stuck or runaway
stream is an anticipated failure with no flow describing it. The child ticket mentions "concurrency
control in DynamoDB" and "the MP4 job claim and reconciliation path", so at least two concurrent
flows exist in code and nowhere in the specification.

**Stories** — prose about a use case, process or outcome — were all in the description and none in
the ACs:

- The system "relays a Zoom webinar panel out to a second, passive audience who never join the Zoom
  meeting and are invisible to the panelists." The only statement of the system's purpose anywhere.
- "A single-owner production system with a bus factor of one is a risk to any event scheduled on it."
- Some fixes "can only fail in ways an operator would not notice until an event is already running."
  This motivates an entire class of observability requirements that exist nowhere.
- The verification work "doubles as onboarding."

**Glossary.** *Room* is the central domain object, never defined, with at least four states.
*Event / session / room / dry-run event / test room / throwaway room* are used interchangeably.
*Viewer* (roundtable) sits beside *attendee* (Zoom) and *panelist* — three populations, adjacent
names, and a referenced Zoom permissions matrix whose vocabulary is never reconciled.

## 3. Single-entity findings — the taxonomy

Each of these came from a real goal, and each would be flattened to `AMBIGUOUS` or `INCOMPLETE` by a
generic prompt.

- **`SOLUTION_NOT_OUTCOME`** — "gated on a Microsoft Entra app role" names a mechanism. Replace the
  identity provider and the goal becomes unachievable while the intent is untouched.
- **`PROXY_FOR_OUTCOME`** — "Conduit is the named owner" is satisfied by editing CODEOWNERS. The
  stated intent is bus factor, and the goal can be fully true with bus factor still at one. Subtle
  *because* it is concrete and checkable; the checkability disguises it.
- **`PERSON_NAMED_NOT_ROLE`** — "Chris Peterson is no longer a required participant" embeds an
  individual. The same human is both an Actor (he operates the system) and a UserStakeholder (domain
  expert, and the CTO who drives the technology). See §5 — this one is more interesting than it looks.
- **`DUPLICATE_AT_DIFFERENT_ABSTRACTION`** — "Chris is no longer required" and "two engineers have
  run it unassisted" have the same success condition. They read as different goals because one is
  about a person and the other about a team. Undetectable without sibling goals in context.
- **`NEGATIVE_ONLY_SPECIFICATION`** — "so tenant membership alone does not grant access" says who is
  excluded and never who is admitted. The denial is testable; the grant is not, because the
  authorized set is nowhere defined.
- **`CONJUNCTIVE_GOAL`** — "alarms route to a Conduit-owned destination, each with a documented first
  response" welds two independently-completable goals. Sign-off covers both; progress cannot be
  tracked.
- **`CONSTRAINT_NOT_GOAL`** — that same goal has an open temporal quantifier: an alarm added next
  quarter silently regresses it. It is a standing rule with no success state, which is a policy.
- **`NOT_A_SYSTEM_GOAL`** — several of these are about team process and operations, not about the
  system being specified.
- **`IMPLICIT_INVARIANT`** — viewers being "invisible to the panelists" is stated as a description of
  how things currently work, with no goal protecting it. Add a viewer chat that surfaces to panelists
  and nothing in the specification says that is wrong. The hardest class to find, because the text
  reads as background.
- **`MEASURE_WITHOUT_BASELINE`** — an instrument named with no baseline or threshold, so "met" is
  never decidable.

Also present and correctly producing *no* finding: the epic states the review as
0 Critical / 5 High / 15 Medium / 12 Low, and the child as 24 fixed plus 8 deferred. 32 = 32. A
reviewer should check that and then say nothing. A definition that cannot stay quiet is worse than
one that misses.

## 4. Cross-entity findings — the ones with the most value

None of these is a property of any single entity, and nothing in Requel's current assistant
architecture can express them.

- **A CI gate that excludes the test its sibling warns about.** The epic's pipeline lists exactly the
  five checks in `npm run verify`. The child ticket lists `npm run test:ui` separately and warns that
  Playwright is not a committed dependency and that "the suite skips cleanly rather than failing when
  it is absent, so an unnoticed skip reads as a pass." The specified pipeline therefore never runs
  the UI tests, and the exact failure the child flags is the one CI is guaranteed to have. **Both
  requirements are correct in isolation.**
- **Two lifecycle states used as one.** The epic says the recording is remuxed "when the room is
  ended." The child lists "end the room" and, separately, "archiving a test room succeeds and the IVS
  channel and stream key are both actually deleted." So *end* ≠ *archive*, neither is defined, and an
  implementer reading the epic alone builds the wrong state machine. A domain-model defect, not a
  wording one.
- **A disposition vocabulary that does not match.** The epic requires every finding "fixed or
  explicitly closed with a written reason." The child describes "24 fixed and 8 deliberately
  deferred." Deferred is not closed. The parent's criterion is unsatisfiable by the child's own
  framing, and the child produces no artifact that would satisfy it.
- **Three terms for one concept** across two documents.

## 5. Type-assignment findings — a third category

Some content is not badly written; it is in the wrong kind of entity.

- A six-step acceptance criterion is a **Scenario**, not a badly-packed AC. The right output is
  "extract these into a Scenario with six Steps; the AC becomes *this scenario passes*."
- Sentences in a problem statement are **Stories**.
- A person's name in a goal is an **Actor** plus a **UserStakeholder**.

The last one repays attention, because modelling it correctly changes the answer rather than just
tidying it. Once Chris is the Operator *role* and separately a stakeholder, "Chris is no longer
required" splits in two. The part that is genuinely about the software — **the Operator role must be
fillable by more than one person** — implies role-based admin access rather than person-based, no
personal credentials on the path, and no undocumented step that exists only in someone's head. That
is testable and belongs in the specification. What remains after the split is operations and does not
belong in Requel at all.

So `PERSON_NAMED_NOT_ROLE` and `NOT_A_SYSTEM_GOAL` co-occur, and they interact: applying the first
*shrinks* the second. An assistant that raises only "this is not a software goal" gives bad advice.

Requel already has commands these findings would want to name: `ConvertStepToScenario`,
`AddScenarioToUseCase`, `upsertGoalFromRequirement`.

## 6. The format observation

**The description mined better than the acceptance criteria.** Seven goals came from the ACs; the
actors, the use cases, the invariants, the purpose of the system and every glossary term came from
the prose around them.

The reason is not that the tickets are badly written. It is that the AC field is being used for
*verification procedure* — "`npm run verify` passes on a clean checkout", "`npx cdk diff` reports no
changes" — rather than for *requirements*. Both are legitimate artifacts. Conflating them in one
field is what loses the requirement: a test procedure tells you how you will know, and says nothing
about what must be true or why. The requirements then migrate into the description, because that is
where people explain things.

Requel already separates these: a Goal or Use Case states what must be true; a Scenario states a flow
that can be walked. The endemic Jira failure is having one field for both.

## 7. What this taught us about Requel

Folded into `doc/ai-per-type-assistants-epic.md`:

1. **Per-type finding vocabularies are justified.** §3 is the evidence; the generic three-bucket list
   destroys it.
2. **Corpus scope is missing.** §4 findings belong to a set of entities, not a target — now child 8.
3. **The Goal context provider needs sibling goals**, not just `GoalRelation`s, because the missing
   relation is often the finding.
4. **The Actor context provider needs stakeholders.** Role and interest are different things about
   the same human, and a reviewer that sees only one cannot propose the fix in §5.
5. **Evidence citations can be verified without a model** — assert the cited phrase occurs in the
   entity text, drop the finding if not.
6. **Extraction findings are a third category** alongside quality and conflict.

Open, and recorded as questions on the epic:

- **Where do explicit non-goals live?** "Panelists do see the livestream indicator; that is expected
  and is not to be suppressed" is normative, is not a goal or a use case, and a Note carries no
  weight. An assistant has nowhere to attach a finding that a non-goal is being violated.
- **Are external systems actors?** Zoom, IVS and MediaConvert participate in every flow.
- **Can a position carry a structured proposal**, so an extraction finding can name a command rather
  than describe one in prose?

## 8. The larger use case this suggests

Reading a ticket into requirements, analysing them, annotating the problems, and emitting a corrected
ticket is a **round trip**, and it is a different capability from "AI reviews an entity." It makes
Requel the consistent model behind inconsistent documents, with the ticket as a rendering of a subset
rather than the source of truth — a live document that stays consistent as a project grows.

Most of the pieces exist: the MCP gateway can read and write entities, `upsertGoalFromRequirement`
is the ingest primitive, `ReportGenerator` is a domain entity, and annotations already carry the
suggestions. What is missing is **provenance** — an entity knowing which external artifact it came
from, so re-ingesting updates rather than duplicates — and a decision about direction. Requel as
source of truth with Jira as an emit target is coherent. True bidirectional sync is where projects
like this die.

This is deliberately **not** folded into the per-type assistants epic; it is a larger idea that would
sink it. It is recorded here as the direction the epic should not foreclose.

## 8a. Model gaps found while building the project

Building the project through the MCP gateway surfaced two gaps that reading the tickets did not.
Each has its own write-up:

- **Provenance has no home.** Tags were used for it and their limits are now measured, not guessed —
  see `doc/entity-provenance-notes.md`.
- **`GoalRelationType` has only `Supports` and `Conflicts`,** so `PROXY_FOR_OUTCOME` and
  `DUPLICATE_AT_DIFFERENT_ABSTRACTION` can be reported but not repaired — see
  `doc/goal-relation-types-proposal.md`.

### Gateway write-surface findings

Building the whole project through the MCP gateway exercised the write surface harder than any test
does. What it found, in the order it was hit:

- **`DeleteProject` is not on the running instance's gateway allowlist**, though the command and its
  ITs are in the tree. Other `Delete*` writes are offered, so writes are enabled — the running build
  predates #242's gateway exposure.
- **`canDelete: false` in a command result is not a bug** — `ProjectCommandRegistrar.toDto()`
  hardcodes it with the comment *"canDelete is only meaningful on the list query
  (ProjectQueryController), which resolves the caller; command-result DTOs report false."* The
  earlier reading here — "the creator cannot delete their own project" — was wrong, twice: the
  creation response always says false, and the same project read back through the list query said
  true. What *is* real is the list-query result: `callerHoldsProjectDelete` requires the caller to
  be a `UserStakeholder` on the project holding `Project[Delete]`, and 377 of 524 projects do not
  satisfy that for the admin who created them. That is authorization working as designed, and it
  blocks the cleanup #239 exists for — filed separately.
- **Steps are shared by design, and the write surface hides it.** `StepImpl.usingScenarios` is
  `@ManyToMany(mappedBy = "steps")`, so a step name *is* its identity and "End the room" is meant to
  be the same Step entity in the happy path and in the verification pass — which is what was
  subsequently done by passing `stepId: 738`, putting one step in both scenarios. The original
  conclusion here (that project-wide step-name uniqueness was wrong for ingest) was mistaken; the
  constraint is correct. What is wrong is the discoverability: creating a scenario whose step list
  repeats an existing step name fails with *"The name conflicts with an existing
  ProjectOrDomainEntity"*, naming neither the step nor the entity it hit, and attributing the
  failure to the scenario. Two scenario renames were wasted before the cause was isolated. And a
  step can only be *reused* by `stepId`, so a client working from prose must look up ids before it
  can express the sharing the model wants.
- **`EditScenario` NPEs on a null name when editing by id.** `EditScenarioInput.name` is nullable,
  but the command throws *"Cannot invoke String.trim() because name is null"* rather than preserving
  the existing name or returning a validation error.
- **`EditScenarioStep` is exposed as a typed MCP tool with an empty input schema** — no properties at
  all — so it cannot be called. Steps are only reachable through `EditScenario`'s `steps` array.
- **A CGLIB proxy class name leaks into the API response.** `EditPosition` returns
  `"positionType": "PositionImpl$$EnhancerByCGLIB$$1f1035a5"`.
- **Tag names and values are silently lower-cased** — see `doc/entity-provenance-notes.md`.

What worked well and is worth recording: `EditUseCase` auto-creates a primary scenario; the glossary
`canonicalTermId` models the three-terms-for-one-concept finding natively and echoes back
`canonicalTermName`; `EditScenario` accepts a whole step list in one call; and `EditIssue` +
`EditPosition` let a finding and its proposed repair be attached in two calls.

## 8b. The emit half

`doc/pq-roundtable-emitted-CON-3685.md` holds the first generated ticket and its analysis. The short
version: structure (actors, use cases, glossary, traceability) is a deterministic projection an XSLT
could produce byte-identically; prose (problem statement, which open questions to surface) is not.
The emitted ticket dropped everything the original carried that is not normative — AWS account,
stack name, repo scripts, architecture summary — because none of it is a Requel entity, so emit
cannot be "export the project"; it has to render the normative sections and leave the rest. And
provenance was missed on the very first emit, at exactly the granularity predicted: the generator
cannot say "AC1 became goals 1 and 2", which is the first thing a reviewer comparing the two
documents wants.

## 9. Three sources, three authorities

The Jira tickets were only the first source. Two more arrived: `docs/Roundtable-Production-Guide.pdf`
(25 pages, v1.1) and `docs/round-1-review.md` (32 findings). Reading all three changed the
conclusions more than any amount of further analysis of the tickets would have.

**The tickets describe a custody handoff. The guide describes the product. The review describes the
truth.** The model built from tickets alone was a model of the *project*, not the system — which is
the deepest reason the ticket pass mined thin, and it is not a criticism of the tickets. A handoff
epic is not supposed to specify a product.

Two of the three sources state their own authority, unprompted. The guide's colophon: *"The
operational detail in this guide is derived from RUNBOOK.md in the roundtable repository; if the two
ever disagree, the repository is correct and this document needs updating."* The review's provenance
section says the same about source versus document. **A living-document model needs to record that
ordering and currently cannot** — there is nowhere in Requel to say "document A is subordinate to
document B".

### What the guide added that the tickets omitted

- **Two IVS channels per room**, primary and standby — the tickets say one. A flat contradiction.
- **A second state machine.** INGEST (offline/connected/live/starved/failed) is independent of STATE
  (draft/test/live/ended). The guide calls confusing them "the most common misread on this screen".
- **The invariant the guide says to trust:** a viewer is never shown the stream unless the room is
  live, "enforced in one place and covered by a test over every combination of states".
- **Only the Zoom host can start a custom livestream** — not a co-host, not a panelist. Called "the
  single most common day-of failure" and explicitly a staffing problem to solve a week out.
- **The recovery ladder**, three ordered actions, and the distinction that matters: *New takeover
  key* is the recovery control; *Rotate stream key* is for a leaked key and cuts off anyone
  mid-stream. **CON-3686's manual pass says "rotate the stream key"** — either the wrong control or
  imprecise language for the right one, and worth confirming before the verification pass runs.
- **The encoder validation panel**, an entire feature: bitrate ceiling, missing-audio detection,
  H.264 profile, aspect ratio, plus an explicit list of what it refuses to guess at.
- **Security facts:** the MP4 download link is minted on click, lives 15 minutes and carries no
  sign-in. Everything in storage expires at 90 days.
- **The best requirement in any of the three documents:** adopting hot standby puts an external
  encoder in the path, which removes Zoom's livestream indicator — and that indicator "is doing our
  disclosure work for us". An architecture choice silently relocating a compliance guarantee from
  the platform to a human step. Nothing in the tickets approaches this.

### What the review added, and one finding that outranks everything else

The review opens by contradicting both tickets: *"The ticket text is wrong about the split. CON-3685
and CON-3686 say 24 fixed and 8 deferred. Checking all 32 findings against the current tree gives 26
with the fix present and 6 without. The 24/8 figure was a recollection, never a count."*

This is the single most instructive result in the whole exercise, because **§3 of this document
records checking that arithmetic and finding it consistent** — 0+5+15+12 = 32 = 24+8 — and treating
the silence as correct behaviour. It was internally consistent and factually wrong. *Consistency
checking cannot detect a shared wrong premise.* Any evaluation fixture must contain a case like
this, and any assistant that reports "the numbers add up" must not imply the numbers are right.

Worse for the model: "deferred" was never accurate. Six findings are **not fixed**, and one of them
is live. Review finding L1 — the committed `cdk.json` still carries an empty `entraAdminRole`, and
the admin Lambda skips the app-role check when it is empty — means that if the deployed stack came
from HEAD, **any authenticated member of the Medlive tenant can reach `/admin`**, end a live room,
rotate keys or download recordings. That is the exact condition CON-3685's third acceptance
criterion forbids. The goal is not merely badly worded; it may be false in production right now.

The review also surfaced three sibling tickets neither source ticket references — CON-3687, CON-3690
and CON-3691 — including CON-3690, which owns the alarm *subscription* and is therefore where the
actual substance of the alarm-routing criterion lives.

### A new finding type: `VACUOUS_QUANTIFIER`

At review time the stack created **zero** CloudWatch alarms, despite log markers written specifically
for them. CON-3685's criterion is "all CloudWatch alarms route to a Conduit-owned destination, each
with a documented first response" — which an empty set satisfies perfectly. The criterion could have
been signed off as met while the system had no alarming at all.

A goal quantified over a set the project also controls needs the set's existence stated as a separate
condition. This joins the Goal vocabulary in `doc/ai-per-type-assistants-epic.md`.

### Reference material is four things, not one

Working through what the tickets carried that the model could not hold:

| Kind | Example | Where it belongs |
| --- | --- | --- |
| Citation | ENTRA_SETUP.md, the Zoom permissions matrix, RUNBOOK.md | Requel — and this is structurally identical to provenance |
| As-built description | "the architecture is Zoom → IVS → CloudFront" | The external doc, cited. It can be *wrong*, which a requirement never can |
| Environment fact | AWS account, region, stack name | The IaC, cited |
| Operational procedure | the recovery ladder, first responses, the rehearsal | Requel, as use cases — normative, just with a human operator as the actor |

The last row is the correction worth recording: operational material is not reference material. The
production guide is 25 pages of operator use cases and scenarios, and Requel held them with no new
machinery. What Requel needs is **one** new thing — a reference attachment that doubles as provenance
— not four.

The discriminator is the greenfield test: for a system that does not exist yet you would carry
citations and operational requirements, and you would not carry as-built or environment facts,
because those are outputs of building rather than inputs to it. With a sharp corollary: **the same
sentence changes category depending on whether the system exists.** "Must use AWS account
714494397341" is a constraint for greenfield and an observation for brownfield. An AI reviewer cannot
tell which without being told — which is not yet in the assistants epic and should be.

### Actor or stakeholder: a boundary test that held up

Devops configuring Entra is not an actor in the roundtable system, and the reason is not seniority or
team. **No roundtable use case contains a step they perform.** They configure a dependency; they are
an actor in Entra's world. Modelled here as the "Platform DevOps" stakeholder. The test generalises:
if you cannot name a use case whose scenario contains a step this person takes, they are a
stakeholder rather than an actor.

### What Requel handled well across all three sources

Worth recording, because failure logs are easier to write than success ones:

- **Glossary synonyms.** `canonicalTermId` modelled "dry-run event / test room / throwaway room" as
  one concept with two alternates, and echoed back `canonicalTermName`. The only finding in the whole
  exercise whose repair landed in the model rather than in a comment.
- **Correcting a definition in place.** Room and Archive were both defined wrongly from the tickets
  and both were flagged as inferred. When the guide contradicted them, the correction was two calls
  and the entries now carry what was wrong and against what. That is the living document working.
- **Scenario types.** `PreCondition / Primary / Optional / Alternative / Exception` was rich enough
  for everything three sources produced — a pointed contrast with `GoalRelationType`'s two values.
- **Shared steps.** One Step entity in two scenarios, which is exactly right for a recovery flow that
  shares its tail with the happy path.
- **Issues with positions.** Every finding above attached to the entity it concerns with a proposed
  repair beside it, which is the shape the whole AI-assistant epic is aiming at — arrived at by hand
  first, which is the right order.

## 9a. The second emission

`doc/pq-roundtable-emitted-CON-3685-v2.md` re-emits the ticket from the three-source model and diffs
it against v1. The result is the clearest evidence yet for what the living document is actually
worth, and where it falls short.

**Every material correction between v1 and v2 came from a new source, not from re-analysis.** Two
IVS channels instead of one; four room states instead of four different guessed ones; archive versus
purge; takeover key versus key rotation; 26/6 instead of 24/8; an admin gate that may be open in
production. None was inferable from the tickets by any amount of additional thinking. *The quality
of a living document is bounded by its sources.* For the assistants epic this argues that ingesting
another source outranks reviewing harder — a product-priority conclusion, not a technique one.

**v1 enshrined an error while correctly fixing a different one.** Its proposed wording for the
disposition goal was "fixed, closed with a written reason, or deferred with a rationale and a review
date". That resolved the parent/child vocabulary mismatch properly and simultaneously baked in
"deferred", which was never true. A reconciliation between two documents that agree with each other
cannot detect that both are wrong — the same failure as the arithmetic, in a different costume.

**Four emit-path gaps, now demonstrated rather than predicted:**

1. No single read returns project *content*. `getProjectTree` gives names and ids; a generator must
   walk it entity by entity or go via the XML export.
2. Provenance is still hand-carried, and it got worse with more sources — with four inputs the
   reader's first question is "says who?", and the model cannot answer.
3. Nothing ranks issues. The Blocking Check at the top of v2 exists because a human knew which
   finding outranked the rest.
4. The `ReportGenerator` hook is there and unused — the project auto-carries one ("HTML
   Specification", id 696). That is where a deterministic renderer belongs; both emissions were
   written by hand.

**What survived v1 → v2 unchanged** is probably the durable part: goals as conditions with
verification held separately as scenarios, the conjunctive ownership AC split into an outcome plus
its administrative record, the six-step AC extracted into a scenario, and an incidental property
promoted to a protected goal.

## 10. Next passes

- Use Case and Scenario Step, the types with no legacy assistant coverage at all.
- Stakeholder and Glossary Term.
- Fold the remaining production-guide detail into the model: the watch-page behaviours, the encoder
  validation panel, the pre-flight checklist as a scenario, and the archive/purge lifecycle.
- Decide whether the 32 review findings belong in this project at all, or whether a review finding is
  a different kind of thing from a requirement.
- Re-emit CON-3685 now that three sources are in the model, and compare against the first emission in
  `doc/pq-roundtable-emitted-CON-3685.md` — the first real test of the living document.
