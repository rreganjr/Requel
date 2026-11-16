/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.user.impl.repository.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.platform.bootstrap.AbstractSystemInitializer;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Issue #381: require the built-in admin to change its password while it is still the default.
 *
 * <p>Runs on every startup, after {@link AdminUserInitializer} (order 100) has created the admin
 * on a fresh install. If {@code admin}'s password is still {@code admin}, the account is flagged
 * "must change password", so its first sign-in can do nothing until it is changed. That one check
 * covers a fresh install, an upgrade from a release without the flag, and an admin set back to
 * the default later. An install whose admin password was already changed is left alone, and
 * nothing is stored to record that the check ran.</p>
 *
 * <p>{@code requel.admin.force-password-change=false} turns it off; the test and dev profiles do,
 * so integration tests and local restarts can keep signing in as {@code admin}/{@code admin}.</p>
 */
@Component("defaultAdminPasswordInitializer")
@Scope("prototype")
public class DefaultAdminPasswordInitializer extends AbstractSystemInitializer {

	static final String ADMIN_USERNAME = "admin";
	static final String DEFAULT_ADMIN_PASSWORD = "admin";

	private final UserRepository userRepository;
	private final boolean enabled;
	private final String username;

	@Autowired
	public DefaultAdminPasswordInitializer(UserRepository userRepository,
			@Value("${requel.admin.force-password-change:true}") boolean enabled) {
		this(userRepository, enabled, ADMIN_USERNAME);
	}

	/**
	 * For tests: check another account, so the shared admin of an integration-test context stays
	 * unflagged.
	 */
	public DefaultAdminPasswordInitializer(UserRepository userRepository, boolean enabled, String username) {
		super(110);
		this.userRepository = userRepository;
		this.enabled = enabled;
		this.username = username;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		if (!enabled) {
			return;
		}
		User admin;
		try {
			admin = userRepository.findUserByUsername(username);
		} catch (NoSuchUserException e) {
			return;
		}
		if (!admin.isMustChangePassword() && admin.isPassword(DEFAULT_ADMIN_PASSWORD)) {
			log.warn("The admin account still has the default password; it must be changed at the next sign-in.");
			admin.setMustChangePassword(true);
			userRepository.merge(admin);
		}
	}
}
