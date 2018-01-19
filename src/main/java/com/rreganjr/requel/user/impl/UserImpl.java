/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user.impl;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.validator.Email;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.Pattern;
import org.hibernate.validator.Size;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.HashUtils;
import com.rreganjr.requel.user.AbstractUserRole;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.exception.NoSuchRoleForUserException;
import com.rreganjr.requel.user.exception.UserEntityException;
import com.rreganjr.requel.utils.jaxb.JAXBOrganizedEntityPatcher;
import com.rreganjr.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "users")
@XmlRootElement(name = "user", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "user", propOrder = { "username", "hashedPassword", "name", "emailAddress",
		"phoneNumber", "organization", "userRoles", "editable" }, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class UserImpl implements User, Serializable {
	static final long serialVersionUID = 0L;

	private Long id;
	private String name;
	private String username;
	private String hashedPassword;
	private String emailAddress;
	private String phoneNumber;
	private Organization organization;
	private boolean editable = true;
	private Set<UserRole> userRoles;
	// start at 1 so hibernate recognizes the new
	// instance as the initial value and not stale.
	private int version = 1;

	/**
	 * @param username
	 * @param password
	 * @param emailAddress
	 * @param organization
	 */
	public UserImpl(String username, String password, String emailAddress, Organization organization) {
		setUsername(username);
		setUserRoles(new TreeSet<UserRole>(UserRole.UserRoleComparator.INSTANCE));
		resetPassword(password);
		setEmailAddress(emailAddress);
		setOrganization(organization);
	}

	/**
	 * @param username
	 * @param password
	 * @param repassword
	 * @param emailAddress
	 * @param organization
	 */
	public UserImpl(String username, String password, String repassword, String emailAddress,
			Organization organization) {
		setUsername(username);
		setUserRoles(new TreeSet<UserRole>(UserRole.UserRoleComparator.INSTANCE));
		resetPassword(password, repassword);
		setEmailAddress(emailAddress);
		setOrganization(organization);
	}

	/**
	 * @param username
	 * @param password
	 * @param repassword
	 * @param name
	 * @param emailAddress
	 * @param phoneNumber
	 * @param organization
	 * @param editable
	 */
	public UserImpl(String username, String password, String repassword, String name,
			String emailAddress, String phoneNumber, Organization organization, Boolean editable) {
		this(username, password, repassword, emailAddress, organization);
		setName(name);
		setPhoneNumber(phoneNumber);
		setEditable(editable);
	}

	protected UserImpl() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@XmlID
	@XmlAttribute(name = "id")
	@XmlJavaTypeAdapter(IdAdapter.class)
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

	@XmlElement(name = "name", defaultValue = "", required = true, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(unique = true, nullable = false)
	@NotEmpty(message = "username is required.")
	@XmlElement(name = "username", required = true, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Transient
	@Override
	public String getDescriptiveName() {
		if (getName() != null) {
			return getName() + " [" + getUsername() + "]";
		}
		return getUsername();
	}

	@Transient
	public boolean isPassword(String password) {
		return getHashedPassword().equals(HashUtils.getMD5HashDigestString(password));
	}

	/**
	 * Reset the user's password if the password and repassword match.
	 * 
	 * @param password
	 * @param repassword
	 */
	public void resetPassword(String password, String repassword) {
		if ((password != null) && (password.trim().length() > 0)) {
			if (password.equals(repassword)) {
				resetPassword(password);
			} else {
				// This is needed because hibernate checks nullability before
				// doing validation and throws a PropertyValueException for
				// the password without validating the rest of the properties.
				setHashedPassword("");
			}
		} else {
			// This is needed because hibernate checks nullability before
			// doing validation and throws a PropertyValueException for
			// the password without validating the rest of the properties.
			if (getHashedPassword() == null) {
				setHashedPassword("");
			}
		}
	}

	public void resetPassword(String password) {
		if ((password != null) && (password.trim().length() > 0)) {
			setHashedPassword(HashUtils.getMD5HashDigestString(password));
		} else {
			// This is needed because hibernate checks nullability before
			// doing validation and throws a PropertyValueException for
			// the password without validating the rest of the properties.
			if (getHashedPassword() == null) {
				setHashedPassword("");
			}
		}
	}

	@Column(nullable = false)
	@NotEmpty(message = "password is required and both fields must match.")
	@XmlElement(name = "password", required = true, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	protected String getHashedPassword() {
		return hashedPassword;
	}

	protected void setHashedPassword(String hashedPassword) {
		this.hashedPassword = hashedPassword;
	}

	@Column(nullable = false)
	@Email
	@NotEmpty(message = "email address is required.")
	@XmlElement(name = "emailAddress", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Pattern(regex = "^((?:\\([2-9]\\d{2}\\)\\ ?|[2-9]\\d{2}(?:\\-?|\\ ?))[2-9]\\d{2}[- ]?\\d{4})?$", message = "must be a valid 10 digit phone number or empty.")
	@XmlElement(name = "phoneNumber", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@XmlElementRef(type = OrganizationImpl.class, namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@ManyToOne(targetEntity = OrganizationImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER, optional = false)
	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	@XmlElement(name = "editable", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@XmlElementWrapper(name = "userRoles", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	@XmlElementRef(type = AbstractUserRole.class)
	@OneToMany(targetEntity = AbstractUserRole.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable(name = "users_user_roles", joinColumns = { @JoinColumn(name = "user_id") }, inverseJoinColumns = { @JoinColumn(name = "role_id") })
	@Size(min = 1, message = "one or more roles must be selected.")
	public Set<UserRole> getUserRoles() {
		return userRoles;
	}

	protected void setUserRoles(Set<UserRole> userRoles) {
		this.userRoles = userRoles;
	}

	@Transient
	public <T extends UserRole> T getRoleForType(Class<T> userRoleType)
			throws NoSuchRoleForUserException {
		for (UserRole role : getUserRoles()) {
			if (userRoleType.isAssignableFrom(role.getClass())) {
				return userRoleType.cast(role);
			}
		}
		throw NoSuchRoleForUserException.forUserRoleTypeName(this, userRoleType);
	}

	@Transient
	public boolean hasRole(Class<? extends UserRole> userRoleType) {
		try {
			getRoleForType(userRoleType);
			return true;
		} catch (NoSuchRoleForUserException e) {
			return false;
		}
	}

	public void grantRole(Class<? extends UserRole> userRoleType) {
		if (!hasRole(userRoleType)) {
			UserRole role = null;
			Constructor<? extends UserRole> constructor = null;
			try {
				constructor = userRoleType.getConstructor(User.class);
				role = constructor.newInstance(this);
			} catch (NoSuchMethodException e) {
				try {
					constructor = userRoleType.getConstructor();
					role = constructor.newInstance();
				} catch (Exception e2) {
					UserEntityException.exceptionGrantingRole(userRoleType, this, e2);
				}
			} catch (Exception e) {
				UserEntityException.exceptionGrantingRole(userRoleType, this, e);
			}
			getUserRoles().add(role);
		}
	}

	public void revokeRole(Class<? extends UserRole> userRoleType) {
		if (hasRole(userRoleType)) {
			UserRole role = getRoleForType(userRoleType);
			getUserRoles().remove(role);
			try {
				Method setUser = role.getClass().getDeclaredMethod("setUser", User.class);
				setUser.setAccessible(true);
				setUser.invoke(role, new Object[] { null });
			} catch (NoSuchMethodException e) {
				// expected if the role is shared by multiple users
			} catch (Exception e) {
				UserEntityException.exceptionRevokingRole(userRoleType, this, e);
			}
		}
	}

	@Override
	public boolean equalsById(User other) {
		if ((getId() != null) && (((UserImpl) other).getId() != null)) {
			return getId().equals(((UserImpl) other).getId());
		}
		return false;
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
		final UserImpl other = (UserImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getUsername() == null) {
			if (other.getUsername() != null) {
				return false;
			}
		} else if (!getUsername().equals(other.getUsername())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(User o) {
		return getUsername().compareToIgnoreCase(o.getUsername());
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public String toString() {
		return User.class.getName() + "[" + getId() + "]: " + getUsername();
	}

	/**
	 * This is for JAXB to patchup existing persistent objects for the objects
	 * that are attached directly to this object.
	 * 
	 * @param userRepository
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(UserRepository userRepository) {
		UnmarshallingContext.getInstance().addPatcher(
				new JAXBOrganizedEntityPatcher(userRepository, this));
	}

	/**
	 * This class is used by JAXB to convert the id of an entity into an xml id
	 * string that will be distinct from other entity xml id strings by the use
	 * of a prefix.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class IdAdapter extends XmlAdapter<String, Long> {
		private static final String prefix = "USR_";

		@Override
		public Long unmarshal(String id) throws Exception {
			return null; // new Long(id.substring(prefix.length()));
		}

		@Override
		public String marshal(Long id) throws Exception {
			if (id != null) {
				return prefix + id.toString();
			}
			return "";
		}
	}
}
