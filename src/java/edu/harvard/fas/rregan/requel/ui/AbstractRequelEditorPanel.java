/*
 * $Id: AbstractRequelEditorPanel.java,v 1.3 2008/12/13 00:41:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.uiframework.panel.editor.AbstractEditorPanel;

/**
 * Base class for Requel specific editors.
 * 
 * @author ron
 */
public class AbstractRequelEditorPanel extends AbstractEditorPanel {
	static final long serialVersionUID = 0L;

	private final CommandHandler commandHandler;

	protected AbstractRequelEditorPanel(String resourceBundleName, Class<?> supportedContentType,
			String panelName, CommandHandler commandHandler) {
		super(resourceBundleName, supportedContentType, panelName);
		this.commandHandler = commandHandler;
	}

	protected AbstractRequelEditorPanel(String resourceBundleName, Class<?> supportedContentType,
			CommandHandler commandHandler) {
		this(resourceBundleName, supportedContentType, null, commandHandler);
	}

	protected User getCurrentUser() {
		return (User) getApp().getUser();
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}
}
