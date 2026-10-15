# #75 command permission scan (release/2.0 @ 1a7d0f93)

The scan #75 was designed from. It describes the code **before** #75: the exemption it lists is now `CascadeAuthorizable`, and the gaps in section C are fixed. The decisions are in [permission_model_plan.md](permission_model_plan.md#decisions-75).

**Paths.** PJ = `modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/`, AJ = `modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/`.

**Abbreviations.**
- **RAFA** = RemoveAnnotationFromAnnotatable. It has no requirement.
- **RAABD** = `AbstractProjectCommand.removeAllAnnotationsBeforeDelete`. It deletes ignored findings and provenance, then runs an exempt RemoveAllAnnotationsFromAnnotatable.

**How the check works.**
- The `AuthorizingCommandHandler` check is skipped when a command is exempt, has no requirement, or has `editedBy == null`.
- Over HTTP `editedBy` is never null, because `CurrentUserCommandHandler` fills it in.
- The exempt flag is set on each instance and never inherited.
- A permission key is `entityClass.getName()[Type]`. There is no hierarchy, so a requirement must name exactly the class seeded in the catalog.

## A. Commands that run sub-commands

| Parent | Requirement | Sub-command | Sub requirement | Exempt | When |
|---|---|---|---|---|---|
| DeleteGoal | Goal[Delete] | RAFA | none | no | per annotation |
| | | RemoveGoalFromGoalContainer | Goal[Edit] | yes | each container |
| | | DeleteGoalRelation | Goal[Edit] | yes | each relation |
| | | RAABD | → Annotation[Delete] | yes | always |
| DeleteActor | Actor[Delete] | RemoveGoalFromGoalContainer | Goal[Edit] | yes | each goal on the actor |
| | | RemoveActorFromActorContainer | Actor[Edit] | yes | each container (refused if primary actor of a use case) |
| | | DeletePosition | Annotation[Delete] | yes | matching AddActor position |
| | | RAFA, RAABD | | | |
| DeleteStory | Story[Delete] | RemoveActorFromActorContainer | Actor[Edit] | yes | each actor |
| | | RemoveGoalFromGoalContainer | Goal[Edit] | yes | each goal |
| | | RemoveStoryFromStoryContainer | Story[Edit] | yes | each container |
| | | RAFA, RAABD | | | |
| DeleteUseCase | UseCase[Delete] | RemoveActor / RemoveGoal / RemoveStory FromContainer | Actor / Goal / Story [Edit] | yes | each |
| | | DeleteScenario, DeleteScenarioStep | Scenario[Delete] | yes | owned, unshared scenarios and steps |
| | | RAFA, RAABD | | | |
| DeleteScenario, DeleteScenarioStep | Scenario[Delete] | RAFA, RAABD | | | |
| DeleteGlossaryTerm | GlossaryTerm[Delete] | DeletePosition | Annotation[Delete] | yes | matching AddGlossaryTerm position |
| DeleteGoalRelation | Goal[Edit] | RAABD | → Annotation[Delete] | yes | always |
| DeleteReportGenerator, DeleteStakeholder | X[Delete] | RAFA, RAABD | | | |
| DeleteProject | Project[Delete] or SystemAdmin | every Delete* above, DeleteAnnotationGroup, RAABD | X[Delete] | yes | everything in the project |
| RAFA / RemoveAllAnnotations / DeleteAnnotationGroup | none | DeleteIssue, DeleteNote | Annotation[Delete] | yes | orphaned annotations |
| DeleteIssue | Annotation[Delete] | DeletePosition | Annotation[Delete] | yes | orphaned positions |
| DeletePosition | Annotation[Delete] | DeleteArgument | Annotation[Delete] | yes | each argument |
| ResolveIssueWithChangeSpellingPosition | Annotation[Edit] | EditGoal / Story / Actor / UseCase / Scenario / ScenarioStep / GlossaryTerm | that type's [Edit] | **no (deliberate)** | each entity containing the word; checked up front |
| ResolveIssueWithAddWordToDictionaryPosition | Annotation[Edit] | EditDictionaryWord | Annotation[Edit] | no | always |
| ResolveIssueWithAddActorPosition | Annotation[Edit] | EditActor | Actor[Edit] | **no** | always |
| ResolveIssueWithAddGlossaryTermPosition | Annotation[Edit] | EditGlossaryTerm | GlossaryTerm[Edit] | **no** | always |
| EditScenario (and ConvertStepToScenario) | Scenario[Edit] | EditScenarioStep | Scenario[Edit] | no | each step |
| | | DeleteScenarioStep | Scenario[Delete] | **yes** | steps dropped and unused elsewhere |
| EditUseCase | UseCase[Edit] | EditActor | Actor[Edit] | **no** | new primary actor name |
| | | EditScenario | Scenario[Edit] | **no** | always on create; on update when there are steps, no primary, or a rename |
| CopyGoal | Goal[Edit] | EditGoalRelation | Goal[Edit] | no | each relation |
| CopyScenario | Scenario[Edit] | CopyScenarioStep | Scenario[Edit] | no | each step |
| CopyUseCase | UseCase[Edit] | CopyScenario | Scenario[Edit] | **no** | always |
| EditProject (create) | **none** | EditReportGenerator | ReportGenerator[Edit] | no | built-in generators; failure logged and swallowed |
| ImportProject | **none** | EditReportGenerator | ReportGenerator[Edit] | no | same |
| UpsertFromSource | X[Edit] | inner Edit* | X[Edit] | no | created / updated |
| | | EditIssue, EditPosition | Annotation[Edit] | **yes** | on CONFLICT |
| DeleteIgnoredFinding | Project[Edit] | RAFA → DeleteIssue | Annotation[Delete] | yes | recorded annotation |
| RemoveUnneedLexicalIssues | none | RAFA | none | no | stale lexical issues |
| GenerateReport | none | ExportProject | none | no | always |

## B. Direct writes to other entity types in execute()

These are mostly relationship bookkeeping (collections, referers and join rows) on the other side.

| Command | Requirement | Other entities changed |
|---|---|---|
| All entity deletes | X[Delete] | GlossaryTerm referers; ignored findings; provenance |
| DeleteStory | Story[Delete] | primary-actor join row |
| DeleteScenario | Scenario[Delete] | other scenarios' steps; every UseCase's additionalScenarios (a use case with it as primary is left as a TODO) |
| DeleteScenarioStep | Scenario[Delete] | using scenarios' steps |
| DeleteUseCase | UseCase[Delete] | Scenario.usingUseCases |
| DeleteGlossaryTerm | GlossaryTerm[Delete] | alternates' canonical term |
| DeleteStakeholder | Stakeholder[Delete] | Goal referers; team membership; the user's activeProjects |
| DeleteProject | Project[Delete] / admin | teams, dictionary words, ignored findings, provenance, assistant settings and definitions |
| DeleteIssue / DeletePosition / DeleteNote / DeleteArgument | Annotation[Delete] | annotatables' collections; issue unresolved |
| EditGoal / EditActor / EditStory | X[Edit] | container collections; EditStory changes the primary-actor join row |
| EditUseCase | UseCase[Edit] | Actor referers (primary actor) |
| ConvertStepToScenario | Scenario[Edit] | moves the step's annotations; **deletes the original Step directly** |
| EditGlossaryTerm | GlossaryTerm[Edit] | the glossaryTerms collection of **any** referer entity |
| AddGlossaryTermReferer | Annotation[Edit] | GlossaryTerm referers |
| ReplaceGlossaryTerm | **none** | rewrites Goal / Story name and text. Unused; the Actor branch is dead code, a duplicate `instanceof Story` at :136 |
| Copy* | X[Edit] | referers on Actor / Goal / Story; annotations; glossary terms |
| Add/Remove X To/From Container | X[Edit] | the container's collection |
| Add/RemoveScenario / SetPrimaryScenario | UseCase[Edit] | links a Scenario with no Scenario permission |
| EditIssue / EditNote | Annotation[Edit] | attaches to any annotatable |
| EditUserStakeholder | Stakeholder[Edit] | **grants and revokes any permission, Grant rows included, on anyone including self**; teams |
| UpsertFromSource | X[Edit] | creates an ExternalSource, which RecordSource otherwise gates with Project[Edit] |

## C. No authorization requirement

| Command | Reachable from |
|---|---|
| RemoveAnnotationFromAnnotatable, RemoveAllAnnotations, DeleteAnnotationGroup, RemoveUnneedLexicalIssues | internal only; denied on the gateway |
| ReplaceGlossaryTerm | no caller; registered but denied on the gateway |
| ExportProject | ProjectQueryController checks project access. **`/projectxml?project=<name>` (ProjectXmlController) checks nothing, and no security chain matches `/projectxml`**, so it is likely reachable without login |
| GenerateReport | ProjectQueryController checks access |
| ImportProject | `/api/commands`. **`createProjects` is not checked anywhere** |
| EditProject (create) | requirement is null, with a comment saying execute() checks `createProjects`. **It doesn't.** The only use of `createProjects` is advisory, in my-permissions |
| Login, EditUser (self), Batch, dictionary tools | as expected |

## D. Derived dependencies

### D1. Grants a closure would add (the effective permission today through exempt sub-commands)

| Holding | also needs (directly) |
|---|---|
| Goal[Delete] | Goal[Edit], Annotation[Delete] |
| Actor[Delete] | Actor[Edit], Goal[Edit], Annotation[Delete] |
| Story[Delete] | Story[Edit], Actor[Edit], Goal[Edit], Annotation[Delete] |
| UseCase[Delete] | Actor[Edit], Goal[Edit], Story[Edit], Scenario[Delete] |
| Scenario[Delete] | Annotation[Delete] |
| GlossaryTerm[Delete], Stakeholder[Delete], ReportGenerator[Delete] | Annotation[Delete] |
| Project[Delete] | every X[Delete] above |
| Goal[Edit] | Annotation[Delete] (DeleteGoalRelation) |
| Scenario[Edit] | Scenario[Delete] (EditScenario drops steps) |
| UseCase[Edit] | Scenario[Edit], Actor[Edit] (non-exempt today, so it fails) |
| Project[Edit] | Annotation[Delete] (DeleteIgnoredFinding) |
| X[Edit] through UpsertFromSource | Annotation[Edit] (conflict issues) |

### D2. Blocked today unless the user also holds Q (non-exempt)

- UseCase[Edit] needs Scenario[Edit]: always on create, sometimes on update. It also needs Actor[Edit] for a new primary actor.
- CopyUseCase needs Scenario[Edit].
- Resolve change-spelling / add-actor / add-glossary-term need the target's [Edit]. This is deliberate.
- EditProject create and ImportProject need ReportGenerator[Edit]. The creator already holds it.

## Catalog

There are 31 rows.
- Edit, Grant and Delete for: Project, Annotation, Goal, Actor, Stakeholder, GlossaryTerm, Story, UseCase, Scenario, ReportGenerator.
- AssistantDefinition has Edit only.

Notes:
- Every literal requirement names a seeded row.
- **No command requires any [Grant].**
- LinkSource, UnlinkSource and UpsertFromSource fall back to `ProjectOrDomainEntity[Edit]` on invalid input, and that row doesn't exist.

## Other inconsistencies

- EditDictionaryWord requires Annotation[Edit], but AddProjectDictionaryWord requires Project[Edit] for the same write.
- The GatewayPolicyConfig comment says EditLexicalIssue, the position commands and ResolveIssue are "not independently authorized". They are now, through inheritance.
