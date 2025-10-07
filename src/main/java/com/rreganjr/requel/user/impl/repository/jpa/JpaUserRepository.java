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
package com.rreganjr.requel.user.impl.repository.jpa;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;

import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import com.rreganjr.validator.InvalidStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.repository.EntityException;
import com.rreganjr.repository.EntityExceptionActionType;
import com.rreganjr.repository.jpa.AbstractJpaRepository;
import com.rreganjr.repository.jpa.ConstraintViolationExceptionAdapter;
import com.rreganjr.repository.jpa.ExceptionMapper;
import com.rreganjr.repository.jpa.GenericPropertyValueExceptionAdapter;
import com.rreganjr.repository.jpa.InvalidStateExceptionAdapter;
import com.rreganjr.repository.jpa.OptimisticLockExceptionAdapter;
import com.rreganjr.repository.jpa.UserPropertyValueExceptionAdapter;
import com.rreganjr.requel.NoSuchEntityException;
import com.rreganjr.requel.user.AbstractUserRole;
import com.rreganjr.requel.user.Organization;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.UserSet;
import com.rreganjr.requel.user.exception.NoSuchOrganizationException;
import com.rreganjr.requel.user.exception.NoSuchUserException;
import com.rreganjr.requel.user.impl.UserSetImpl;

/**
 * @author ron
 */
@Repository("userRepository")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { NoSuchUserException.class,
		NoSuchOrganizationException.class, EntityException.class })
public class JpaUserRepository extends AbstractJpaRepository implements UserRepository {

	/**
	 * 
	 */
	@Autowired
	public JpaUserRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
		addExceptionAdapter(PropertyValueException.class, new UserPropertyValueExceptionAdapter(),
				User.class);
		addExceptionAdapter(PropertyValueException.class,
				new GenericPropertyValueExceptionAdapter(), Organization.class, UserRole.class,
				UserRolePermission.class);

		addExceptionAdapter(InvalidStateException.class, new InvalidStateExceptionAdapter(),
				User.class, Organization.class, UserRole.class, UserRolePermission.class);

		addExceptionAdapter(ConstraintViolationException.class,
				new ConstraintViolationExceptionAdapter("username"), User.class);

		addExceptionAdapter(OptimisticLockException.class, new OptimisticLockExceptionAdapter(),
				User.class, Organization.class, UserRole.class, UserRolePermission.class);

		addExceptionAdapter(StaleObjectStateException.class, new OptimisticLockExceptionAdapter(),
				User.class, Organization.class, UserRole.class, UserRolePermission.class);

		addExceptionAdapter(LockAcquisitionException.class, new OptimisticLockExceptionAdapter(),
				User.class, Organization.class, UserRole.class, UserRolePermission.class);

		addExceptionAdapter(CannotAcquireLockException.class, new OptimisticLockExceptionAdapter(),
				User.class, Organization.class, UserRole.class, UserRolePermission.class);

        addExceptionAdapter(ObjectOptimisticLockingFailureException.class,
                new OptimisticLockExceptionAdapter(), User.class, Organization.class,
                UserRole.class, UserRolePermission.class);

	}

	public User findUserByUsername(String username) throws NoSuchUserException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(user) from UserImpl as user where user.username like :username");
			query.setParameter("username", username);
			return (User) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchUserException.forUsername(username);
		} catch (Exception e) {
			throw convertException(e, User.class, null, EntityExceptionActionType.Reading);
		}
	}

	public UserSet findUsers() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(user) from UserImpl as user");
			return new UserSetImpl(query.getResultList());
		} catch (NoResultException e) {
			return (UserSet) new HashSet<User>();
		} catch (Exception e) {
			throw convertException(e, UserSet.class, null, EntityExceptionActionType.Reading);
		}
	}

	public UserSet findUsersForRole(Class<? extends UserRole> roleType) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(user) from UserImpl as user inner join user.userRoles as roles, AbstractUserRole role where role.roleType like :roleType and role in roles");
			query.setParameter("roleType", roleType.getName());
			return new UserSetImpl(query.getResultList());
		} catch (NoResultException e) {
			return (UserSet) new HashSet<User>();
		} catch (Exception e) {
			throw convertException(e, UserSet.class, null, EntityExceptionActionType.Reading);
		}
	}

	public Organization findOrganizationByName(String name) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(organization) from OrganizationImpl as organization where name like :name");
			query.setParameter("name", name);
			return (Organization) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchOrganizationException.forName(name);
		} catch (Exception e) {
			throw convertException(e, Organization.class, null, EntityExceptionActionType.Reading);
		}
	}

	public Set<Organization> findOrganizations() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(organization) from OrganizationImpl as organization");
			return new HashSet<Organization>(query.getResultList());
		} catch (Exception e) {
			throw convertException(e, Organization.class, null, EntityExceptionActionType.Reading);
		}
	}

	public Set<String> getOrganizationNames() {
		Set<String> orgNames = new HashSet<String>();
		for (Organization org : findOrganizations()) {
			orgNames.add(org.getName());
		}
		return orgNames;
	}

	public UserRolePermission findUserRolePermission(Class<? extends UserRole> userRoleType,
			String name) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(permission) from UserRolePermission as permission where userRoleType like :userRoleType and name like :name");
			query.setParameter("userRoleType", userRoleType.getName());
			query.setParameter("name", name);
			return (UserRolePermission) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(UserRolePermission.class, "name", name);
		} catch (Exception e) {
			throw convertException(e, Organization.class, null, EntityExceptionActionType.Reading);
		}
	}

	public Set<UserRolePermission> findUserRolePermissions(Class<? extends UserRole> userRoleType) {
		Set<UserRolePermission> permissions = new HashSet<UserRolePermission>();
		for (UserRolePermission permission : AbstractUserRole
				.getAvailableUserRolePermissions(userRoleType)) {
			permissions.add(merge(permission));
		}
		return permissions;
	}

	public Set<Class<? extends UserRole>> findUserRoleTypes() {
		return AbstractUserRole.getAvailableUserRoles();
	}
}
