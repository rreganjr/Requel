package com.rreganjr.requel.imports.project;

/**
 * Minimal stakeholder draft linking a user into a project.
 */
public class StakeholderImportDraft {
    private final String externalId;
    private final String createdByExternalId;
    private final String userExternalId;

    public StakeholderImportDraft(String externalId, String createdByExternalId, String userExternalId) {
        this.externalId = externalId;
        this.createdByExternalId = createdByExternalId;
        this.userExternalId = userExternalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCreatedByExternalId() {
        return createdByExternalId;
    }

    public String getUserExternalId() {
        return userExternalId;
    }
}
