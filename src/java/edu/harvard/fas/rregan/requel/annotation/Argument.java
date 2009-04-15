/*
 * $Id: Argument.java,v 1.5 2008/04/14 08:57:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation;

import edu.harvard.fas.rregan.requel.CreatedEntity;

/**
 * An argument for or against a Position of an Annotation.
 * 
 * @author ron
 */
public interface Argument extends Comparable<Argument>, CreatedEntity {

	/**
	 * The position that the argument is for or against.
	 * 
	 * @return
	 */
	public Position getPosition();

	/**
	 * @return the text of the argument describing why a position should be or
	 *         should not be chosen as the resolution to an issue.
	 */
	public String getText();

	/**
	 * @return the level at which this argument is for or against the position.
	 */
	public ArgumentPositionSupportLevel getSupportLevel();
}
