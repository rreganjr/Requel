/*
 * $Id: PanelFactory.java,v 1.3 2008/03/06 02:41:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel;

import java.lang.reflect.InvocationTargetException;

/**
 * @author ron
 */
public interface PanelFactory extends PanelDescriptor {

	/**
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public Panel newInstance() throws NoSuchMethodException, InvocationTargetException,
			InstantiationException, IllegalAccessException;

}