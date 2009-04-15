/*
 * $Id: DeletedEntityEvent.java,v 1.1 2009/02/15 09:31:37 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.navigation.event;

import edu.harvard.fas.rregan.uiframework.panel.Panel;

/**
 * @author ron
 */
public class DeletedEntityEvent extends UpdateEntityEvent {
	static final long serialVersionUID = 0;

	/**
	 * @param source
	 * @param panelToClose
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Object source, Panel panelToClose, Object updatedObject) {
		super(source, panelToClose, updatedObject);
	}

	/**
	 * @param source
	 * @param command
	 * @param panelToClose
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Object source, String command, Panel panelToClose,
			Object updatedObject) {
		super(source, command, panelToClose, updatedObject);
	}

	/**
	 * @param source
	 * @param updatedObject
	 */
	public DeletedEntityEvent(Panel source, Object updatedObject) {
		super(source, updatedObject);
	}
}
