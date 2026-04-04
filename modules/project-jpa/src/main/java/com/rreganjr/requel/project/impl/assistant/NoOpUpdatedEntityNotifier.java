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
package com.rreganjr.requel.project.impl.assistant;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of UpdatedEntityNotifier for the Angular SPA.
 * The Echo2 UI used this callback to push refresh events to open panels;
 * the REST/SSE-based Angular client does not need push notification from the assistant layer.
 */
@Component
public class NoOpUpdatedEntityNotifier implements UpdatedEntityNotifier {

    @Override
    public void entityUpdated(ProjectOrDomain pod) {
        // no-op: Angular client fetches updated state via REST after command completion
    }

    @Override
    public void entityUpdated(ProjectOrDomainEntity entity) {
        // no-op: Angular client fetches updated state via REST after command completion
    }
}
