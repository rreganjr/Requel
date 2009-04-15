/*
 * $Id: EntityExceptionActionType.java,v 1.1 2008/12/13 00:40:41 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository;

/**
 * @author ron
 */
public enum EntityExceptionActionType {

	Creating(), Reading(), Updating(), Deleting(), Unknown();

	private EntityExceptionActionType() {

	}
}
