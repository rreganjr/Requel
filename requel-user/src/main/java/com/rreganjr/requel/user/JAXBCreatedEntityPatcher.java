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
package com.rreganjr.requel.user;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import org.xml.sax.SAXException;

import com.rreganjr.requel.user.exception.NoSuchUserException;

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
			throw new SAXException(e);
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
