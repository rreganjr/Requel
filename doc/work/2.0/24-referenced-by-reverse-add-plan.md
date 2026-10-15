# #24 — Add referring entities from a "Referenced By" section

Ticket: [#24](https://github.com/rreganjr/Requel/issues/24) (rewritten from a 2022 request).
Branch: `24-referenced-by-reverse-add` off `release/2.0`. **Full-stack** (one PR, all four editors).

## What it does

Every editor with a "Referenced By" section can now add the entity it edits *into* a referring
container directly from that section (reverse-association), instead of navigating to the container
and adding it there. Remove is symmetric. One multi-type picker per editor; the built-in type
filter in `entity-selector-dialog` handles the multi-type cases.

Reverse-add uses the existing generic container commands. Each of those merges and returns the
**container** DTO, not the edited entity — so every handler reloads the edited entity after
add/remove to refresh its `referencedBy`, rather than adopting `result.entity`.

| Editor    | Referrer types offered      | Add / Remove command                                   |
|-----------|-----------------------------|--------------------------------------------------------|
| Goal      | Actor, Stakeholder, Story, UseCase | AddGoalToGoalContainer / RemoveGoalFromGoalContainer (containerType) |
| Actor     | Story, UseCase              | AddActorToActorContainer / RemoveActorFromActorContainer (containerType) |
| Story     | UseCase                     | AddStoryToStoryContainer / RemoveStoryFromStoryContainer (containerType) |
| Scenario  | UseCase                     | AddScenarioToUseCase / RemoveScenarioFromUseCase (useCaseId) |

Project is a valid container for goal/actor/story but is not offered in any picker — it is the
implicit fallback container. Remove still handles a Project row if one exists (uses the row's type).

## Backend (Story/Scenario had no `referencedBy` before)

- `StoryDto` / `ScenarioDto`: added `List<EntityReferenceDto> referencedBy`.
- `ProjectQueryController`:
  - Story detail populates from `story.getReferers()` (Set<StoryContainer>) via a new
    `toEntityReference(StoryContainer)` overload, mirroring the Goal path.
  - Scenario detail populates by scanning `project.getUseCases()` for use cases whose
    `getAdditionalScenarios()` contains this scenario (matched by id). `getUsingUseCases()` is
    **not** used — it tracks only the *primary*-scenario relationship, whereas reverse-add manages
    the additional-scenarios many-to-many.
  - Summary DTOs and the inline UseCase-detail constructors pass `null` for the new field.
- Goal/Actor DTOs already exposed `referencedBy` / `referencedByUseCases`+`referencedByStories`;
  no backend change there.

## Frontend

- `entity-selector-dialog`: multi-type mode (`entityTypes[]` + `title`) — done earlier in the branch.
- Goal & Actor editors: replaced their read-only `referencedBy` tables with an editable
  `app-relationship-section`; disambiguated the by-type `@ViewChild(RelationshipSectionComponent)`
  into template-ref ViewChilds so the second section doesn't collide. Actor combines its two
  referrer lists into one via `referencedByAll()`.
- Story & Scenario editors: new "Referenced By" section (edit view only).
- TS models `StoryDto` / `ScenarioDto` gained `referencedBy`.

## Verification

- Frontend: `ng build` (template type-check) green; unit tests green incl. new reverse-add tests
  on goal + actor editors (goal 37→39, actor 38→40). Full affected-spec run: 213 passing.
- Backend: build + tests to be run on a JDK 17 toolchain (the device sandbox has only JDK 11 / no
  Maven, so backend was verified by static review, not compiled here).
