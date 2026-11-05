# #472 — Offer to merge two steps that #455 flags as one actor's or the system's

https://github.com/rreganjr/Requel/issues/472

Branch `472-merge-steps` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

#455 raises an advisory issue on a scenario when two steps in a row are by the same actor, or are
both system steps. Its only position is "Do nothing.". This adds a position per mergeable pair:
`Merge into "Member fills in the form and submits it".`. Accepting it does two things in that
scenario: the first step takes the merged name, and the second step is removed. This follows
#459's one-click rewrite: a new position type, a guard against stale names, a resolve command,
and export/import.

## Locked decisions (2026-11-05)

1. **The first step is shared with another scenario:** no merge is offered. Renaming it would
   rename it in every scenario, and forking it would leave its issues on the old step.
   "Do nothing." still covers the break. The resolve command refuses too, in case the step
   became shared after the position was made.
2. **The second step** is removed from this scenario, and deleted when no other scenario uses it
   (#325's rule for a step dropped by `EditScenario`).
3. **Building the sentence:**
   - The first step must read "<subject> …"; its name is used as written, without a trailing
     period.
   - The second step either reads "<subject> …", and loses its subject ("Member submits it" →
     "submits it"), or is a command, and is conjugated as #459 does ("Submit the form" → "submits
     the form").
   - The merged name is `<first> and <rest>`.
   - Anything else gets no merge position: an unreadable second step, the subject not at the
     start of the name, a command as the first step, or a result over 255 characters.
4. **Two system steps** are merged the same way ("The system saves the loan and shows the new due
   date").
5. **Stale guard.** Accepting is refused, on `positionId`, unless the scenario still has a step
   named as the first was, immediately followed by one named as the second was. Step names are
   unique in a project, so the names identify the steps. The first step must also still be used
   only by this scenario.
6. **Several breaks** each get their own merge position, all on the scenario's one #455 issue.
   Accepting one resolves the issue, and the edit re-runs the scenario's analysis.
7. **The issue is keyed by the breaks it lists** (decided 2026-11-05, during implementation).
   #455 keyed the issue by the scenario alone, so a re-run wrote any breaks left after a merge
   onto the resolved issue, where they never showed. The key is now
   `legacy-step-sequence:Scenario:<id>:sequence:<digest>`, where the digest is the first 12 hex
   digits of a SHA-256 of the break list.
   - An unchanged scenario keeps its issue.
   - A different set of breaks raises a new issue, and an untouched old one is removed as before.
   - "Do nothing." still silences exactly the breaks it was chosen for.

## Design

### The position

`MergeStepsPosition` (new) lives in project-jpa, beside `AddActorPosition`, because resolving it
edits a scenario. Its discriminator is `com.rreganjr.requel.project.impl.MergeStepsPosition` and
its JAXB name is `mergeStepsPosition`. It has three fields:

| Field | Column | Meaning |
|---|---|---|
| `proposedName` | `proposed_word` (as #459) | the merged name |
| `expectedName` | `expected_name` (V42) | the first step's name when the position was made |
| `expectedSecondName` | `expected_second_name` (new, V43) | the second step's name then |

### Offering it

`StepSequenceAssistant` builds the merge for each same-actor or two-system break, following
decisions 1, 3 and 4. Each merge becomes a `CREATE_OR_UPDATE_POSITION` action with:
- `kind` `MERGE_STEPS`;
- `proposedName`, `expectedName` and `expectedSecondName`;
- text `Merge into "{0}".`;
- key `<issueKey>:merge:<first step id>:<second step id>`.

The positions come before "Do nothing.".

`StepReading` already gives the subject text and a command's verb. A new
`StepVerbs.predicate(name, reading)` returns the part after the subject, or the conjugated command,
or null when the step doesn't qualify.

### Applying it

`CommandBackedAssistantResultApplicator` sends `MERGE_STEPS` to
`ProjectCommandFactory.newEditMergeStepsPositionCommand()`, alongside the existing
project-specific positions. `EditMergeStepsPositionCommandImpl` (project-jpa) works like #459's
`EditRewriteNamePositionCommandImpl`:
- With no position set, it looks for an existing `MergeStepsPosition` on the issue with the same
  proposed name, so a re-run updates it in place.
- It sets the three fields.
- It attaches the position to the issue.

#459's pruning removes a merge a re-run no longer offers, if nobody argued on it.

### Resolving it

`ResolveIssueWithMergeStepsPositionCommandImpl` (project-jpa) is registered in
`PositionCommandRegistration`. On the issue's scenario (or the given annotatable, which must be a
scenario), it:

1. Applies the guard in decision 5, and refuses with a validation error on `positionId` that says
   what changed.
2. Runs an `EditScenario` for the scenario with every step except the second. The first step's
   command carries the merged name. Existing steps and sub-scenarios are passed through by id,
   as `ProjectCommandRegistrar` does for the API.
   - **Authorization:** `EditScenario` authorizes itself as Scenario[Edit], and the resolve needs
     Annotation[Edit], as every resolve does.
   - **Name clash:** if the merged name belongs to another step, the step-name guard (#254)
     refuses it.
   - **The dropped step:** `EditScenario` deletes it when no other scenario uses it.
3. Resolves the issue with the position (`super.execute()`).

`EditScenario` is the analysis target, so the scenario's post-edit run (including #455's check)
follows as usual.

## Changes

project-domain
- `EditMergeStepsPositionCommand` (new) and `ProjectCommandFactory.newEditMergeStepsPositionCommand()`.

project-jpa
- `MergeStepsPosition`, `EditMergeStepsPositionCommandImpl`,
  `ResolveIssueWithMergeStepsPositionCommandImpl`; `PositionCommandRegistration` registers the
  resolver.
- Export: `MergeStepsPosition` joins the JAXB classes. Import: `ProjectPositionAssembler` gets a
  `mergeStepsPosition` case.

assistant-legacy-nlp
- `StepVerbs.predicate`; `StepSequenceAssistant` offers the merges.

assistant-core
- Applicator: the `MERGE_STEPS` kind.

requel-app
- `V43__positions_expected_second_name.sql`:
  `ALTER TABLE positions ADD COLUMN expected_second_name varchar(255) NULL`.

service-impl / service-api
- `ProjectCommandRegistrar` registers `EditMergeStepsPosition`, and `GatewayPolicyConfig.DENIED`
  adds it (it's assistant-internal, like `EditAddActorToProjectPosition`).
- `ResolveIssueInput` and `PositionDto` mention the new type.

Schema
- `doc/samples/project.xsd` and `website/integration/2.1/project.xsd` (kept identical) get the
  `mergeStepsPosition` element and type. 2.1 isn't released, so its published schema can still
  change.

requel-angular
- `annotations-section.ts`: `resolveLabel('MergeStepsPosition')` → "Merge".

website
- `guide/issues.html`: the "Actor and system turns" row mentions the merge.

## Steps

1. Entity, V43, the edit and resolve commands, registration.
2. `StepVerbs.predicate`, the assistant's merge positions.
3. Applicator, gateway, export/import, both XSDs, Angular label, guide.
4. Tests; `tmp/472-verify.sh`.

## Test plan

`tmp/472-verify.sh` runs:
- the backend `mvn clean verify -pl modules/coverage-report -am` and the coverage-modules check;
- the Angular typechecks and `annotations-section.spec.ts`.

- **`StepVerbsTest.predicate`:**
  - "Member submits it" → "submits it";
  - "The system shows the date" → "shows the date";
  - "Submit the form" → "submits the form";
  - a subject not at the start → null.
- **`StepSequenceAssistantTest`:**
  - a same-actor pair gets a merge position, with its metadata, before "Do nothing.";
  - a two-system pair does too;
  - a command as the second step is conjugated;
  - no merge when:
    - the first step is a command;
    - the second is unreadable;
    - the first step is shared;
    - the result is over 255 characters;
  - two breaks get two merge positions;
  - "starts with a system step" gets none;
  - the issue key is the same for the same breaks and different for different ones.
- **`StepSequenceAssistantIT` (real parser):**
  - **The AC:** "Member fills in the form" + "Member submits it" is offered "Merge into "Member
    fills in the form and submits it"."; accepting it:
    - leaves one step with that name;
    - removes the second;
    - deletes it, since nothing else uses it;
    - resolves the issue;
    - and a re-run raises nothing on the scenario.
  - **Stale:** accepting after either step was renamed, or after the steps were reordered, is
    refused, and nothing changes.
  - **Shared second step:** a merge in one scenario leaves another scenario that uses the
    second step unchanged, and the step still exists.
  - **First step shared after the position was made:** the merge is refused.
  - **Re-run:** an unchanged scenario keeps the same merge position (same id).
  - **Remaining break:** with two breaks, accepting one merge resolves the issue, and the re-run
    raises the other on a new open issue.
- **Round trip:** a project exported with a `MergeStepsPosition` imports with its three fields.
- **Migration:** `V43` runs in the MySQL migration ITs.
- **Angular:** `annotations-section.spec.ts` checks the "Merge" label.

## Out of scope

- Which pairs #455 flags.
- Merging more than two steps at once (accepting merges in turn covers a run of three).
- Rewriting pronouns ("submits it" keeps its "it").

## Risks

- **Clumsy sentences.** "Member fills in the form and submits it" reads well; a long first step
  makes a long merged one. The position shows the full text before anyone accepts it, and "Do
  nothing." stays.
- **A changed set of breaks is a new issue (decision 7).** An issue somebody argued on stays,
  superseded, beside the new one, as other assistants' findings do.

## AC mapping

| AC | Covered by |
|---|---|
| The merge position on the #455 issue; accepting leaves one step, resolves, re-run raises nothing | StepSequenceAssistantIT |
| Refused after a rename or reorder; nothing changes | StepSequenceAssistantIT |
| Other scenarios using the second step are unchanged | StepSequenceAssistantIT |
| Export/import keeps a merge position | ProjectXmlStreamingRoundTripIT |
