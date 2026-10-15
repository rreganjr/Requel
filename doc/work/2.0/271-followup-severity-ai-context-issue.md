Follow-on to #271, which adds `LOW | MEDIUM | HIGH` severity to issues but deliberately leaves it out of what the AI assistants see.

### What exists after #271

The entity context pack that `RequirementsReviewAssistant` sends lists the entity's existing annotations as `AnnotationSnapshot` (`EntityContextPackBuilder`), and `IssueContextPackBuilder` builds `IssueSnapshot`. Both carry `mustBeResolved` and `resolved` but no severity. So the model is told which issues already exist, "for awareness only", without knowing which ones matter.

The pack also has an annotation budget: once `count` reaches the limit, the rest are dropped. The builder takes annotations in the entity's own order, so on a noisy entity the dropped ones can be `HIGH` findings while `LOW` lexical output survives.

### Work

- Add `severity` to `AnnotationSnapshot` (issues only; null for notes) and `IssueSnapshot`.
- When the annotation budget truncates, keep the highest severity first (rank descending, then the current order), so a cut drops `LOW` before `HIGH`.
- Tell the review prompt what the field means, and that an existing `HIGH` issue on the same point is a reason not to raise a duplicate. Bump the prompt/context version if the pack format is versioned.

### AC

- An issue's severity appears in the entity context pack and the issue context pack.
- With more annotations than the budget allows, every `HIGH` issue is kept before any `LOW` one.
- Notes carry no severity in the pack.
- Existing context-pack tests are updated, and a truncation test covers the ordering.

### Not in scope

- Letting the AI change an existing issue's severity.
- Severity for the legacy NLP assistants (#268).
