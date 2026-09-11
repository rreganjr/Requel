# GoalRelationType — expand the vocabulary, and stop keeping two copies of it

Found while building the **PlatformQ Roundtable** project (`doc/pq-roundtable-case-study.md`):
`GoalRelationType` has exactly two values, `Supports` and `Conflicts`, and two of the most valuable
findings from that exercise have no relation type to express their repair.

## What exists today

```java
public enum GoalRelationType { Supports, Conflicts; }
```

- Persisted `@Enumerated(EnumType.STRING)` on `GoalRelationImpl.getRelationType()`, so **adding
  values needs no migration** and carries no ordinal-remapping risk.
- Marshalled to XML by name via `GoalRelationImpl.GoalRelationTypeAdapter`.
- `EditGoalRelationCommandImpl` resolves the incoming string with `GoalRelationType.valueOf(...)`.
- **Nothing in main reasons over the value.** There is no branch anywhere on `Supports` vs
  `Conflicts`; it is stored, exported and displayed. So new values cost no behaviour.
- `EditGoalRelationInput.relationType` is a plain `@NotBlank String`, documented in its javadoc as
  `"Supports" or "Conflicts"`.

## The gap, concretely

Two findings from the case study that the model cannot hold:

- **`PROXY_FOR_OUTCOME`.** "Conduit is the named owner" is a checkable proxy that can be fully true
  while the actual intent (bus factor above one) fails. The relationship between the proxy and the
  outcome is the finding, and there is no type for it.
- **`DUPLICATE_AT_DIFFERENT_ABSTRACTION`.** "Chris is no longer a required participant" and "two
  engineers have run it unassisted" have the same success condition. During the build the only
  available approximation was `Supports`, which is true and loses the point entirely.

An assistant can raise either as prose in an issue. Neither can be *repaired* structurally, which
means the model never gets better — the finding stays a comment forever.

## Proposed vocabulary

Seven values, each with a referent in something the case study actually found:

| Value | Meaning | Directed? |
| --- | --- | --- |
| `Supports` | from has a positive influence on the success of to | yes |
| `Conflicts` | from and to cannot both be satisfied | symmetric |
| `Refines` | from is a more concrete statement of to | yes |
| `Duplicates` | from and to state the same requirement | symmetric |
| `DependsOn` | from cannot be achieved until to is | yes |
| `Obstructs` | from makes to harder without making it unsatisfiable | yes |
| `Measures` | from is the measurable proxy for the outcome to | yes |

`Refines` and `Duplicates` are what the abstraction finding needed — which of the two applies is
exactly the judgement a reviewer should be asked to make. `Measures` turns `PROXY_FOR_OUTCOME` from a
complaint into a modelling move: state the real outcome as a goal, and relate the checkable proxy to
it. `Obstructs` is the honest type for the CI-gate case, where one requirement does not contradict
another but does undermine it.

This set is conventional in goal modelling (i*, KAOS, GRL all carry some form of contribution,
conflict, decomposition and dependency), so it is unlikely to need a second expansion soon.

## Should relation types be data-driven?

No — and the reasoning is the opposite of the assistant-definitions case in
`doc/ai-per-type-assistants-epic.md`, which is worth spelling out because the two look alike.

A prompt is content: it varies by project, by domain, by house style, and nothing else depends on
two projects phrasing it the same way. A relation type is **model semantics**. It is a small closed
vocabulary that everyone reading the model must interpret identically, and future analysis will
reason over it — a corpus assistant asking "is this `Conflicts` pair genuinely in conflict?" needs
`Conflicts` to mean one thing. If project A's `Refines` differs from project B's, cross-project
comparison and any relation-aware analysis break, quietly.

Same principle as keeping the AI output schema in code while the instructions become data: the part
everyone must agree on stays fixed. A project that needs its own relationship semantics has **tags**
for that.

## What "easily expandable" should actually mean

Not runtime-editable. One source of truth, so adding a value is a single change:

1. **The vocabulary is currently duplicated in the frontend.** `goal-editor.ts` hardcodes
   `relationTypeOptions = [{ label: 'Supports', … }, { label: 'Conflicts', … }]`, and
   `models/goal.ts` declares `relationType: 'Supports' | 'Conflicts'`. Adding a value today means
   editing the enum *and* two Angular files, and forgetting either leaves a type the UI cannot
   produce or cannot narrow. **Serve the vocabulary from the server** (a small read endpoint
   returning value, label, description, and whether it is symmetric) and have the editor render from
   it. Then the enum is the only place.
2. **Attach the metadata to the enum**, not to the UI: display label, description, symmetric or
   directed, and the inverse label for rendering the reverse direction (`relationsToThisGoal`
   currently renders the same word in both directions, which reads wrong for anything directed).
3. **Give `valueOf` a friendly failure.** An unrecognised `relationType` currently surfaces as an
   `IllegalArgumentException`; it should be a field-level validation error naming the permitted
   values, which the served vocabulary makes easy.
4. **Decide symmetry handling.** `Conflicts` and `Duplicates` are symmetric; the model stores a
   direction regardless. Either normalise on write or teach the UI to render symmetric types without
   an arrow. Doing neither means the same fact can be entered twice, in both directions, and both
   will display.

## Compatibility notes

- **Database:** none needed — `EnumType.STRING`.
- **XML export/import:** new names export fine and import fine into a build that knows them. An
  *older* Requel importing a newer export fails at `valueOf`. If old-version import matters, the
  JAXB adapter should fall back rather than throw.
- **MCP:** `relationType` is a free string in `EditGoalRelationInput`, so the typed tool schema does
  not enumerate the values. Worth having the generated schema carry them once the vocabulary is
  served, so a client gets the list without guessing.
- **Existing data:** unaffected; `Supports` and `Conflicts` keep their meaning.

## Suggested ticket shape

One ticket: add the five values with metadata, serve the vocabulary from a read endpoint, consume it
in `goal-editor.ts`, delete the hardcoded union in `models/goal.ts`, friendly validation error, and a
decision recorded on symmetry. Small, self-contained, and it unblocks the structural repairs that the
per-type Goal assistant will want to propose.
