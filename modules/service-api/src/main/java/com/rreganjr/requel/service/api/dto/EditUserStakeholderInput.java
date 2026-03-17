package com.rreganjr.requel.service.api.dto;

import java.util.List;

/**
 * Input DTO for creating or editing a user-backed stakeholder.
 *
 * @param projectName     project to add the stakeholder to
 * @param username        system user to link as a stakeholder
 * @param teamName        optional team assignment (find-or-create)
 * @param permissionKeys  stakeholder permission keys (e.g. "com.rreganjr.requel.project.Goal[Edit]")
 * @param version         optimistic lock version (null for create)
 */
public record EditUserStakeholderInput(
        String projectName,
        String username,
        String teamName,
        List<String> permissionKeys,
        Integer version
) {
}
