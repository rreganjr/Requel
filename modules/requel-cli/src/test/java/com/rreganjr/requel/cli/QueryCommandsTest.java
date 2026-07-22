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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.OpenIssueDto;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class QueryCommandsTest {

    private static RequelCli parent(OutputFormat output) {
        RequelCli p = new RequelCli();
        p.url = "http://localhost:8080";
        p.output = output;
        return p;
    }

    private static ProjectDto project(String name) {
        return new ProjectDto(1L, 0, name, "A demo project", "Org", "ron", "ACTIVE",
                2, 3, 4, 5, 6, 7, 8, 0);
    }

    @Test
    void projectsListsNamesInText() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.listProjects()).thenReturn(List.of(project("Demo"), project("Other")));
        ProjectsCommand cmd = new ProjectsCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("Demo").contains("Other").contains("goals=3");
    }

    @Test
    void projectsEmptyMessage() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.listProjects()).thenReturn(List.of());
        ProjectsCommand cmd = new ProjectsCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("No projects.");
    }

    @Test
    void projectsJsonOutput() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.listProjects()).thenReturn(List.of(project("Demo")));
        ProjectsCommand cmd = new ProjectsCommand();
        cmd.parent = parent(OutputFormat.JSON);
        cmd.queryOverride = gw;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out.trim()).startsWith("[").contains("\"name\" : \"Demo\"");
    }

    @Test
    void projectSummary() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getProject("Demo")).thenReturn(project("Demo"));
        ProjectCommand cmd = new ProjectCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("Demo").contains("stakeholders=2").contains("A demo project");
    }

    @Test
    void projectTreeIndented() {
        QueryGateway gw = mock(QueryGateway.class);
        ProjectTreeNodeDto child = new ProjectTreeNodeDto(9L, "Goal", "G1");
        ProjectTreeNodeDto group = new ProjectTreeNodeDto("GoalGroup", "Goals", List.of(child));
        when(gw.getProjectTree("Demo")).thenReturn(List.of(group));
        ProjectCommand cmd = new ProjectCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";
        cmd.tree = true;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("- Goals").contains("  - G1 [Goal #9]");
    }

    @Test
    void glossaryTerms() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getGlossaryTerms("Demo")).thenReturn(List.of(
                new GlossaryTermDto(1L, 0, "SLA", "Service level agreement", "ron", null, null, null, null)));
        GlossaryCommand cmd = new GlossaryCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("SLA — Service level agreement");
    }

    @Test
    void openIssues() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getOpenIssues("Demo")).thenReturn(List.of(
                new OpenIssueDto(5L, "Ambiguous wording", true, "Goal", 3L, "Login goal")));
        OpenIssuesCommand cmd = new OpenIssuesCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("[Goal Login goal] Ambiguous wording").contains("(must resolve)");
    }

    @Test
    void search() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.searchProjectEntities("Demo", "log")).thenReturn(List.of(
                new EntityReferenceDto("Goal", 3L, "Login goal")));
        SearchCommand cmd = new SearchCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";
        cmd.queryText = "log";

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("Goal #3").contains("Login goal");
    }

    @Test
    void entityNeighborsGrouped() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getEntityNeighbors("Demo", "Goal", 3L)).thenReturn(Map.of(
                "refinedBy", List.of(new EntityReferenceDto("Goal", 4L, "Sub goal"))));
        EntityCommand cmd = new EntityCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";
        cmd.entityType = "Goal";
        cmd.entityId = 3L;
        cmd.neighbors = true;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("refinedBy:").contains("Goal #4").contains("Sub goal");
    }

    @Test
    void entitySingleRendersAsJson() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getEntity("Demo", "Goal", 3L)).thenReturn(Map.of("id", 3, "name", "Login goal"));
        EntityCommand cmd = new EntityCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";
        cmd.entityType = "Goal";
        cmd.entityId = 3L;

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("\"name\" : \"Login goal\"");
    }

    @Test
    void contextRendersBundle() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getProjectContext("Demo")).thenReturn(Map.of("project", Map.of("name", "Demo"),
                "openIssues", List.of()));
        ContextCommand cmd = new ContextCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";

        String out = capture(() -> assertThat(cmd.call()).isEqualTo(ExitCode.SUCCESS));
        assertThat(out).contains("\"project\"").contains("Demo");
    }

    @Test
    void unauthorizedReturnsAuthCode() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.listProjects()).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
        ProjectsCommand cmd = new ProjectsCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;

        assertThat(cmd.call()).isEqualTo(ExitCode.AUTH);
    }

    @Test
    void otherHttpErrorReturnsRequestErrorCode() {
        QueryGateway gw = mock(QueryGateway.class);
        when(gw.getProject("Demo")).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        ProjectCommand cmd = new ProjectCommand();
        cmd.parent = parent(OutputFormat.TEXT);
        cmd.queryOverride = gw;
        cmd.projectName = "Demo";

        assertThat(cmd.call()).isEqualTo(ExitCode.REQUEST_ERROR);
    }

    private static String capture(Runnable body) {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try {
            body.run();
        } finally {
            System.setOut(original);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
