/*
 * $Id: StoryType.java,v 1.2 2008/09/26 23:08:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * The type/disposition of a story, such as a successful or exceptional case.
 * 
 * @author ron
 */
public enum StoryType {

	/**
	 * A story with a successful outcome.
	 */
	Success,

	/**
	 * A story describing a problem case.
	 */
	Exception;

	private StoryType() {
	}
}
