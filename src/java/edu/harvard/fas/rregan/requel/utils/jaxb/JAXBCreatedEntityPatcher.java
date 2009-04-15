/*
 * $Id: JAXBCreatedEntityPatcher.java,v 1.5 2009/01/07 09:50:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * This is used to swap the createdBy User objects on CreatedEntity objects
 * assigned by the JAXB Unmarshaller with existing users from the database.
 * 
 * @author ron
 */
public class JAXBCreatedEntityPatcher implements Patcher {

	private final CreatedEntity createdEntity;
	private final UserRepository userRepository;
	private final User defaultCreatedByUser;

	/**
	 * @param userRepository
	 * @param createdEntity
	 * @param defaultCreatedByUser
	 */
	public JAXBCreatedEntityPatcher(UserRepository userRepository, CreatedEntity createdEntity,
			User defaultCreatedByUser) {
		this.userRepository = userRepository;
		this.createdEntity = createdEntity;
		this.defaultCreatedByUser = defaultCreatedByUser;
	}

	@Override
	public void run() throws SAXException {
		try {
			if (createdEntity.getCreatedBy() != null) {
				try {
					User existingUser = userRepository.findUserByUsername(createdEntity
							.getCreatedBy().getUsername());
					setCreatedBy(createdEntity, existingUser);
				} catch (NoSuchUserException e) {
					// new user
					// TODO: the following causes an exception because the
					// JAXBUserRolePermissionPatcher hasn't run yet to map
					// the persistant permissions to the ones created by
					// the xml. Solved by making references to User in
					// implementors of CreatedEntity cascade
					// userRepository.persist(createdEntity.getCreatedBy());
				}
			} else {
				setCreatedBy(createdEntity, defaultCreatedByUser);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException2(e);
		}
	}

	private void setCreatedBy(CreatedEntity createdEntity, User user)
			throws IllegalAccessException, InvocationTargetException {
		Class<?> entityType = createdEntity.getClass();
		while (entityType != null) {
			try {
				Method createdBySetter = entityType.getDeclaredMethod("setCreatedBy", User.class);
				createdBySetter.setAccessible(true);
				createdBySetter.invoke(createdEntity, user);
				return;
			} catch (NoSuchMethodException e) {
				// try super class
				entityType = entityType.getSuperclass();
			}
		}
	}
}
