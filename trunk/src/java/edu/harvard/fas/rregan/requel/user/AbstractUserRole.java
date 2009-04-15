/*
 * $Id: AbstractUserRole.java,v 1.12 2009/01/10 11:08:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.user;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.utils.jaxb.JAXBUserRolePatcher;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "user_roles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING, length = 255)
@XmlType(namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public abstract class AbstractUserRole implements UserRole, Serializable {
	static final long serialVersionUID = 0L;

	protected static final Set<Class<? extends UserRole>> userRoleTypes = new HashSet<Class<? extends UserRole>>();
	// TODO: this is public for DatabaseInitializationListener, find a way for
	// this not to be public
	public static final Map<Class<? extends UserRole>, Set<UserRolePermission>> userRoleTypePermissions = new HashMap<Class<? extends UserRole>, Set<UserRolePermission>>();

	private Long id;
	private Set<UserRolePermission> userRolePermissions = new HashSet<UserRolePermission>();
	private String roleType;
	private int version = 1; // start at 1 so hibernate recognizes the new

	// instance as the initial value and not stale.

	/**
	 * @param roleName
	 */
	protected AbstractUserRole() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Version
	protected int getVersion() {
		return version;
	}

	protected void setVersion(int version) {
		this.version = version;
	}

	@ManyToMany(targetEntity = UserRolePermission.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable(name = "user_roles_permissions", joinColumns = { @JoinColumn(name = "user_role_id") }, inverseJoinColumns = { @JoinColumn(name = "user_role_permission_id") })
	@XmlElementWrapper(name = "userPermissions", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef
	protected Set<UserRolePermission> getUserRolePermissions() {
		return userRolePermissions;
	}

	protected void setUserRolePermissions(Set<UserRolePermission> userRolePermissions) {
		this.userRolePermissions = userRolePermissions;
	}

	@Override
	public void grantUserRolePermission(UserRolePermission permission) {
		getUserRolePermissions().add(permission);
	}

	@Override
	public void revokeUserRolePermission(UserRolePermission permission) {
		getUserRolePermissions().remove(permission);
	}

	public boolean hasUserRolePermission(UserRolePermission permission) {
		return getUserRolePermissions().contains(permission);
	}

	@Transient
	public Set<UserRolePermission> getAvailableUserRolePermissions() {
		return getAvailableUserRolePermissions(this.getClass());
	}

	// this is a hack so that roles can be searched by type
	@Column(name = "role_type", insertable = false, updatable = false)
	protected String getRoleType() {
		return roleType;
	}

	protected void setRoleType(String roleType) {
		this.roleType = roleType;
	}

	@Transient
	public String getRoleName() {
		return getRoleName(this.getClass());
	}

	/**
	 * @return The interface of this object that is a subclass of UserRole
	 *         interface, basically getting the type of the role of a subclass.
	 */
	@Transient
	public Class<?> getUserRoleInterface() {
		Class<?> type = getClass();
		while (UserRole.class.isAssignableFrom(type)) {
			for (Class<?> face : type.getInterfaces()) {
				if (UserRole.class.isAssignableFrom(face)) {
					return face;
				}
			}
			type = type.getSuperclass();
		}
		throw new RuntimeException("The class " + type.getSimpleName()
				+ " is not a descendent of AbstractUserRole.");
	}

	@Override
	public String toString() {
		return getUserRoleInterface().toString() + "[" + getId() + "]";
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			} else {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((getRoleName() == null) ? 0 : getRoleName().hashCode());
				tmpHashCode = new Integer(result);
			}
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final AbstractUserRole other = (AbstractUserRole) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getRoleName() == null) {
			if (other.getRoleName() != null) {
				return false;
			}
		} else if (!getRoleName().equals(other.getRoleName())) {
			return false;
		}
		return true;
	}

	/**
	 * This is for JAXB to patchup existing persistent objects for the objects
	 * that are attached directly to this object.
	 * 
	 * @param userRepository
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository) {
		UnmarshallingContext.getInstance()
				.addPatcher(new JAXBUserRolePatcher(userRepository, this));
	}

	/**
	 * @param userRoleType
	 * @return
	 */
	public static String getRoleName(Class<? extends UserRole> userRoleType) {
		return userRoleType.getSimpleName();
	}

	/**
	 * @param userRoleType
	 * @return
	 */
	public static Set<UserRolePermission> getAvailableUserRolePermissions(
			Class<? extends UserRole> userRoleType) {
		return Collections.unmodifiableSet(userRoleTypePermissions.get(userRoleType));
	}

	/**
	 * @return
	 */
	public static Set<Class<? extends UserRole>> getAvailableUserRoles() {
		return Collections.unmodifiableSet(userRoleTypes);
	}
}
