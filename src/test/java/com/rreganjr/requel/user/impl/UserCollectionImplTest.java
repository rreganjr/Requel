package com.rreganjr.requel.user.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.rreganjr.TestCase;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserSet;

/**
 * @author ron
 */
public class UserCollectionImplTest extends TestCase {

	/**
	 * 
	 */
	public void testConstruction() {
		Collection<User> init = new ArrayList<User>(5);

		init.add(new TestUserImpl(1L, "aaaaa"));
		init.add(new TestUserImpl(2L, "bbbbb"));
		init.add(new TestUserImpl(3L, "ccccc"));
		init.add(new TestUserImpl(4L, "ddddd"));
		init.add(new TestUserImpl(5L, "eeeee"));
		UserSet users = new UserSetImpl(init);
		assertEquals(init, users);
		assertTrue(users.contains(new TestUserImpl(1L, "aaaaa")));
		assertTrue(users.contains(new TestUserImpl(2L, "bbbbb")));
		assertTrue(users.contains(new TestUserImpl(3L, "ccccc")));
		assertTrue(users.contains(new TestUserImpl(4L, "ddddd")));
		assertTrue(users.contains(new TestUserImpl(5L, "eeeee")));
	}

	/**
	 * 
	 */
	public void testInvalidConstruction() {
		try {
			Collection<User> init = new ArrayList<User>(5);

			init.add(new TestUserImpl(1L, "aaaaa"));
			init.add(new TestUserImpl(2L, "bbbbb"));
			init.add(new TestUserImpl(3L, "ccccc"));
			init.add(new TestUserImpl(4L, "ddddd"));
			init.add(new TestUserImpl(5L, "eeeee"));
			init.add(new TestUserImpl(6L, "aaaaa"));
			UserSet users = new UserSetImpl(init);
			fail("expected an exception because a user with the same user name but different id was detected.");
		} catch (Exception e) {

		}
	}

	/**
	 * 
	 */
	public void testEditedUsers() {
		Collection<User> init = new ArrayList<User>(5);

		init.add(new TestUserImpl(1L, "aaaaa"));
		init.add(new TestUserImpl(2L, "bbbbb"));
		init.add(new TestUserImpl(3L, "ccccc"));
		init.add(new TestUserImpl(4L, "ddddd"));
		init.add(new TestUserImpl(5L, "eeeee"));
		UserSet users = new UserSetImpl(init);
		assertEquals(init, users);

		// simulate detecting user with changed username
		assertTrue(users.contains(new TestUserImpl(1L, "xxxxx")));

		// this should replace the old entry with username "aaaaa"
		users.add(new TestUserImpl(1L, "xxxxx"));
		assertFalse(users.contains(new TestUserImpl(99L, "aaaaa")));
	}

	/**
	 * the iterator should return users in order by username.
	 */
	public void testIterator() {
		Collection<User> init = new ArrayList<User>(5);

		init.add(new TestUserImpl(4L, "ddddd"));
		init.add(new TestUserImpl(2L, "bbbbb"));
		init.add(new TestUserImpl(5L, "eeeee"));
		init.add(new TestUserImpl(3L, "ccccc"));
		init.add(new TestUserImpl(1L, "aaaaa"));
		UserSet users = new UserSetImpl(init);
		Iterator<User> iter = users.iterator();
		assertEquals(new TestUserImpl(1L, "aaaaa"), iter.next());
		assertEquals(new TestUserImpl(2L, "bbbbb"), iter.next());
		assertEquals(new TestUserImpl(3L, "ccccc"), iter.next());
		assertEquals(new TestUserImpl(4L, "ddddd"), iter.next());
		assertEquals(new TestUserImpl(5L, "eeeee"), iter.next());
	}

	private static class TestUserImpl extends UserImpl {
		protected TestUserImpl(Long id, String username) {
			super();
			setUsername(username);
			setId(id);
		}

		@Override
		public String toString() {
			return "User[" + getId() + "]: " + getUsername();
		}
	}
}
