/*
 * $Id: PanelDescriptor.java,v 1.2 2008/02/29 19:36:11 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel;

/**
 * Describes the action, content type, and/or name of a panel.
 * 
 * @author ron
 */
public interface PanelDescriptor {

	/**
	 * @return the type of content the panel supports.
	 */
	public Class<?> getSupportedContentType();

	/**
	 * @return the type of action the panel supports.
	 */
	public PanelActionType getSupportedActionType();

	/**
	 * @return for a named panel return the name, otherwise return null.
	 */
	public String getPanelName();

	/**
	 * @return the class of the panel being described.
	 */
	public Class<? extends Panel> getPanelType();

	/**
	 * tell the descriptor to cleanup its resources and ui components.
	 */
	public void dispose();
}
