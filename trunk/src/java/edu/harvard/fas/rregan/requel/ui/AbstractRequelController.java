/*
 * $Id: AbstractRequelController.java,v 1.4 2009/01/27 09:30:19 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.controller.AbstractController;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public abstract class AbstractRequelController extends AbstractController {
	static final long serialVersionUID = 0;
	protected static final Logger log = Logger.getLogger(AbstractRequelController.class);

	protected AbstractRequelController() {
		super();
	}

	protected AbstractRequelController(EventDispatcher eventDispatcher) {
		super(eventDispatcher);
	}
}
