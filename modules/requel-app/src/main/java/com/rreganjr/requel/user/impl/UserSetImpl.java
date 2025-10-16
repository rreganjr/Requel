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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserSet;

/**
 * A set of users that recognizes a user even if the username changes. TODO:
 * this may not be needed because of considering the id in equals, compareTo and
 * hashCode methods.
 * 
 * @author ron
 */
public class UserSetImpl implements UserSet {

	Set<User> usersByName = new TreeSet<User>(User.UserComparator);
	Map<Long, User> usersById = new HashMap<Long, User>();

	/**
	 * create a new empty UserSet
	 */
	public UserSetImpl() {
	}

	/**
	 * Create a new UserSet with the given collection of users.
	 * 
	 * @param users
	 */
	public UserSetImpl(Collection<?> users) {
		for (Object o : users) {
			UserImpl user = (UserImpl) o;
			User originalUser = usersById.get(user.getId());
			if ((originalUser == null) && usersByName.contains(user)) {
				throw new IllegalArgumentException("user " + user
						+ " conflicts with an existing user " + originalUser);
			}
			usersById.put(user.getId(), user);
			usersByName.add(user);
		}
	}

	/**
	 * @see java.util.Collection#add(java.lang.Object)
	 * @return true if the user was not in the collection or the user is not
	 *         equal to the copy that was in the collection.
	 */
	public boolean add(User u) {
		UserImpl user = (UserImpl) u;
		User originalUser = usersById.get(user.getId());
		if ((originalUser == null) && usersByName.contains(user)) {
			throw new IllegalArgumentException("user " + user + " conflicts with an existing user "
					+ originalUser);
		}
		usersById.put(user.getId(), user);
		usersByName.add(user);
		return !user.equals(originalUser);
	}

	/**
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends User> c) {
		boolean rval = false;
		for (User u : c) {
			rval = (rval || add(u));
		}
		return rval;
	}

	/**
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		usersByName.clear();
		usersById.clear();
	}

	/**
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		if (o instanceof User) {
			UserImpl user = (UserImpl) o;
			return usersById.containsKey(user.getId());
		}
		return false;
	}

	/**
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return usersById.isEmpty();
	}

	/**
	 * @see java.util.Collection#iterator()
	 */
	public Iterator<User> iterator() {
		return usersByName.iterator();
	}

	/**
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		if (o instanceof User) {
			UserImpl u = (UserImpl) o;
			User originalUser = usersById.remove(u.getId());
			if (originalUser != null) {
				usersByName.remove(originalUser);
			}
			return (originalUser != null);
		}
		return false;
	}

	/**
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		boolean rval = false;
		for (Object o : c) {
			rval = (rval || remove(o));
		}
		return rval;
	}

	/**
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		boolean rval = usersByName.retainAll(c);
		usersById.clear();
		for (User u : usersByName) {
			usersById.put(((UserImpl) u).getId(), u);
		}
		return rval;
	}

	/**
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return usersById.size();
	}

	/**
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		return usersByName.toArray();
	}

	/**
	 * @see java.util.Collection#toArray(T[])
	 */
	public <T> T[] toArray(T[] a) {
		return usersByName.toArray(a);
	}
}
