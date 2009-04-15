/*
 * $Id: MessageHandler.java,v 1.1 2008/10/30 05:55:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework;

/**
 * @author ron
 */
public interface MessageHandler {

	/**
	 * Set a message to report to the user.
	 * 
	 * @param message -
	 *            the message to report.
	 */
	public void setGeneralMessage(String message);
}
