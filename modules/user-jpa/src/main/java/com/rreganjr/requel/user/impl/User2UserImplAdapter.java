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

import com.rreganjr.platform.identity.User;
import com.rreganjr.repository.jpa.EntityProxyInterceptor;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Adapter for JAXB to convert interface User to class UserImpl and back.
 *
 * @author ron
 */
@XmlTransient
public class User2UserImplAdapter extends XmlAdapter<UserImpl, User> {

	private static final Map<User, User> replacementMap = Collections
			.synchronizedMap(new IdentityHashMap<User, User>());

	public static void registerReplacement(User original, User replacement) {
		if ((original == null) || (replacement == null) || (original == replacement)) {
			return;
		}
		replacementMap.put(original, replacement);
	}

	@SuppressWarnings("unchecked")
	public static <T extends User> T resolveIdentity(T candidate) {
		if (candidate == null) {
			return null;
		}
		User replacement = replacementMap.get(candidate);
		return (replacement != null) ? (T) replacement : candidate;
	}

	public static com.rreganjr.requel.user.User resolveDomain(User candidate) {
		User resolved = resolveIdentity(candidate);
		if (resolved instanceof com.rreganjr.requel.user.User) {
			return (com.rreganjr.requel.user.User) resolved;
		}
		return null;
	}

	public static void clearReplacements() {
		replacementMap.clear();
	}

	@Override
	public UserImpl marshal(User user) throws Exception {
		if (user == null) {
			return null;
		}
		if (EntityProxyInterceptor.isEntityProxy(user)) {
			user = EntityProxyInterceptor.unwrap(user);
		}
		User resolved = resolveIdentity(user);
		if (resolved instanceof UserImpl) {
			return (UserImpl) resolved;
		}
		return (UserImpl) user;
	}

	@Override
	public User unmarshal(UserImpl user) throws Exception {
		return resolveIdentity(user);
	}

}
