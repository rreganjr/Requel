/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.project.imports;

import com.rreganjr.requel.imports.AggregateAssembler;
import com.rreganjr.requel.imports.ImportException;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import com.rreganjr.requel.imports.project.StoryImportDraft;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.impl.StoryImpl;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;
import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * Assembles stories from drafts and links to actors/goals via the unit-of-work.
 */
public class StoryAssembler implements AggregateAssembler<StoryImportDraft, StoryImpl> {

    private final Project project;
    private final UserRepository userRepository;
    private final User defaultCreatedBy;

    public StoryAssembler(Project project, UserRepository userRepository, User defaultCreatedBy) {
        this.project = project;
        this.userRepository = userRepository;
        this.defaultCreatedBy = defaultCreatedBy;
    }

    @Override
    public Class<StoryImportDraft> draftType() {
        return StoryImportDraft.class;
    }

    @Override
    public Class<StoryImpl> aggregateType() {
        return StoryImpl.class;
    }

    @Override
    public StoryImpl assemble(StoryImportDraft draft, ImportUnitOfWork unitOfWork) throws ImportException {
        if (draft == null) {
            throw new ImportException("story draft is required");
        }

        User createdBy = resolveCreatedBy(draft, unitOfWork);
        StoryImpl story = new StoryImpl(project, createdBy, draft.getName(), draft.getDescription(),
                com.rreganjr.requel.project.StoryType.valueOf(draft.getStoryType()));

        // Register early for cross references (e.g., usecases)
        unitOfWork.register(StoryImpl.class, draft.getExternalId(), story);
        unitOfWork.register(com.rreganjr.requel.project.Story.class, draft.getExternalId(), story);

        draft.getGoalExternalIds().forEach(goalId -> {
            Optional<Goal> goal = unitOfWork.resolve(Goal.class, goalId);
            goal.ifPresent(g -> {
                story.getGoals().add(g);
                g.getReferers().add(story);
            });
        });

        draft.getActorExternalIds().forEach(actorId -> {
            Optional<Actor> actor = unitOfWork.resolve(Actor.class, actorId);
            actor.ifPresent(a -> {
                story.getActors().add(a);
                a.getReferers().add(story);
            });
        });

        // 2.0 adds a single primary actor on stories alongside the additional
        // actors collection (V7 Flyway migration, primary_actor_id column). The
        // streaming import flow previously dropped this on the floor — the
        // round-trip test in #47 caught it. Resolve via the same unit-of-work
        // the actors collection uses; actors are assembled before stories, so
        // by the time we get here the actor is registered.
        if (StringUtils.hasText(draft.getPrimaryActorExternalId())) {
            unitOfWork.resolve(Actor.class, draft.getPrimaryActorExternalId())
                    .ifPresent(story::setPrimaryActor);
        }

        attachGlossaryTerms(story, draft.getGlossaryTermExternalIds(), unitOfWork);

        return story;
    }

    private User resolveCreatedBy(StoryImportDraft draft, ImportUnitOfWork unitOfWork) {
        if (StringUtils.hasText(draft.getCreatedByExternalId())) {
            Optional<User> resolved = unitOfWork.resolve(User.class, draft.getCreatedByExternalId());
            if (resolved.isPresent()) {
                return resolved.get();
            }
            try {
                return userRepository.findUserByUsername(draft.getCreatedByExternalId());
            } catch (Exception ignored) {
                // fall back to default
            }
        }
        return defaultCreatedBy;
    }

    private void attachGlossaryTerms(StoryImpl story, java.util.Set<String> termIds, ImportUnitOfWork unitOfWork) {
        termIds.forEach(termId -> unitOfWork.resolve(GlossaryTermImpl.class, termId)
                .ifPresent(term -> {
                    story.getGlossaryTerms().add(term);
                    term.getReferers().add(story);
                }));
    }
}
