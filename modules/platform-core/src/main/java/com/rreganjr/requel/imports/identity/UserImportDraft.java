/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
