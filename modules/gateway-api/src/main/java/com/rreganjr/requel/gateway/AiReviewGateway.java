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
package com.rreganjr.requel.gateway;

import java.util.Optional;

import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;

/**
 * Issue #387: the AI review of one entity through the gateway - request one and read the latest
 * run - for the MCP tools and {@code requel review}. The same rules as the editor's button: the
 * caller is a stakeholder on the project or a system admin, and the entity must be in
 * {@code projectName}.
 * <p>
 * Failures are {@link GatewayException}s: NOT_FOUND (no such project or entity), INVALID_INPUT (a
 * type that isn't reviewable, an unknown task type, an entity from another project),
 * UNAUTHORIZED, and CONFLICT when no review can run for the entity (AI off on the server, the
 * project not allowlisted, or the type's review switched off in the project; the message says
 * which).
 */
public interface AiReviewGateway {

    /** The task type the per-entity review runs as; the default for {@link #latestReview}. */
    String REQUIREMENTS_REVIEW = "REQUIREMENTS_REVIEW";

    /** The composed policy pass a request also queues when a policy applies (#265). */
    String POLICY_REVIEW = "POLICY_REVIEW";

    /**
     * Queue a review of the entity, or return the run already queued or running for it.
     *
     * @return the runs to follow with {@link #latestReview}
     */
    AiReviewRequestDto requestReview(String projectName, String entityType, long entityId)
            throws GatewayException;

    /**
     * The entity's latest review run and its findings; empty when it has never been reviewed.
     *
     * @param taskType {@link #REQUIREMENTS_REVIEW} (also when null) or {@link #POLICY_REVIEW}
     */
    Optional<AiReviewDto> latestReview(String projectName, String entityType, long entityId,
            String taskType) throws GatewayException;
}
