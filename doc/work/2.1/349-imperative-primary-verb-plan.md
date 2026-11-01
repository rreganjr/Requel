# #349 — Primary verb for commands; tell a step with no actor from one that couldn't be parsed

https://github.com/rreganjr/Requel/issues/349

Branch `349-imperative-primary-verb` from `release/2.1`, PR to `release/2.1`. Size 2.

## Summary

The NLP pipeline takes a sentence's primary verb to be the governor of its subject, so a
command ("Create the room") has none. The step-structure check then can't tell a step that
doesn't say who does it from text it couldn't read, and posts the same "could not analyze" note
for both. Scenarios are steps (`Scenario extends Step`), so every scenario named as a command
gets the note too. This gives commands a primary verb, marks them imperative, and has the step
check apply the step and scenario writing rules below.

## Locked decisions (2026-10-31)

1. **A command gets a primary verb.** When a sentence has no subject relation and the root of its
   dependency parse is a base-form verb (`VB`), or a `VBP` that is the sentence's first word
   (the parser sometimes tags a leading command verb that way), `DependencyPrimaryVerbFinder` uses
   the root as the primary verb and marks the sentence imperative. A new `NLPText.isImperative()`
   carries the mark. It is analysis state only and is never persisted. A word the parser tags as a
   noun stays unparsed (a parser mistake, out of scope).
2. **The writing rules.** A use case and its scenarios are meant to be well structured; a less
   formal description is a story.
   - A **step** reads "&lt;actor&gt; does something" or "the system does something". The system is
     an implicit actor: a step where it responds to a user action.
   - A **scenario name** reads "&lt;actor&gt; does something" or is a command (its primary
     scenario is usually named after the use case's goal phrase: 7 of 8 on the roundtable project).
     A sub-scenario follows the scenario rule: ideally actor-initiated, but a command is fine.

   | Text | Scenario name | Step |
   |---|---|---|
   | "&lt;actor&gt; does …", a known actor | no finding; the actor-in-use-case check as today | same |
   | "&lt;actor&gt; does …", not a known actor | the "subject doesn't match a known actor" issue as today | same |
   | "The system does …" | no finding | no finding |
   | A command | no finding | LOW "doesn't say who does it" |
   | Neither (no verb found) | LOW "doesn't read as …" | LOW "doesn't read as …" |

3. **"The system" is a subject without an actor.** "system" or "the system", ignoring case, is
   accepted as the subject with no actor, so it gets no "subject doesn't match" issue and no
   actor-in-use-case check. A product or project name ("Roundtable starts the stream") is not
   treated as the system; it is checked like any other subject.
4. **The new findings are advisory issues**: severity LOW, `mustBeResolved` false, each with a
   "Do nothing." position, like the step check's other issues. Open Issues' LOW filter (#338)
   hides them.
   - Command step: `The step "Start the livestream" doesn't say who does it. Name the actor, for
     example "The host starts the livestream", or "The system …" when the system responds.`
   - Neither form: `Requel couldn't read "<text>" as "<actor> does something" or a command. A
     step names who does it; a scenario name can also be a command.` (The scenario wording drops
     the step sentence.)
5. **The could-not-analyze note goes away.** The "neither form" issue replaces it for steps and
   scenarios. A sentence the parser gets wrong becomes a LOW issue rather than a note; its
   hedged wording and "Do nothing." cover that.
6. **Alternating actor and system steps is a follow-on**, filed on epic #267. It is a check on a
   whole scenario, not on one step.
7. **Test text stays generic.** The bracketed-note example is written as `[Ingested verbatim from
   the source, criterion 1.]`, without the work ticket key the issue quotes.

## Changes

nlp
- `NLPText` / `NLPTextImpl`: `isImperative()` and a setter; transient analysis state.
- `DependencyPrimaryVerbFinder.process`: when no subject relation is found, look for the `ROOT`
  relation's dependent. If it is `VB`, or a `VBP` at the first word, set it as the primary verb
  and mark the sentence imperative. The existing subject path and its mistag fix are unchanged;
  text with neither still gets no primary verb (the DEBUG log stays).
- `SemanticRoleLabeler`: roles relative to the root verb of a command. With no subject there is
  no AGENT; the PATIENT and other roles come out as for a full sentence. Checked by test, and
  changed only if it trips on the missing subject.

assistant-legacy-nlp `StepStructureAssistant`
- Decide the target kind: a `Scenario` follows the scenario rule, any other `Step` the step rule.
- After processing the name:
  - AGENT present: `system` / `the system` → nothing; a known actor → the use-case check; an
    unknown subject → the existing must-resolve issue.
  - No AGENT and `isImperative()`: a step gets the LOW command-step issue; a scenario nothing.
  - No primary verb: the LOW "doesn't read as" issue (replacing the could-not-analyze note).
- Issue keys stay `<assistant>:<type>:<id>:<findingType>` so re-runs update in place;
  finding types `no-actor` and `unreadable`. Old could-not-analyze notes are no longer reported,
  so the next analysis marks them stale (#270) and its cleanup retires them.
- Class Javadoc rewritten for the rules.

## Test plan

`tmp/349-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` + the coverage check.
(No Angular change.)

- `NLPTests` (real parser):
  - `processText("Create the room")`: primary verb "Create", imperative.
  - "Start the livestream", "Hand the stream key to Zoom": imperative with their verbs.
  - "The host creates the room": primary verb "creates", not imperative.
  - "Webinar" and `[Ingested verbatim from the source, criterion 1.]`: no primary verb, not
    imperative.
- `SemanticRoleLabelerTests`: "Create the room" has no AGENT and "room" as PATIENT.
- `StepStructureAssistantIT` (new, real NLP, a project with an actor "Host" and a use case that
  uses the scenario):
  - Step "Start the livestream" → the LOW no-actor issue with "Do nothing.", not mustResolve, and
    no could-not-analyze note.
  - Step "The host starts the livestream" → nothing new.
  - Step "The system starts the livestream" → nothing.
  - Step "Webinar" → the LOW unreadable issue.
  - Step "The producer starts the stream" (no such actor) → the existing must-resolve issue.
  - Scenario "Create the room" → nothing.
  - Scenario "Webinar dry run" → the LOW unreadable issue.
  - Scenario "The host runs the session" → as a step with a known actor.
- `StepStructureAssistantTest`: the identity checks it has, plus the system-subject matcher.

Roundtable check (you run it): the could-not-analyze count in project 621 before and after a
re-run, posted on the issue. The plan's final step gives the SQL.

## Out of scope

- Parser mistakes on full sentences (the issue's example), beyond the hedged LOW issue.
- The lexical checks (#268).
- Alternating actor/system steps (decision 6).
- Treating a product or project name as the system.

## Risks

- **Tagger variance.** Short command titles are where the tagger is least reliable; the tests
  pin the issue's examples, and a miss falls back to the LOW unreadable issue, not a crash.
- **More visible findings.** On roundtable, 14 of 15 steps are commands and become LOW issues
  instead of notes. That is the rule working; the #338 filter hides LOW.

## AC mapping

| AC | Covered by |
|---|---|
| "Create the room" has primary verb "Create", imperative; "The host creates the room" not; "Webinar" none | `NLPTests` |
| "Start the livestream" step: no-actor finding, no could-not-analyze; "The host starts the livestream" nothing new; bracketed note no no-actor finding | `StepStructureAssistantIT`, `NLPTests` |
| A scenario named "Create the room" gets what the review decided, pinned by a test | Decision 2; `StepStructureAssistantIT` |
| Roundtable could-not-analyze count before and after, on the issue | Your re-run |
