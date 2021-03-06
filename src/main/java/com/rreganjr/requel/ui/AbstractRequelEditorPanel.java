/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.ui;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.user.User;
import net.sf.echopm.panel.editor.AbstractEditorPanel;

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
