/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.service.stream;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.service.query.ProjectReadAccess;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Issue #450: the event stream's subscription rule.
 * <ul>
 *   <li>The broadcasts ({@link #PROJECT_BROADCAST}, {@link #PERMISSIONS_BROADCAST}): anyone signed
 *       in. Their payload names nothing.</li>
 *   <li>An annotatable entity, by its {@link AnnotatableTypeRegistry} type ({@code Goal},
 *       {@code UseCase}, {@code Project} with a real id, ...): when the user can read the
 *       entity's project ({@link ProjectReadAccess}), the same rule the annotation and tag reads
 *       use.</li>
 *   <li>Anything else, including an unknown type, a malformed key or an entity that doesn't
 *       exist: refused. One answer for all of these, so a refusal says nothing about what
 *       exists.</li>
 * </ul>
 */
@Component
public class ProjectStreamTargetAccess implements StreamTargetAccess {

    private static final Logger log = LoggerFactory.getLogger(ProjectStreamTargetAccess.class);

    private final AnnotatableTypeRegistry annotatableTypeRegistry;
    private final EntityManager entityManager;
    private final UserRepository userRepository;

    public ProjectStreamTargetAccess(AnnotatableTypeRegistry annotatableTypeRegistry,
            EntityManager entityManager, UserRepository userRepository) {
        this.annotatableTypeRegistry = annotatableTypeRegistry;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSubscribe(String username, String targetKey) {
        if (username == null || targetKey == null) {
            return false;
        }
        if (PROJECT_BROADCAST.equals(targetKey) || PERMISSIONS_BROADCAST.equals(targetKey)) {
            return true;
        }
        try {
            int colon = targetKey.indexOf(':');
            if (colon <= 0 || colon == targetKey.length() - 1) {
                return false;
            }
            Optional<Class<? extends Annotatable>> type = annotatableTypeRegistry
                    .resolveEntityType(targetKey.substring(0, colon));
            if (type.isEmpty()) {
                return false;
            }
            long id = Long.parseLong(targetKey.substring(colon + 1));
            Annotatable entity = entityManager.find(type.get(), id);
            if (entity == null) {
                return false;
            }
            Project project = ProjectReadAccess.projectOf(entity);
            User user = userRepository.findUserByUsername(username);
            return ProjectReadAccess.canRead(project, user);
        } catch (Exception e) {
            // A bad id, an unknown user, a lookup failure: all "no".
            log.debug("canSubscribe user={} target={} refused: {}", username, targetKey, e.toString());
            return false;
        }
    }
}
