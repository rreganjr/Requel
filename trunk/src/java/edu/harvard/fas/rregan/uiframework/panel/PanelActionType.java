/*
 * $Id: PanelActionType.java,v 1.1 2008/02/27 11:34:39 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel;

/**
 * @author ron
 */
public enum PanelActionType {

	/**
	 * Indicates the panel is an editor panel for editing some type of content.
	 */
	Editor(),
	/**
	 * Indicates the panel is a selector panel for choosing some type of
	 * content.
	 */
	Selector(),
	/**
	 * Indicates the panel is a navigator panel for listing and navigating over
	 * some type of content.
	 */
	Navigator(),
	/**
	 * Indicates the panel panel has an unspecified function and is only
	 * accessed by name.
	 */
	Unspecified();

	private PanelActionType() {
	}
}
