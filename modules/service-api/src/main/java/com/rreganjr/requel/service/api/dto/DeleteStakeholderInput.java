package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for deleting a stakeholder (either type).
 *
 * @param projectName    project the stakeholder belongs to
 * @param stakeholderId  id of the stakeholder to delete
 * @param version        optimistic lock version
 */
public record DeleteStakeholderInput(
        String projectName,
        Long stakeholderId,
        Integer version
) {
}
