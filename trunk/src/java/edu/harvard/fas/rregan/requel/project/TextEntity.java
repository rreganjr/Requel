/*
 * $Id: TextEntity.java,v 1.3 2008/03/19 09:57:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * An entity whose primary content is text, such as a goal or story.
 * 
 * @author ron
 */
public interface TextEntity extends ProjectOrDomainEntity {

	/**
	 * @return get the text of this entity.
	 */
	public String getText();

	/**
	 * change the text of this entity.
	 * 
	 * @param text -
	 *            new text
	 */
	public void setText(String text);
}
