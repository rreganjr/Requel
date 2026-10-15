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
package com.rreganjr.requel.utils.jaxb.imports;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "user", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserImportXml {

    @XmlAttribute(name = "id")
    private String id;

    @XmlElement(name = "username", namespace = "http://www.rreganjr.com/requel")
    private String username;

    @XmlElement(name = "password", namespace = "http://www.rreganjr.com/requel")
    private String encryptedPassword;

    @XmlElement(name = "passwordSalt", namespace = "http://www.rreganjr.com/requel")
    private String passwordSalt;

    @XmlElement(name = "passwordEncryptingAlgorithmName", namespace = "http://www.rreganjr.com/requel")
    private String passwordAlgorithm;

    @XmlElement(name = "passwordEncryptingIterations", namespace = "http://www.rreganjr.com/requel")
    private String passwordIterations;

    @XmlElement(name = "name", namespace = "http://www.rreganjr.com/requel")
    private String name;

    @XmlElement(name = "emailAddress", namespace = "http://www.rreganjr.com/requel")
    private String email;

    @XmlElement(name = "organization", namespace = "http://www.rreganjr.com/requel")
    private OrganizationXml organization;

    @XmlElement(name = "editable", namespace = "http://www.rreganjr.com/requel")
    private Boolean editable;

    @XmlElement(name = "userRoles", namespace = "http://www.rreganjr.com/requel")
    private UserRoleImportXml userRoles;

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getPasswordAlgorithm() { return passwordAlgorithm; }
    public String getPasswordIterations() { return passwordIterations; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public OrganizationXml getOrganization() { return organization; }
    public Boolean getEditable() { return editable; }
    public UserRoleImportXml getUserRoles() { return userRoles; }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OrganizationXml {
        @XmlAttribute(name = "name")
        private String name;
        public String getName() { return name; }
    }
}
