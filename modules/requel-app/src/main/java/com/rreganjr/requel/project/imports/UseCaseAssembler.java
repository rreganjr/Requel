package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.UseCaseImportDraft;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.UseCaseImpl;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles use cases from drafts, linking actors, goals, stories, and scenarios via unit-of-work.
 */
public class UseCaseAssembler implements AggregateAssembler<UseCaseImportDraft, UseCaseImpl> {

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public UseCaseAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<UseCaseImportDraft> draftType() {
        return UseCaseImportDraft.class;
    }

    @Override
    public Class<UseCaseImpl> aggregateType() {
        return UseCaseImpl.class;
    }

    @Override
    public UseCaseImpl assemble(UseCaseImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("use case draft is required");
        }
        User createdBy = resolveCreatedBy(draft, unitOfWork);
        Actor primaryActor = resolveActor(draft.getPrimaryActorExternalId(), unitOfWork).orElse(null);
        Scenario scenario = resolveScenario(draft.getScenarioExternalId(), unitOfWork).orElse(null);
        UseCaseImpl useCase = new UseCaseImpl(project, primaryActor, createdBy, draft.getName(), draft.getDescription(), scenario);

        unitOfWork.register(UseCaseImpl.class, draft.getExternalId(), useCase);
        unitOfWork.register(com.rreganjr.requel.project.UseCase.class, draft.getExternalId(), useCase);

        draft.getGoalExternalIds().forEach(goalId -> resolveGoal(goalId, unitOfWork)
                .ifPresent(goal -> {
                    useCase.getGoals().add(goal);
                    goal.getReferers().add(useCase);
                }));

        draft.getActorExternalIds().forEach(actorId -> resolveActor(actorId, unitOfWork)
                .ifPresent(actor -> {
                    useCase.getActors().add(actor);
                    actor.getReferers().add(useCase);
                }));

        draft.getStoryExternalIds().forEach(storyId -> resolveStory(storyId, unitOfWork)
                .ifPresent(story -> {
                    useCase.getStories().add(story);
                    story.getReferers().add(useCase);
                }));

        return useCase;
    }

    private Optional<Actor> resolveActor(String id, ImportUnitOfWork uow) {
        return uow.resolve(Actor.class, id);
    }

    private Optional<Goal> resolveGoal(String id, ImportUnitOfWork uow) {
        return uow.resolve(Goal.class, id);
    }

    private Optional<Story> resolveStory(String id, ImportUnitOfWork uow) {
        return uow.resolve(Story.class, id);
    }

    private Optional<Scenario> resolveScenario(String id, ImportUnitOfWork uow) {
        return uow.resolve(Scenario.class, id);
    }

    private User resolveCreatedBy(UseCaseImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(draft.getCreatedByExternalId())) {
            Optional<User> resolved = unitOfWork.resolve(User.class, draft.getCreatedByExternalId());
            if (resolved.isPresent()) {
                return resolved.get();
            }
            try {
                return userRepository.findUserByUsername(draft.getCreatedByExternalId());
            } catch (Exception ignored) {
            }
        }
        return defaultCreatedBy;
    }
}
