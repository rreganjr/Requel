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
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "userRoles", namespace = "http://www.rreganjr.com/requel")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserRoleImportXml {

    @XmlAnyElement(lax = true)
    private List<Object> roles = new ArrayList<>();

    public List<Object> getRoles() {
        return roles;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProjectUserRoleXml {
        @XmlElement(name = "userPermissions", namespace = "http://www.rreganjr.com/requel")
        private UserPermissionContainer userPermissions = new UserPermissionContainer();

        public UserPermissionContainer getUserPermissions() {
            return userPermissions;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SystemAdminUserRoleXml {
        // no specific fields
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UserPermissionContainer {
        @XmlElement(name = "userPermission", namespace = "http://www.rreganjr.com/requel")
        private List<UserPermissionXml> permissions = new ArrayList<>();

        public List<UserPermissionXml> getPermissions() {
            return permissions;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class UserPermissionXml {
        @XmlAttribute(name = "name")
        private String name;
        public String getName() { return name; }
    }
}
