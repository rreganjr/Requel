# #459 — Step with no actor: an example built from the step, and one-click rewrites

https://github.com/rreganjr/Requel/issues/459

Branch `459-no-actor-step-rewrites` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

#349 raises a LOW `no-actor` issue on a step written as a command ("Download the resulting MP4").
Every one of these issues gives the same example, "The host starts the livestream". This builds
the example from the step itself, with the actor from its use case, and adds a position for each
rewrite. Accepting one sets the step's name to the rewrite and resolves the issue.

## Locked decisions (2026-11-02)

1. **Which actor.** Each use case has one primary actor. The candidates are the primary actors of
   the use cases that use the step, found by walking up through the scenarios that use it (the
   walk the actor-in-use-case check does).
   - **One candidate:** the message names it, and there is one actor rewrite.
   - **Unclear:** several use cases with different primary actors, or no primary actor in any of
     them while they have secondary actors. The message says the actor is unclear and lists the
     candidates. There is one rewrite position per candidate: the primary actors when there are
     several, otherwise the secondary actors. All of them are offered, with no cap.
   - **No use case, or no actors at all:** the example uses `<actor>`, and only the system
     rewrite is offered.
   - The system rewrite ("The system downloads the resulting MP4") is always offered.
2. **A stale rewrite is refused.** A new `RewriteNamePosition` stores the proposed name in the
   existing `proposed_word` column, and the name it was built from in a new nullable
   `expected_name` column (`V42`). Accepting it is refused, on `positionId`, when the step's
   name no longer equals `expected_name`. Without the check, a step that stopped being a command
   would keep an open, stale issue whose rewrite could overwrite the name the user already fixed.
3. **Re-runs replace the positions.** Rewrite positions are keyed by actor
   (`<issueKey>:rewrite:<actor name>`, `<issueKey>:rewrite:system`).
   - A re-run finds an existing position by its key and updates it in place: text, proposed name
     and expected name. A position used to be found by its text, so a changed name would have
     added a second set.
   - When a re-run re-reports an issue but not one of its positions (an actor that no longer
     applies), that position is deleted, if nobody has argued on it. A position used to be left
     in place as superseded. This applies to every assistant.
4. **The verb isn't the first word** ("Then download the file"). The actor goes just before the
   verb, and the words in front are kept: "Then Host downloads the file".
5. **A scenario used as a step gets no rewrites.** #349 raises nothing on a scenario named as a
   command.

## Changes

assistant-legacy-nlp
- `StepVerbs` (new): `thirdPerson(String verb)`.
  - "-es" after s, x, z, ch, sh or o.
  - A consonant plus "y" becomes "-ies".
  - Irregulars: be → is, have → has (do and go are covered by "-o").
  - A hyphenated verb changes its last part: "Force-stop" → "force-stops".
  - The first letter is lowercased unless the verb is an acronym.
- `StepVerbs.rewrite(name, verbText, subject)`: the name with `<subject> <verb-s>` in place of
  the verb. The words before the verb and after it stay as written. Returns null when the verb
  isn't found in the name as a whole word.
- `StepStructureAssistant`:
  - Builds the candidates (decision 1) and the message:
    - one actor: `The step "{0}" doesn't say who does it. Name the actor, for example "{1}", or
      "{2}" when the system does it.`
    - unclear: `The step "{0}" doesn't say who does it, and its use cases don't settle the actor
      ({3}). Name the actor, for example "{1}", or "{2}" when the system does it.`
    - none: the example with `<actor>`.
  - One `REWRITE_NAME` position per rewrite (`Rewrite as "{0}".`), carrying `proposedName` and
    `expectedName`, next to "Do nothing.".
  - A rewrite over 255 characters (the name limit) isn't offered.
  - The actor's name is used as written, with no "The"; "The system" keeps its article.

annotation-domain / annotation-jpa
- `RewriteNamePosition` (new entity, single-table, discriminator
  `com.rreganjr.requel.annotation.impl.RewriteNamePosition`): `proposedName` (column
  `proposed_word`, as `ChangeSpellingPosition` maps it) and `expectedName` (column
  `expected_name`). JAXB `rewriteNamePosition`, attributes `proposedName` and `expectedName`.
- `EditRewriteNamePositionCommand` (new), with a factory method.
- `ResolveIssueWithRewriteNamePositionCommandImpl` (new), registered for `RewriteNamePosition`
  as `ChangeSpellingPosition` is.
  - For each annotatable on the issue: read the Name through the text-editor registry. If it
    isn't `expectedName`, refuse with a validation error on `positionId`.
  - Then edit the name through the registry's edit command (authorized as Fix Spelling is), and
    resolve.

assistant-core
- `CommandBackedAssistantResultApplicator`:
  - `REWRITE_NAME` positions go through `EditRewriteNamePositionCommand`.
  - `applyPosition` finds an existing position by action key before falling back to its text.
  - `reconcileStaleFindings`: a stale position finding whose parent issue this run re-reported
    is deleted, if untouched (no human arguments), and marked `AUTO_RESOLVED`. Otherwise the
    policy applies as now.

requel-app
- `V42__positions_expected_name.sql`: `ALTER TABLE positions ADD COLUMN expected_name
  varchar(255) NULL`.

project-jpa
- Export: `RewriteNamePosition` joins the JAXB classes. Import: `ProjectPositionAssembler` gets
  a `rewriteNamePosition` case. `doc/samples/project.xsd` gets the element.

service-impl / service-api
- `AnnotationCommandRegistrar` registers `EditRewriteNamePosition`. `GatewayPolicyConfig.DENIED`
  gets it next to `EditChangeSpellingPosition` (assistant-internal).
- `ResolveIssueInput`'s description and authorization name `RewriteNamePosition`.
- `PositionDto`'s comment lists the new type.

requel-angular
- `annotations-section.ts`: `resolveLabel('RewriteNamePosition')` → "Rewrite".

## Steps

1. Branch.
2. `StepVerbs` and its unit test.
3. Entity, migration, edit and resolve commands, factory registration.
4. Applicator: the new kind, lookup by key, deleting dropped positions.
5. `StepStructureAssistant`: candidates, message, positions.
6. Export/import, xsd, gateway, DTO text, Angular label.
7. Tests below; `tmp/459-verify.sh`.

## Test plan

`tmp/459-verify.sh`: backend `mvn clean verify -pl modules/coverage-report -am`, the coverage
check, the Angular typechecks and unit tests.

- `StepVerbsTest`: download → downloads, issue → issues, push → pushes, fix → fixes, go → goes,
  carry → carries, play → plays, be → is, have → has, Force-stop → force-stops, an acronym kept;
  `rewrite` with a leading word ("Then download the file"), with the verb missing (null).
- `StepStructureAssistantTest`:
  - one primary actor: the message and three positions (Host, system, Do nothing);
  - two use cases with different primary actors: the unclear message and a rewrite for each;
  - no primary actor, two secondary actors: a rewrite for each secondary;
  - no use case: `<actor>`, system rewrite only;
  - a rewrite over 255 characters is skipped;
  - a scenario named as a command: nothing.
- `StepStructureAssistantIT` (real NLP):
  - the AC steps: "Download the resulting MP4", "Force-stop the active stream" → "Host
    force-stops the active stream", "Issue a new takeover key" → "Host issues a new takeover key";
  - accepting the Host rewrite sets the name and resolves the issue, and a re-run raises
    nothing on the step;
  - a re-run of an unchanged step keeps the same issue and positions (ids unchanged, no
    duplicates);
  - renaming the step to another command updates the positions in place;
  - changing the use case's primary actor deletes the old actor's position and adds the new one;
  - accepting a rewrite after the name changed is refused, and the name is unchanged.
- `RewriteNamePositionIT` or the applicator test: a position somebody argued on is kept when
  dropped.
- Round trip: an exported project with a `RewriteNamePosition` imports with both fields.
- `V42` runs in the MySQL migration ITs.
- Angular: `annotations-section.spec.ts`, the label.

## Out of scope

- Which steps count as commands (#349).
- Alternating actor and system steps (the other #349 follow-on).
- Rewrites for the other step-structure findings.

## Risks

- **Hyphenated verbs.** The tagger may give "stop" rather than "Force-stop" as the verb. `rewrite`
  widens the match to the whole hyphenated word around it.
- **Deleting dropped positions** now applies to every assistant's positions under a re-reported
  issue. The spelling assistant keys its change-spelling positions by the suggested word, so a
  suggestion that drops out is deleted instead of left behind. That is the intent, and the
  untouched check keeps anything a person argued on.
- **Existing `no-actor` issues** from #349 keep their key. The next run updates them in place and
  adds the rewrites.

## AC mapping

| AC | Covered by |
|---|---|
| "Download the resulting MP4" with primary actor Host: both examples, three positions | StepStructureAssistantTest, StepStructureAssistantIT |
| "Force-stop …" / "Issue …" rewrites | StepVerbsTest, StepStructureAssistantIT |
| No use case: `<actor>`, system rewrite only | StepStructureAssistantTest |
| Accepting the Host rewrite renames the step; a re-run raises nothing | StepStructureAssistantIT |
| Re-runs update in place, no duplicate positions | StepStructureAssistantIT |
