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
