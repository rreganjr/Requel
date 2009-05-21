/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.repository;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import edu.harvard.fas.rregan.SystemInitializer;

/**
 * @author ron
 */
@Component("databaseInitializer")
@Scope("prototype")
public class DatabaseInitializer {

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
			initializer.initialize();
		}
	}
}
