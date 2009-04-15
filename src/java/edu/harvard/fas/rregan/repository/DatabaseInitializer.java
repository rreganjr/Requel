/*
 * $Id: DatabaseInitializer.java,v 1.2 2009/01/26 10:19:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
