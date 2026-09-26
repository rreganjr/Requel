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

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresRolePermission;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermissionOrSystemAdminRole;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.platform.identity.Role;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import org.junit.jupiter.api.Test;

/** Issue #296: how a command's authorization requirement reads as a hint. */
class AuthorizationHintsTest {

    /** A role with no phrase of its own, to show the fallback to the type name. */
    private interface ReviewerUserRole extends Role {
    }

    @Test
    void aStakeholderPermissionReadsAsEntityAndPermission() {
        assertThat(AuthorizationHints.render(new RequiresStakeholderPermission(Goal.class, "Edit")))
                .isEqualTo("Goal[Edit]");
    }

    @Test
    void theAdministratorAlternativeIsNamed() {
        assertThat(AuthorizationHints.render(new RequiresStakeholderPermissionOrSystemAdminRole(
                Project.class, "Delete", SystemAdminUserRole.class)))
                .isEqualTo("Project[Delete] or system administrator");
    }

    @Test
    void aSystemRoleReadsAsAPhraseOrItsTypeName() {
        assertThat(AuthorizationHints.render(new RequiresSystemRole(SystemAdminUserRole.class)))
                .isEqualTo("system administrator");
        assertThat(AuthorizationHints.render(new RequiresSystemRole(ReviewerUserRole.class)))
                .isEqualTo("ReviewerUserRole");
    }

    @Test
    void aRolePermissionIsNamed() {
        assertThat(AuthorizationHints.render(new RequiresRolePermission("createProjects")))
                .isEqualTo("createProjects role permission");
    }

    @Test
    void noRequirementMeansNoHint() {
        assertThat(AuthorizationHints.render(null)).isNull();
    }

    /**
     * {@link AuthorizationHints#render} is an if-chain rather than a pattern switch (Java 17), so
     * the compiler cannot check it covers the sealed interface. This pins the four kinds the
     * tests above render, so a new kind fails here until it has a branch and a test.
     */
    @Test
    void everyKindOfRequirementHasARendering() {
        assertThat(AuthorizationRequirement.class.getPermittedSubclasses())
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("RequiresSystemRole", "RequiresRolePermission",
                        "RequiresStakeholderPermission",
                        "RequiresStakeholderPermissionOrSystemAdminRole");
    }
}
