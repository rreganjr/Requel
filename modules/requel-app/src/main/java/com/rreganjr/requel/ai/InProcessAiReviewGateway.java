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
package com.rreganjr.requel.ai;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.gateway.AiReviewGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.service.api.dto.AiReviewDto;
import com.rreganjr.requel.service.api.dto.AiReviewRequestDto;

/**
 * Issue #387: the gateway's AI review, in process - what the MCP tools and the gateway REST
 * endpoints call. {@link AiReviewService} does the work as the current user; this adds a
 * transaction (the MCP and gateway paths don't always have a request-scoped session) and turns
 * its exceptions into {@link GatewayException} kinds.
 */
@Component
public class InProcessAiReviewGateway implements AiReviewGateway {

	private final AiReviewService service;
	private final TransactionTemplate transaction;

	@Autowired
	public InProcessAiReviewGateway(AiReviewService service,
			PlatformTransactionManager transactionManager) {
		this.service = service;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	@Override
	public AiReviewRequestDto requestReview(String projectName, String entityType, long entityId)
			throws GatewayException {
		return call(() -> service.requestReview(projectName, entityType, entityId));
	}

	@Override
	public Optional<AiReviewDto> latestReview(String projectName, String entityType,
			long entityId, String taskType) throws GatewayException {
		return call(() -> service.latestReview(projectName, entityType, entityId, taskType)
				.map(AiReviewDtos::of));
	}

	private <T> T call(java.util.function.Supplier<T> work) throws GatewayException {
		try {
			return transaction.execute(status -> work.get());
		} catch (NoSuchEntityException e) {
			throw new GatewayException(GatewayException.Kind.NOT_FOUND, e.getMessage(), e);
		} catch (AuthorizationException e) {
			throw new GatewayException(GatewayException.Kind.UNAUTHORIZED, e.getMessage(), e);
		} catch (AiReviewUnavailableException e) {
			throw new GatewayException(GatewayException.Kind.CONFLICT, e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			throw new GatewayException(GatewayException.Kind.INVALID_INPUT, e.getMessage(), e);
		}
	}
}
