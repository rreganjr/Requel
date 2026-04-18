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
package com.rreganjr.requel.project.impl.command;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.command.DeleteReportGeneratorCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for report generator management commands:
 * {@link EditReportGeneratorCommand} and {@link DeleteReportGeneratorCommand}.
 *
 * A ReportGenerator stores an XSLT stylesheet that transforms the project XML
 * export into a formatted report. These tests verify persistence and removal
 * only; the actual XSLT transformation (GenerateReportCommand) is deferred.
 */
public class ReportGeneratorCommandTest extends AbstractIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static final String MINIMAL_XSLT =
            "<?xml version=\"1.0\"?><xsl:stylesheet version=\"1.0\" " +
            "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
            "<xsl:template match=\"/\"><output/></xsl:template>" +
            "</xsl:stylesheet>";

    private Project createProject(String label) throws Exception {
        long ts = System.currentTimeMillis();
        User admin = getUserRepository().findUserByUsername("admin");
        EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
        cmd.setEditedBy(admin);
        cmd.setName(label + "-" + ts);
        cmd.setText("test project for " + label);
        cmd.setOrganizationName("ReportTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private ReportGenerator createReportGenerator(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditReportGeneratorCommand cmd = getProjectCommandFactory().newEditReportGeneratorCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName(name);
        cmd.setText(MINIMAL_XSLT);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getReportGenerator();
    }

    // -------------------------------------------------------------------------
    // EditReportGeneratorCommand
    // -------------------------------------------------------------------------

    @Test
    public void createReportGenerator() throws Exception {
        Project project = createProject("Report-create");

        ReportGenerator rg = createReportGenerator(project, "Summary Report");

        assertNotNull(rg, "report generator should have been created");
        assertEquals("Summary Report", rg.getName(), "name should match");
        assertEquals(MINIMAL_XSLT, rg.getText(), "XSLT content should match");

        // Verify it is findable via the project
        Project reloaded = getProjectRepository().findProjectByName(project.getName());
        assertTrue(reloaded.getReportGenerators().stream()
                        .anyMatch(r -> "Summary Report".equals(r.getName())),
                "report generator should appear in project's report generators");
    }

    @Test
    public void editExistingReportGenerator() throws Exception {
        Project project = createProject("Report-edit");
        ReportGenerator original = createReportGenerator(project, "Editable Report");
        User admin = getUserRepository().findUserByUsername("admin");

        EditReportGeneratorCommand cmd = getProjectCommandFactory().newEditReportGeneratorCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setReportGenerator(original);
        cmd.setName("Editable Report Updated");
        cmd.setText("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"/\"><updated/></xsl:template></xsl:stylesheet>");
        cmd = getCommandHandler().execute(cmd);

        ReportGenerator updated = cmd.getReportGenerator();
        assertEquals(original.getId(), updated.getId(), "edit should update the same report generator");
        assertEquals("Editable Report Updated", updated.getName(), "name should be updated");
        assertTrue(updated.getText().contains("<updated/>"), "updated XSLT should be saved");
    }

    @Test
    public void duplicateReportGeneratorNameIsRejected() throws Exception {
        Project project = createProject("Report-duplicate");
        createReportGenerator(project, "Existing Report");
        User admin = getUserRepository().findUserByUsername("admin");

        EditReportGeneratorCommand cmd = getProjectCommandFactory().newEditReportGeneratorCommand();
        cmd.setEditedBy(admin);
        cmd.setProjectOrDomain(project);
        cmd.setName("Existing Report");
        cmd.setText(MINIMAL_XSLT);

        assertThrows(EntityException.class, () -> getCommandHandler().execute(cmd),
                "creating a duplicate report generator name should fail");
    }

    // -------------------------------------------------------------------------
    // DeleteReportGeneratorCommand
    // -------------------------------------------------------------------------

    @Test
    public void deleteReportGenerator() throws Exception {
        Project project = createProject("Report-delete");
        ReportGenerator rg = createReportGenerator(project, "Report To Delete");

        // Confirm it is present before delete
        Project before = getProjectRepository().findProjectByName(project.getName());
        assertTrue(before.getReportGenerators().stream()
                        .anyMatch(r -> "Report To Delete".equals(r.getName())),
                "pre-condition: report generator should be present before delete");

        User admin = getUserRepository().findUserByUsername("admin");
        DeleteReportGeneratorCommand deleteCmd =
                getProjectCommandFactory().newDeleteReportGeneratorCommand();
        deleteCmd.setEditedBy(admin);
        deleteCmd.setReportGenerator(rg);
        getCommandHandler().execute(deleteCmd);

        // Verify it is gone from the project
        Project after = getProjectRepository().findProjectByName(project.getName());
        assertTrue(after.getReportGenerators().stream()
                        .noneMatch(r -> "Report To Delete".equals(r.getName())),
                "report generator should be absent from project after delete");
    }
}
