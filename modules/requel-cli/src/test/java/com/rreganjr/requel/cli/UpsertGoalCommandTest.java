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
package com.rreganjr.requel.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.AnnotationsDto;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditNoteInput;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.NoteDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpsertGoalCommandTest {

    /** Query side with an empty project, so the upserter always takes the create path. */
    private static final class EmptyQueryGateway implements QueryGateway {
        @Override
        public List<EntityReferenceDto> searchProjectEntities(String projectName, String query) {
            return List.of();
        }

        @Override
        public AnnotationsDto getAnnotations(String projectName, String entityType, long entityId) {
            return new AnnotationsDto(List.of(), List.of());
        }

        @Override
        public List<com.rreganjr.requel.service.api.dto.ProjectDto> listProjects() {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.rreganjr.requel.service.api.dto.ProjectDto getProject(String projectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto> getProjectTree(
                String projectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.rreganjr.requel.service.api.dto.GlossaryTermDto> getGlossaryTerms(
                String projectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.rreganjr.requel.service.api.dto.OpenIssueDto> getOpenIssues(
                String projectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getEntity(String projectName, String entityType, long entityId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, List<EntityReferenceDto>> getEntityNeighbors(String projectName,
                String entityType, long entityId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> getProjectContext(String projectName) {
            throw new UnsupportedOperationException();
        }
    }

    /** Command side that satisfies EditGoal (create) + EditNote with real DTOs. */
    private static CommandGateway creatingGateway() {
        return request -> switch (request.commandType()) {
            case "EditGoal" -> {
                EditGoalInput i = (EditGoalInput) request.input();
                yield new GatewayResult("EditGoal",
                        new GoalDto(1L, 0, i.name(), i.text(), "t", null, null, null));
            }
            case "EditNote" -> {
                EditNoteInput i = (EditNoteInput) request.input();
                yield new GatewayResult("EditNote", new NoteDto(1L, 0, i.text(), "t"));
            }
            default -> throw new IllegalStateException("unexpected " + request.commandType());
        };
    }

    private static UpsertGoalCommand command(CommandGateway command, QueryGateway query) {
        RequelCli parent = new RequelCli();
        parent.url = "http://localhost:8080";
        parent.output = OutputFormat.TEXT;
        UpsertGoalCommand cmd = new UpsertGoalCommand();
        cmd.parent = parent;
        cmd.project = "Demo";
        cmd.criterionText = "Users can export reports to CSV.";
        cmd.sourceSystem = "jira";
        cmd.sourceRef = "PROJ-1#1";
        cmd.commandGatewayOverride = command;
        cmd.queryGatewayOverride = query;
        return cmd;
    }

    @Test
    void successReturnsZero() {
        UpsertGoalCommand cmd = command(creatingGateway(), new EmptyQueryGateway());
        assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS);
    }

    @Test
    void unauthorizedReturnsAuthCode() {
        CommandGateway denies = request -> {
            throw new GatewayException(GatewayException.Kind.UNAUTHORIZED, "nope");
        };
        UpsertGoalCommand cmd = command(denies, new EmptyQueryGateway());
        assertThat(cmd.call()).isEqualTo(ExitCode.AUTH);
    }

    @Test
    void blankRequiredInputReturnsUsageWithoutCallingGateway() {
        CommandGateway neverCalled = request -> {
            throw new AssertionError("gateway must not be called for invalid input");
        };
        UpsertGoalCommand cmd = command(neverCalled, new EmptyQueryGateway());
        cmd.project = "   "; // blank -> UpsertGoalRequest rejects it
        assertThat(cmd.call()).isEqualTo(ExitCode.USAGE);
    }
}
