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
package com.rreganjr.requel.service.gateway;

import com.rreganjr.requel.gateway.CommandPolicy;
import com.rreganjr.requel.gateway.DefaultCommandPolicy;
import com.rreganjr.requel.project.ProjectRepository;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the gateway's {@link CommandPolicy}: a <strong>default-deny</strong>
 * {@link DefaultCommandPolicy} with an explicit allowlist (the "anything a project user can do"
 * surface) and an explicit denylist (the boundary that must never be crossed), decorated by the
 * {@link NonUserStakeholderDeletePolicy} for the input-aware non-user-stakeholder delete guard.
 * <p>
 * The allowlist is deliberately curated rather than derived from "everything registered", so a
 * newly registered command is <em>not</em> auto-exposed until it is consciously added here. Every
 * allowlisted command implements {@code AuthorizableCommand} (verified by
 * {@code GatewayPolicyTest}), so the gateway never exposes a write that bypasses per-stakeholder
 * authorization. The denylist is authoritative and wins over the allowlist (belt-and-braces for
 * identity commands).
 */
@Configuration
public class GatewayPolicyConfig {

    /**
     * The curated gateway write surface. Each entry implements {@code AuthorizableCommand} (so it
     * is permission-checked in the handler chain) and represents an action a project member may
     * legitimately drive from an external client. User/identity management is intentionally
     * absent.
     */
    public static final Set<String> ALLOWED = Set.of(
            // Project
            "EditProject",
            // Stakeholders (non-user only; DeleteStakeholder additionally input-guarded)
            "EditNonUserStakeholder", "DeleteStakeholder",
            // Goals
            "EditGoal", "EditGoalRelation", "DeleteGoalRelation", "CopyGoal", "DeleteGoal",
            "AddGoalToGoalContainer", "RemoveGoalFromGoalContainer",
            // Stories
            "EditStory", "CopyStory", "DeleteStory",
            "AddStoryToStoryContainer", "RemoveStoryFromStoryContainer",
            // Actors
            "EditActor", "CopyActor", "DeleteActor",
            "AddActorToActorContainer", "RemoveActorFromActorContainer",
            // Use cases
            "EditUseCase", "CopyUseCase", "DeleteUseCase",
            "AddScenarioToUseCase", "RemoveScenarioFromUseCase", "SetPrimaryScenarioOnUseCase",
            // Scenarios (EditScenario / ConvertStepToScenario inherit auth from EditScenarioStep)
            "EditScenario", "CopyScenario", "DeleteScenario",
            "EditScenarioStep", "CopyScenarioStep", "ConvertStepToScenario", "DeleteScenarioStep",
            // Glossary
            "EditGlossaryTerm", "DeleteGlossaryTerm",
            // Reports (definition only; GenerateReport produces a file and is denied)
            "EditReportGenerator", "DeleteReportGenerator",
            // Annotations / IBIS
            "EditNote", "DeleteNote", "EditIssue", "DeleteIssue",
            "EditPosition", "DeletePosition", "EditArgument", "DeleteArgument",
            // Tagging / categorization (tags reuse the Annotation stakeholder permission:
            // Edit to create/assign, Delete to delete; global/system tags require admin)
            "EditTag", "DeleteTag", "AssignTag", "UnassignTag",
            "EditTagCategory", "DeleteTagCategory");

    /**
     * Commands that must never be exposed, even if mistakenly added to the allowlist. Covers
     * identity/user management, file-transfer commands (multipart import/export, report
     * generation), and assistant/structural-internal commands that are not independently
     * permission-checked (no {@code AuthorizableCommand}).
     */
    public static final Set<String> DENIED = Set.of(
            // Identity / user management — out of scope by design
            "Login", "EditUser", "EditUserStakeholder",
            // File transfer / generation (not plain JSON commands)
            "ImportProject", "ExportProject", "GenerateReport",
            // Not independently authorized (assistant / structural-internal)
            "ReplaceGlossaryTerm", "EditAddWordToGlossaryPosition", "EditAddActorToProjectPosition",
            "RemoveUnneedLexicalIssues", "EditLexicalIssue", "EditChangeSpellingPosition",
            "EditAddWordToDictionaryPosition", "ResolveIssue", "RemoveAnnotationFromAnnotatable");

    @Bean
    public CommandPolicy gatewayCommandPolicy(ProjectRepository projectRepository) {
        CommandPolicy base = new DefaultCommandPolicy(ALLOWED, DENIED);
        return new NonUserStakeholderDeletePolicy(base, projectRepository);
    }
}
