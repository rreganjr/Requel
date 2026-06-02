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
package com.rreganjr.requel.mcp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.query.AnnotationQueryController;
import com.rreganjr.requel.service.query.ProjectQueryController;

/**
 * In-process gateway for the first bundled MCP server. A future standalone
 * bridge can provide an HTTP-backed implementation of {@link ProjectQueryGateway}
 * without changing protocol handlers.
 */
@Component
public class InProcessProjectQueryGateway implements ProjectQueryGateway {

	private final ProjectQueryController projectQueryController;
	private final AnnotationQueryController annotationQueryController;

	@Autowired
	public InProcessProjectQueryGateway(ProjectQueryController projectQueryController,
			AnnotationQueryController annotationQueryController) {
		this.projectQueryController = projectQueryController;
		this.annotationQueryController = annotationQueryController;
	}

	@Override
	public List<ProjectDto> listProjects() {
		return projectQueryController.listProjects();
	}

	@Override
	public ProjectDto getProject(String projectName) {
		return unwrap(projectQueryController.getProject(projectName), ProjectDto.class);
	}

	@Override
	public List<ProjectTreeNodeDto> getProjectTree(String projectName) {
		ResponseEntity<List<ProjectTreeNodeDto>> response = projectQueryController.getProjectTree(
				projectName);
		return unwrap(response);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<GlossaryTermDto> getGlossaryTerms(String projectName) {
		return (List<GlossaryTermDto>) unwrap(projectQueryController.listTerms(projectName));
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<OpenIssueDto> getOpenIssues(String projectName) {
		return (List<OpenIssueDto>) unwrap(projectQueryController.getOpenIssues(projectName));
	}

	@Override
	public AnnotationsDto getAnnotations(String projectName, String entityType, long entityId) {
		return unwrap(annotationQueryController.getAnnotations(projectName, entityType, entityId));
	}

	private <T> T unwrap(ResponseEntity<?> response, Class<T> bodyType) {
		Object body = unwrap(response);
		if (!bodyType.isInstance(body)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unexpected query response body: " + body);
		}
		return bodyType.cast(body);
	}

	private <T> T unwrap(ResponseEntity<T> response) {
		if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
			return response.getBody();
		}
		throw new ResponseStatusException(HttpStatus.valueOf(response.getStatusCode().value()));
	}
}
