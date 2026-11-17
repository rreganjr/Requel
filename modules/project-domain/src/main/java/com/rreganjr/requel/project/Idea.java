/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import com.rreganjr.platform.CreatedEntity;
import com.rreganjr.platform.domain.Describable;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

import java.util.Set;

/**
 * A rough requirement idea submitted to a project, as text or as the text of an uploaded file.
 * It starts {@link Status#SUBMITTED}; later work (decomposition, clarification) moves it on.
 */
public interface Idea extends ProjectOrDomainEntity, CreatedEntity, Describable {

    enum Status {
        SUBMITTED,
        CLARIFYING,
        BREAKDOWN_REVIEW,
        COMPLETE
    }

    /**
     * @return the idea source text (typed text + extracted file text)
     */
    String getSourceText();

    /**
     * @return current workflow status
     */
    Status getStatus();

    /**
     * @return requirements derived from this idea
     */
    Set<Requirement> getDerivedRequirements();
}
