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
package com.rreganjr.requel.utils.jaxb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import com.rreganjr.repository.Repository;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * @author ron
 */
public class UnmarshallerListener extends Unmarshaller.Listener {
	protected static final Logger log = Logger.getLogger(UnmarshallerListener.class);

	// a list of the possible before/afterUnmarshal method parameters
	private static final List<Class<?>[]> beforeAndAfterUnmarshalMethodParams = new ArrayList<Class<?>[]>();
	static {
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] {});
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { String.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { Object.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { Repository.class, Object.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { ProjectRepository.class,
				UserRepository.class, Object.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { ProjectRepository.class,
				Object.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { UserRepository.class, User.class,
				Object.class });
		beforeAndAfterUnmarshalMethodParams
				.add(new Class<?>[] { UserRepository.class, Object.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { ProjectRepository.class });
		beforeAndAfterUnmarshalMethodParams.add(new Class<?>[] { UserRepository.class });
		beforeAndAfterUnmarshalMethodParams
				.add(new Class<?>[] { UserRepository.class, User.class });
	}

	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final User defaultUser;
	private final String projectNameOverride;

	/**
	 * @param projectRepository
	 * @param userRepository
	 * @param defaultUser
	 * @param projectNameOverride
	 */
	public UnmarshallerListener(ProjectRepository projectRepository, UserRepository userRepository,
			User defaultUser, String projectNameOverride) {
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.defaultUser = defaultUser;
		this.projectNameOverride = projectNameOverride;
	}

	@Override
	public void beforeUnmarshal(Object target, Object parent) {
		super.beforeUnmarshal(target, parent);
		findAndCallUnmarshalMethod("beforeUnmarshal", target, parent);
	}

	@Override
	public void afterUnmarshal(Object target, Object parent) {
		super.afterUnmarshal(target, parent);
		findAndCallUnmarshalMethod("afterUnmarshal", target, parent);
	}

	/**
	 * Search for a method with the supplied name and predefined possible method
	 * parameters, and if found call the method.
	 * 
	 * @param methodName
	 * @param target
	 * @param parent
	 */
	private void findAndCallUnmarshalMethod(String methodName, Object target, Object parent) {
		Class<?> targetType = target.getClass();
		Method afterUnmarshalMethod = null;
		Object[] params = null;
		try {
			// search for the method starting with the most specific class
			// and working up the inheritence hierarchy
			while (targetType != null) {
				for (Class<?>[] methodParamTypes : beforeAndAfterUnmarshalMethodParams) {
					try {
						afterUnmarshalMethod = targetType.getDeclaredMethod(methodName,
								methodParamTypes);
						afterUnmarshalMethod.setAccessible(true);
						params = new Object[methodParamTypes.length];
						for (int i = 0; i < methodParamTypes.length; i++) {
							if (methodParamTypes[i].equals(ProjectRepository.class)) {
								params[i] = projectRepository;
							} else if (methodParamTypes[i].equals(UserRepository.class)) {
								params[i] = userRepository;
							} else if (methodParamTypes[i].equals(Repository.class)) {
								params[i] = userRepository;
							} else if (methodParamTypes[i].equals(User.class)) {
								params[i] = defaultUser;
							} else if (methodParamTypes[i].equals(String.class)) {
								// NOTE: this assumes that only a project will
								// have an before/afterUnmarshall method that
								// takes a string.
								params[i] = projectNameOverride;
							} else if (methodParamTypes[i].equals(Object.class)) {
								params[i] = parent;
							}
						}
						afterUnmarshalMethod.invoke(target, params);
					} catch (NoSuchMethodException e) {
					} catch (SecurityException e) {
						log.error("The afterUnmarshal method for the class " + targetType.getName()
								+ " is not accessible.", e);
					}
				}
				targetType = targetType.getSuperclass();
			}
		} catch (Exception e) {
			log.error("Exception processing afterUnmarshal(" + params + ") on class " + targetType
					+ " for object " + target, e);
		}
	}
}