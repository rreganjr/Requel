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
package com.rreganjr.requel.service.query;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Who may read a project, shared by the read controllers (issue #296). {@code ProjectQueryController}
 * had the rule to itself, and {@code AnnotationQueryController} applied none: it loaded any entity
 * by type and id, so any signed-in user could read the notes and issues of a project they are not
 * a stakeholder on.
 */
final class ProjectReadAccess {

    private ProjectReadAccess() {
    }

    /** A system administrator reads every project; anyone else, the projects they are a user stakeholder on. */
    static boolean canRead(Project project, User user) {
        if (project == null || user == null) {
            return false;
        }
        if (user.hasRole(SystemAdminUserRole.class)) {
            return true;
        }
        for (Stakeholder s : project.getStakeholders()) {
            if (s instanceof UserStakeholder && s.matchesUser(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The project an annotatable belongs to, or {@code null} when it has none this can find: the
     * project itself, a project entity's project, or a goal relation's goals' project.
     */
    static Project projectOf(Annotatable annotatable) {
        if (annotatable instanceof Project project) {
            return project;
        }
        if (annotatable instanceof ProjectOrDomainEntity entity
                && entity.getProjectOrDomain() instanceof Project project) {
            return project;
        }
        if (annotatable instanceof GoalRelation relation && relation.getFromGoal() != null) {
            return projectOf(relation.getFromGoal());
        }
        return null;
    }

    /** Whether the annotatable is in {@code project}, compared by id. */
    static boolean belongsTo(Annotatable annotatable, Project project) {
        Project owner = projectOf(annotatable);
        return owner != null && project != null && owner.getId() != null
                && owner.getId().equals(project.getId());
    }
}
