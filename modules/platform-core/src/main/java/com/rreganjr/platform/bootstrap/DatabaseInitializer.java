/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.platform.bootstrap;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author ron
 */
@Component("databaseInitializer")
@Scope("prototype")
public class DatabaseInitializer {

	private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

	private final Set<SystemInitializer> entityInitializers;

	/**
	 * Create a database initializer with a set of entity initalizers that it
	 * will call.
	 * 
	 * @param entityInitializers
	 */
	@Autowired
	public DatabaseInitializer(Set<SystemInitializer> entityInitializers) {
		this.entityInitializers = new TreeSet<SystemInitializer>(entityInitializers);
	}

	/**
	 * initialize all the entity initializers.
	 */
	public void initialize() {
		for (SystemInitializer initializer : entityInitializers) {
			try {
				initializer.initialize();
			} catch (RuntimeException e) {
				// Don't let one failing initializer abort the rest of the chain (e.g. user
				// seeding ordered after it). Log and continue so the system still bootstraps.
				log.error("System initializer {} failed; continuing with remaining initializers",
						initializer.getClass().getSimpleName(), e);
			}
		}
	}
}
