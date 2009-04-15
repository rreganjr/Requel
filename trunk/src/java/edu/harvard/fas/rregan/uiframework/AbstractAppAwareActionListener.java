/*
 * $Id: AbstractAppAwareActionListener.java,v 1.1 2008/02/15 21:41:43 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.event.ActionListener;

/**
 * @author ron
 */
public abstract class AbstractAppAwareActionListener implements ActionListener {
	static final long serialVersionUID = 0;

	private final UIFrameworkApp app;

	protected AbstractAppAwareActionListener(UIFrameworkApp app) {
		this.app = app;
	}

	protected UIFrameworkApp getApp() {
		return app;
	}
}
