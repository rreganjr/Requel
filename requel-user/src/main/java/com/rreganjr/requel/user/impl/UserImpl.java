/*
 * $Id$
 * Copyright 2008, 2009, 2018 Ron Regan Jr. All Rights Reserved.
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
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
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

import com.rreganjr.requel.user.*;
import org.hibernate.validator.Email;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.Pattern;
import org.hibernate.validator.Size;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import com.rreganjr.requel.user.exception.NoSuchRoleForUserException;
import com.rreganjr.requel.user.exception.UserEntityException;
import com.rreganjr.requel.user.JAXBOrganizedEntityPatcher;
import org.springframework.util.StringUtils;

/**
 * @author ron
 */
@Entity
@Table(name = "users")
@XmlRootElement(name = "user", namespace = "http://www.rreganjr.com/requel")
@XmlType(name = "user", propOrder = { "username", "encryptedPassword", "passwordSalt", "passwordEncryptingAlgorithmName", "passwordEncryptingIterations", "name", "emailAddress",
		"phoneNumber", "organization", "userRoles", "editable" }, namespace = "http://www.rreganjr.com/requel")
public class UserImpl implements User, Serializable {
	static final long serialVersionUID = 0L;

	/**
	 * Algorithm to use for new users or when a user changes their password. This should be
	 * periodically changed as Java supports new methods.
	 */
	private static final String PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM = "PBKDF2WITHHMACSHA512";

	/**
	 * Default algorithm to support passwords encrypted in old releases.
	 */
	private static final String DEFAULT_PASSWORD_ENCRYPTING_ALGORITHM = "MD5";

	/**
	 * Default password salt to support passwords encoded before salt was supported.
	 */
	private static final String DEFAULT_PASSWORD_SALT = "";

	/**
	 * Default password encryption iteration count based on original value.
	 */
	private static final String DEFAULT_PASSWORD_ENCRYPTING_ITERATIONS = "50000";

	private static final Integer PREFERRED_PASSWORD_ENCRYPTING_ITERATIONS = 50000;

	/**
	 * Have a maximum password length so that a huge password can't be supplied as a DOS attack
	 * thanks to comments on
	 * https://nakedsecurity.sophos.com/2013/11/20/serious-security-how-to-store-your-users-passwords-safely/
	 */
	static final Integer MAX_PASSWORD_LENGTH = 128;

	private Long id;
	private String name;
	private String username;
	private String encryptedPassword;
	private String passwordSalt;
	private String passwordEncryptingAlgorithmName;
	private Integer passwordEncryptingIterations;
	private String emailAddress;
	private String phoneNumber;
	private Organization organization;
	private boolean editable = true;
	private Set<UserRole> userRoles;
	// start at 1 so hibernate recognizes the new
	// instance as the initial value and not stale.
	private int version = 1;

	/**
	 * @param username - used for login and as an alias that isn't email or real name.
	 * @param password - required for login
	 * @param name - full name or something like that
	 * @param emailAddress - distinct from username so it can be kept private if desired or shared so that users can
	 *                     communicate outside the system, also the system will send emails to this address.
	 * @param phoneNumber - if users need to communicate quickly, maybe the system could send text messages?
	 * @param organization - a group or company.
	 * @param editable - if the user can change their own data, non-editable for guests.
	 */
	public UserImpl(String username, String password, String name, String emailAddress,
					String phoneNumber, Organization organization, Boolean editable) {
		setUsername(username);
		setPasswordSalt(makePasswordSalt());
		setPasswordEncryptingAlgorithmName(PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM);
		setEmailAddress(emailAddress);
		setOrganization(organization);
		setName(name);
		setPhoneNumber(phoneNumber);
		setEditable(editable);
		setUserRoles(new TreeSet<>(UserRole.UserRoleComparator.INSTANCE));

		resetPassword(password);
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

	@XmlElement(name = "name", defaultValue = "", required = true, namespace = "http://www.rreganjr.com/requel")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(unique = true, nullable = false)
	@NotEmpty(message = "username is required.")
	@XmlElement(name = "username", required = true, namespace = "http://www.rreganjr.com/requel")
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
		boolean matches;
		// because validation rules could change between when a user set their password and when they login don't check
		// the validity, but do check for length to insulate from DOS attacks that try to pass invalid huge passwords.
		// Note 128 was the original maximum length so that value is hard coded in case the max is later lowered a user
		// that set a password when 128 was valid will still be able to login.
		if (getEncryptedPassword() != null && password != null && password.length() <= Math.max(MAX_PASSWORD_LENGTH,128)) {
			matches = getEncryptedPassword().equals(encryptPassword(password));
			if (matches && (
					!(PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM.equals(getPasswordEncryptingAlgorithmName())) ||
					!(PREFERRED_PASSWORD_ENCRYPTING_ITERATIONS.equals(getPasswordEncryptingIterations())))) {
				_resetPassword(password);
			}
		} else {
			matches = false;
		}
		return matches;
	}

	/**
	 * Reset the user's password if the new password meets the password validation constraints.
	 * 
	 * @param password - a string that can't be empty or only whitespace
	 */
	@Override
	public final void resetPassword(String password) {
		if (isValidPassword(password)) {
			_resetPassword(password);
		} else {
			// This is needed because hibernate checks nullability before
			// doing validation and throws a PropertyValueException for
			// the password without validating the rest of the properties.
			if (getEncryptedPassword() == null) {
				setEncryptedPassword("");
			}
		}
	}

	private  void _resetPassword(String password) {
		// When resetting a password use the preferred algorithm and a new salt value.
		setPasswordEncryptingAlgorithmName(PREFERRED_PASSWORD_ENCRYPTING_ALGORITHM);
		setPasswordEncryptingIterations(PREFERRED_PASSWORD_ENCRYPTING_ITERATIONS);
		setPasswordSalt(makePasswordSalt());
		// Order is important, encryptPassword uses above properties
		setEncryptedPassword(encryptPassword(password));
	}

	/**
	 * @return a semi-random value to use for password salt.
	 */
	private static String makePasswordSalt() {
		try {
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[64];
			sr.nextBytes(salt);
			return toHexString(salt);
		} catch (Exception e) {
			throw PasswordException.problemGeneratingPasswordSalt(e);
		}
	}

	/**
	 * @param password - the password from the user
	 * @see SecretKeyFactory#getInstance(String)
	 * @see MessageDigest#getInstance(String)
	 * @return The encoded password as a hexadecimal sequence
	 * based on code from https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
	 */
	private String encryptPassword(String password) {
		byte[] encodedPassword;
		try {
			String algorithmName = (StringUtils.isEmpty(getPasswordEncryptingAlgorithmName()) ? DEFAULT_PASSWORD_ENCRYPTING_ALGORITHM : getPasswordEncryptingAlgorithmName());
			String salt = (StringUtils.isEmpty(getPasswordSalt()) ? DEFAULT_PASSWORD_SALT : getPasswordSalt());
			Integer iterations = (getPasswordEncryptingIterations()==null?Integer.parseInt(DEFAULT_PASSWORD_ENCRYPTING_ITERATIONS): getPasswordEncryptingIterations());
			if (Security.getAlgorithms("SecretKeyFactory").contains(algorithmName.toUpperCase())) {
				PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, 512);
				SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithmName);
				encodedPassword = skf.generateSecret(spec).getEncoded();
			} else if (Security.getAlgorithms("MessageDigest").contains(algorithmName.toUpperCase())) {
				encodedPassword = MessageDigest.getInstance(algorithmName).digest(password.getBytes());
			} else {
				throw PasswordException.badAlgorithmName(algorithmName);
			}
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw PasswordException.problemEncryptingPassword(e);
		}
		return toHexString(encodedPassword);
	}

	/**
	 * @param binaryData
	 *            Array containing the hashed/encrypted value
	 * @return a hexadecimal string representation of the supplied binaryData
	 * based on code from https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
	 */
	private static String toHexString(byte[] binaryData) {
		BigInteger bigInteger = new BigInteger(1, binaryData);
		String hex = bigInteger.toString(16);
		int paddingLength = (binaryData.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0"  + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}

	/**
	 * Test that password matches the rules:
	 * not null or empty or only white space and less than {@link #MAX_PASSWORD_LENGTH}
	 * @param password - the password
	 * @return true if the supplied password passes the validation rules.
	 */
	@Transient
	private boolean isValidPassword(String password) {
		return (password != null) && (password.trim().length() > 0) && password.length() <= MAX_PASSWORD_LENGTH;
	}

	@Column(name="hashed_password", nullable = false)
	@NotEmpty(message = "password is required and both fields must match.")
	@XmlElement(name = "password", required = true, namespace = "http://www.rreganjr.com/requel")
	private String getEncryptedPassword() {
		return encryptedPassword;
	}

	private void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	@XmlElement(name = "passwordSalt", namespace = "http://www.rreganjr.com/requel", defaultValue = DEFAULT_PASSWORD_SALT)
	private String getPasswordSalt() {
		return passwordSalt;
	}

	private void setPasswordSalt(String passwordSalt) {
		this.passwordSalt = passwordSalt;
	}

	@XmlElement(name = "passwordEncryptingAlgorithm", namespace = "http://www.rreganjr.com/requel", defaultValue = DEFAULT_PASSWORD_ENCRYPTING_ALGORITHM)
	private String getPasswordEncryptingAlgorithmName() {
		return passwordEncryptingAlgorithmName;
	}

	private void setPasswordEncryptingAlgorithmName(String passwordEncryptingAlgorithmName) {
		this.passwordEncryptingAlgorithmName = passwordEncryptingAlgorithmName;
	}

	@XmlElement(name = "passwordEncryptingIterations", namespace = "http://www.rreganjr.com/requel", defaultValue = DEFAULT_PASSWORD_ENCRYPTING_ITERATIONS)
	private Integer getPasswordEncryptingIterations() {
		return passwordEncryptingIterations;
	}

	private void setPasswordEncryptingIterations(Integer passwordEncryptingIterations) {
		this.passwordEncryptingIterations = passwordEncryptingIterations;
	}

	@Column(nullable = false)
	@Email
	@NotEmpty(message = "email address is required.")
	@XmlElement(name = "emailAddress", namespace = "http://www.rreganjr.com/requel")
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Pattern(regex = "^((?:\\([2-9]\\d{2}\\)\\ ?|[2-9]\\d{2}(?:\\-?|\\ ?))[2-9]\\d{2}[- ]?\\d{4})?$", message = "must be a valid 10 digit phone number or empty.")
	@XmlElement(name = "phoneNumber", namespace = "http://www.rreganjr.com/requel")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@XmlElementRef(type = OrganizationImpl.class, namespace = "http://www.rreganjr.com/requel")
	@ManyToOne(targetEntity = OrganizationImpl.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER, optional = false)
	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

	/**
	 *
	 * @return true if this can change their information. this is mainly to prevent guests from changing accounts.
	 */
	@XmlElement(name = "editable", namespace = "http://www.rreganjr.com/requel")
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@XmlElementWrapper(name = "userRoles", namespace = "http://www.rreganjr.com/requel")
	@XmlElementRef(type = AbstractUserRole.class)
	@OneToMany(targetEntity = AbstractUserRole.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable(name = "users_user_roles", joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "role_id") })
	@Size(min = 1, message = "one or more roles must be selected.")
	public Set<UserRole> getUserRoles() {
		return userRoles;
	}

	protected void setUserRoles(Set<UserRole> userRoles) {
		this.userRoles = userRoles;
	}

	/**
	 *
	 * @param userRoleType the type of role to retrieve.
	 * @param <T> the type of role.
	 * @return The role for the type supplied.
	 * @throws NoSuchRoleForUserException if the user doesn't have the role.
	 */
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

	/**
	 *
	 * @param userRoleType the type of role to check.
	 * @return true if the user was {@link #grantRole(Class)}ed the role.
	 */
	@Transient
	public boolean hasRole(Class<? extends UserRole> userRoleType) {
		try {
			getRoleForType(userRoleType);
			return true;
		} catch (NoSuchRoleForUserException e) {
			return false;
		}
	}

	/**
	 * If the user doesn't already have the role it will be granted to the user.
	 * @param userRoleType - The type of role to grant.
	 * @throws UserEntityException if role can't be granted.
	 */
	public void grantRole(Class<? extends UserRole> userRoleType) {
		if (!hasRole(userRoleType)) {
			UserRole role;
			Constructor<? extends UserRole> constructor;
			try {
				constructor = userRoleType.getConstructor(User.class);
				role = constructor.newInstance(this);
			} catch (NoSuchMethodException e) {
				try {
					constructor = userRoleType.getConstructor();
					role = constructor.newInstance();
				} catch (Exception e2) {
					throw UserEntityException.exceptionGrantingRole(userRoleType, this, e2);
				}
			} catch (Exception e) {
				throw UserEntityException.exceptionGrantingRole(userRoleType, this, e);
			}
			getUserRoles().add(role);
		}
	}

	/**
	 * If the user has a role of the specified type, the role will be removed.
	 * @param userRoleType - The type of role to remove.
	 * @throws UserEntityException if the role can't be revoked.
	 */
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
				throw UserEntityException.exceptionRevokingRole(userRoleType, this, e);
			}
		}
	}

	/**
	 *
	 * @param other - an other {@link User} object
	 * @return true if both user's have non-null {@link #getId()} that are equal, otherwise false.
	 */
	@Override
	public boolean equalsById(User other) {
		return getId() != null &&
			((UserImpl) other).getId() != null &&
			getId().equals(((UserImpl)other).getId());
	}

	/**
	 *
	 * @param obj - any type of object
	 * @return true if the supplied obj is a {@link User} object and both objects have a non null {@link #getId()} that
	 * 				is equal or if{@link #getUsername()} are equal (null or not), otherwise false.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!User.class.isAssignableFrom(obj.getClass())) {
			return false;
		}
		final UserImpl other = (UserImpl) obj;
		if ((getId() != null) && other.getId() != null) {
			return getId().equals(other.getId());
		}
		if (getUsername() == null) {
			return other.getUsername() == null;
		} else {
			return getUsername().equals(other.getUsername());
		}
	}

	@Override
	public int compareTo(User o) {
		return getUsername().compareToIgnoreCase(o.getUsername());
	}

	private Integer tmpHashCode = null;

	/**
	 * hashcode is by id if defined or username if not, but once it is calculated for a user it won't change while
	 * the object exists. this is because if the object is in a {@link HashMap} or {@link HashSet} if you change a
	 * value that changes the hashcode the object won't be found.
	 * @return a hashcode for this object that won't change once returned.
	 */
	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = getId().hashCode();
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
			tmpHashCode = result;
		}
		return tmpHashCode;
	}

	@Override
	public String toString() {
		return User.class.getName() + "[" + getId() + "]: " + getUsername();
	}

	/**
	 * This is for JAXB to patch-up existing persistent objects for the objects
	 * that are attached directly to this object.
	 * 
	 * @param userRepository - pass in user repository to patch up relationship with an organization.
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
