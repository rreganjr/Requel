# Domain-Oriented Import Strategy
# Domain-Oriented Import Strategy (2025-10-30)

## 1. Current Behaviour Through a DDD Lens
- The import workflow is orchestrated by `ImportProjectCommandImpl`, which effectively operates as an **application service** coordinating several **aggregates** (Project, Annotation, Identity, Organization). However, many aggregates expose JAXB `afterUnmarshal` hooks that reach into infrastructure concerns (repositories, entity managers), bypassing the intended boundaries.
- During unmarshalling, aggregates eagerly collaborate with repositories (`UserRepository`, `ProjectRepository`, etc.). This prematurely attaches identity and organisation aggregates, so the **aggregate persistence boundary** is crossed before invariants are enforced, producing duplicate inserts and constraint violations.
- Annotation grouping logic pulls project-specific collaborators directly into the annotation aggregate. This violates the **bounded context** separation we want between the Annotation context and the Project context.

### 1.1 Aggregate Touchpoints of `afterUnmarshal` Hooks

| Aggregate/Entity | Current responsibility (problematic behaviour) |
| --- | --- |
| `AbstractProjectOrDomain` | Registers a patcher that reattaches `Goal` references to the Project aggregate and enqueues `JAXBCreatedEntityPatcher`, tying import-time lifecycle to repository access. |
| `AbstractProjectOrDomainEntity` | Forces parent injection and adds annotatable + created-entity patchers, coupling the entity to infrastructure services. |
| `AbstractTextEntity` | Performs simple text normalisation (benign, remains a value-object concern). |
| `ActorImpl`, `NonUserStakeholderImpl`, `StoryImpl`, `GoalRelationImpl`, `ScenarioImpl`, `StepImpl`, `UseCaseImpl`, `ProjectTeamImpl` | Invoke `JAXBCreatedEntityPatcher` and bespoke fix-ups to rebuild bidirectional associations, effectively acting as mini-application services inside aggregates. |
| `ProjectImpl` | Coordinates annotatable and organised-entity patchers and ensures owner roles are linked—again pulling infrastructure logic into the aggregate root. |
| `StakeholderPermissionImpl` | Calls `ProjectRepository` to swap permission stubs with managed entities, violating aggregate autonomy. |
| `ProjectUserRole` | Reaches through the `User2UserImplAdapter` to resolve persistent users, coupling to the Identity bounded context during deserialisation. |
| `UserStakeholderImpl` | Uses repository merge logic inside the aggregate, breaking persistence ignorance. |
| `UserImpl` | Normalises primitives (fine) but also looks up existing users, registers replacements, and clones state—work that belongs in an application/domain service, not the aggregate itself. |
| `AbstractUserRole` | Uses `JAXBUserRolePatcher` to reach repositories for permissions. |
| `JAXBCreatedEntityPatcher` | A procedural service that mutates aggregates while they are still detached, effectively acting as an anti-corruption layer but embedded in the domain model. |
| `JAXBOrganizedEntityPatcher` | Same pattern for organisations—repository lookups mixed with aggregate mutation. |
| `JAXBAnnotationGroupedByPatcher` | Depends on `ProjectOrDomain` entities, leaking Project bounded-context knowledge into the Annotation bounded context. |

## 2. Strategic Goals (DDD Terminology)
- Re-establish **aggregate boundaries** so that import logic lives in application or domain services, not inside entities.
- Maintain **persistence ignorance** during unmarshalling; aggregates should be materialised in a detached state and only attached when the application service commits the transaction.
- Introduce a dedicated **mapping layer** (DTOs) to act as an **anti-corruption layer** between the XML contract and our aggregates.
- Provide an extensible **domain service** (renaming the current `EntityPatcher` idea to `AggregateAssembler`) that can enforce invariants and replace stubs with managed references before the unit of work is flushed.
- Decouple annotation grouping by introducing an **Annotation bounded-context adapter** so the Annotation aggregate can remain agnostic of Project-specific types.

## 3. Tactical Steps

1. **Catalogue infrastructure leakage.**
   - Keep the existing audit of `afterUnmarshal` hooks as a reference artefact for identifying where aggregates violate persistence ignorance.
   - Map each hook to the bounded context and aggregate that owns the behaviour to understand the correct home for that logic.

2. **Introduce an anti-corruption layer.**
   - Define DTOs (`ProjectImportDto`, `AnnotationDto`, etc.) representing the XML schema. These DTOs live outside the domain layer and shield aggregates from transport concerns.
   - Build mappers (could leverage MapStruct or manual assemblers) that translate DTOs into aggregate construction calls. Mapping occurs inside the application service after unmarshalling completes.

3. **Implement `AggregateAssembler` SPI.**
   - Define an interface (formerly `EntityPatcher`) such as:
     ```java
     interface AggregateAssembler<T> {
         Class<T> targetAggregate();
         void assemble(T aggregate, ImportUnitOfWork context);
     }
     ```
   - `ImportUnitOfWork` (formerly `ImportContext`) supplies repositories, identity caches, and factories needed by assemblers while keeping transactional control in the application service.
   - Assemblers function as domain services that complete aggregates (resolve references, enforce invariants) without performing persistence operations themselves.

4. **Registry & orchestration.**
   - The application service collects all `AggregateAssembler` beans and registers them by aggregate type.
   - After DTO → aggregate materialisation, the application service traverses the aggregate graph, invoking assemblers inside the import **unit of work**. Once all invariants hold, it hands the fully-consistent aggregate to the repository for persistence.
   - Assemblers must replace identity/organisation placeholders in situ (e.g., `setCreatedBy`) so the final graph is consistent before calling repositories.

5. **Annotation bounded context adapter.**
   - Define interfaces like `AnnotationGroupingAdapter` within the Annotation context. Project-specific implementations live in the Project context and are injected via configuration, preserving the layered architecture.
   - Move `JAXBAnnotationGroupedByPatcher` (or its successor) into the annotation module once adapters are in place, ensuring the domain model for annotations depends only on abstractions.

6. **Verification.**
   - Exercise the import application service with representative project XMLs to ensure aggregates remain detached until the final persistence boundary.
   - Add integration tests that assert duplicate identities/organisations are resolved via assemblers and that annotation grouping remains consistent.

## 4. Open Questions
- **Multi-format ingest:** Should the DTO anti-corruption layer back both XML and future JSON endpoints? (Current stance: prioritise XML, design DTOs so they can be shared later.)
- **Context-specific behaviours:** How do we package domain-specific positions (e.g., dictionary-driven annotations) so they remain inside their bounded context yet integrate with the Annotation context? (Idea: module-specific assemblers or adapters that plug into the annotation bounded context without polluting the core model.)
- **Caching & performance:** What caching strategies should `ImportUnitOfWork` adopt to avoid repeated repository access while keeping aggregate state consistent? (Needs measurement once assemblers are in place.)
