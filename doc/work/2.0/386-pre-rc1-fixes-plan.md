# #386 Pre-rc1 fixes — implementation plan

Child of the Release 2.0.0 epic #375. Found while writing the user guide (#377); fixed before
tagging `2.0.0-rc1` so #378 tests a clean candidate.

## 1. Deleting the imported sample project fails

**Cause.** `JpaAnnotationRepository.removeAnnotatableFromAnnotationJoinTable` deleted the
`annotation_annotatable` row by `annotation_id` and `annotatable_id` only. `annotatable_id` is
shared across entity types, so unlinking a note from use case 1 also unlinked it from story 1,
and from use case 2 also from step 2. With its registry rows gone the note looked unused and
`RemoveAnnotationFromAnnotatableCommandImpl` deleted it, while `scenarios_annotations` (or, on
H2, `actors_annotations`) still referenced it: the foreign-key failure. The sample hits it
because several notes sit on a use case, a story and a step whose ids coincide.

**Fix.** The method takes the annotatable instead of its id and restricts the delete by
`annotatable_type`, using the same discriminator set as `unlinkAllAnnotations` (#247).

**Tests** (`DeleteProjectIT`):

- `unlinkingAnAnnotationKeepsAnotherTypesLinkWithTheSameId`: a note on a goal plus a `Story`
  registry row carrying the goal's id; unlinking the goal leaves the Story row.
- `importedSampleProjectDeletes`: import `doc/samples/Requel.xml`, delete it.

Both fail without the fix.

## 1b. Deleting anything a glossary term refers to fails (found while fixing 1)

With 1 fixed, deleting the sample *after re-running analysis* still failed:
`Unable to locate parameter terms_referers.referer_id for RESTRICT - DELETE`. Analysis links
glossary terms to the goals and stories that use them (`terms_referers`, a `@ManyToAny` set),
and Hibernate fails whenever an element is removed from such a set. So these all failed once a
term referred to the item: deleting a goal, story, actor, use case, scenario, step or
stakeholder; deleting the term; setting a term's referers; replacing a term with its canonical
one; deleting the project. Same for a stakeholder's goals (`goals_goalcontainers`).

**Fix.** The same workaround the goal/actor/story containers already use: delete the join rows
natively, then refresh.

- `ProjectRepository.removeGlossaryTermReferer(term, referer)` (null term: every term) and
  `removeAllGlossaryTermReferers(term)`, used by the seven delete commands,
  `DeleteGlossaryTerm`, `EditGlossaryTerm` and `ReplaceGlossaryTerm`.
- `DeleteGlossaryTerm` also removes the term from its referers' `getGlossaryTerms()`, whose
  join tables (`goals_glossary_terms`, ...) reference it by key.
- `DeleteStakeholder` unlinks its goals with `removeGoalContainerFromGoalJoinTable`.
- `ReplaceGlossaryTerm` iterated the referers while removing from them, and its actor branch
  tested `instanceof Story` twice, so actors threw; both fixed.

## 1c. The container join-table removals had the same type-less delete as 1

`removeGoalContainerFromGoalJoinTable`, `removeActorContainerFromActorJoinTable` and
`removeStoryContainerFromStoryJoinTable` deleted by both ids only, so removing a goal from story
5 also dropped it from actor 5's referers. They now take the container's discriminator (the
`Add*ToContainer` commands' helpers, now package-visible). Stored values were checked on 1.2
and 2.0 databases: always the full interface name, which the helpers produce.

**Tests** (`DeleteProjectIT`): `itemsAndTermsLinkedByGlossaryRefererDelete` (delete a goal and
a story a term refers to, re-point the term's referers, delete the term, delete the project)
and `changingPrimaryActorAndDeletingAStakeholderWithGoals`. Both failed before.

## 2. A hand-written position's resolve button said "Ignore"

`annotations-section.ts` `resolveLabel()` now returns "Resolve" for a plain position (and any
unknown type); `IgnorePosition` keeps "Ignore". Unit spec and the e2e resolve test assert it;
the guide's issues page names the button.

## 3. Re-running analysis on the imported sample added a second "Ignore this word."

The sample's spelling issues shared one plain `<position>` "Ignore this word." (`POS_12`, the
2009 assistant's form). The lexical assistant reuses an existing `IgnorePosition` with the same
text in the same project, so a plain one was never matched and a second was added. `POS_12` is
now an `<ignorePosition>`. Checked on MySQL: after import every spelling issue has one ignore
position, and after re-running analysis still one (76 issues).

Older 1.x exports carry the same plain position; they show two after re-analysis until one is
resolved. Not converted on import: matching on text would be a guess.

## Test plan

- `DeleteProjectIT` (four new tests), `ProjectXmlStreamingRoundTripIT`,
  `ImportProjectStreamingCommandTest`, `annotations-section.spec.ts`.
- By hand on MySQL 8.4: the imported sample deletes, before and after re-running analysis;
  re-analysis leaves one ignore position.
- `mvn clean verify`; e2e in CI.
