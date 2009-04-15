/*
 * $Id: TestActionListener.java,v 1.4 2008/03/10 23:57:21 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;

/**
 * @author ron
 */
public class TestActionListener implements ActionListener {
	private static final Logger log = Logger.getLogger(TestActionListener.class);
	static final long serialVersionUID = 0;

	private ActionEvent eventReceived;
	private final String name;

	public TestActionListener(String name) {
		this.name = name;
	}

	public void actionPerformed(ActionEvent e) {
		eventReceived = e;
	}

	public ActionEvent getEventReceived() {
		return eventReceived;
	}

	public void clearEventReceived() {
		eventReceived = null;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if ((o != null) && (o instanceof TestActionListener)) {
			TestActionListener other = (TestActionListener) o;
			log.debug("this.name = '" + name + "', other.name = '" + other.name + "' equals = "
					+ name.equals(other.name));
			return name.equals(other.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
