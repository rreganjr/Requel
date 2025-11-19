package com.rreganjr.requel.imports.identity;

/**
 * Minimal user import draft to resolve identities during import.
 */
public class UserImportDraft {
    private final String externalId;
    private final String username;
    private final String name;
    private final String email;
    private final String organizationName;
    private final boolean editable;
    private final String encryptedPassword;
    private final String passwordSalt;
    private final String passwordAlgorithm;
    private final String passwordIterations;
    private final java.util.List<UserRoleImportDraft> roles;

    public UserImportDraft(String externalId, String username, String name, String email, String organizationName,
                           boolean editable, String encryptedPassword, String passwordSalt,
                           String passwordAlgorithm, String passwordIterations,
                           java.util.List<UserRoleImportDraft> roles) {
        this.externalId = externalId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.organizationName = organizationName;
        this.editable = editable;
        this.encryptedPassword = encryptedPassword;
        this.passwordSalt = passwordSalt;
        this.passwordAlgorithm = passwordAlgorithm;
        this.passwordIterations = passwordIterations;
        this.roles = roles == null ? java.util.List.of() : java.util.List.copyOf(roles);
    }

    public String getExternalId() {
        return externalId;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public boolean isEditable() {
        return editable;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    public String getPasswordIterations() {
        return passwordIterations;
    }

    public java.util.List<UserRoleImportDraft> getRoles() {
        return roles;
    }
}
