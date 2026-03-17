package com.rreganjr.requel.service.api.dto;

/**
 * Input DTO for creating or editing a non-user stakeholder (external authority).
 *
 * @param projectName  project to add the stakeholder to
 * @param name         stakeholder name (e.g. "Financial Accounting Standards Board")
 * @param text         description of the stakeholder
 * @param version      optimistic lock version (null for create)
 */
public record EditNonUserStakeholderInput(
        String projectName,
        String name,
        String text,
        Integer version
) {
}
