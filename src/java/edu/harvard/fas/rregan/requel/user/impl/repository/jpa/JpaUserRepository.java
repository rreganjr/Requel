package edu.harvard.fas.rregan.requel.user.impl.repository.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.validator.InvalidStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.jpa.AbstractJpaRepository;
import edu.harvard.fas.rregan.repository.jpa.ConstraintViolationExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.ExceptionMapper;
import edu.harvard.fas.rregan.repository.jpa.GenericPropertyValueExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.InvalidStateExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.OptimisticLockExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.UserPropertyValueExceptionAdapter;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.user.AbstractUserRole;
import edu.harvard.fas.rregan.requel.user.Organization;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserRole;
import edu.harvard.fas.rregan.requel.user.UserRolePermission;
import edu.harvard.fas.rregan.requel.user.UserSet;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchOrganizationException;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;
import edu.harvard.fas.rregan.requel.user.impl.UserSetImpl;

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

		addExceptionAdapter(HibernateOptimisticLockingFailureException.class,
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
