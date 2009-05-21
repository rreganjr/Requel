/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.uiframework.panel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A factory for creating instances of a specific type of panel.
 * 
 * @author ron
 */
public class DefaultPanelFactory implements PanelFactory {

	private final PanelActionType supportedActionType;
	private final Class<?> supportedContentType;
	private final String panelName;
	private final Class<? extends Panel> panelType;
	private final List<Object> panelConstructorArgs;

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param panelName
	 */
	public DefaultPanelFactory(Class<? extends Panel> panelType, Class<?> supportedContentType,
			String panelName) {
		this(panelType, supportedContentType, PanelActionType.Unspecified, panelName, null);
	}

	/**
	 * For a panel type that doesn't take an object for editing.
	 * 
	 * @param panelType
	 * @param supportedActionType
	 * @param panelName
	 * @param panelConstructorArgs
	 */
	public DefaultPanelFactory(Class<? extends Panel> panelType,
			PanelActionType supportedActionType, String panelName, List<Object> panelConstructorArgs) {
		this(panelType, null, supportedActionType, panelName, panelConstructorArgs);
	}

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param supportedActionType
	 */
	public DefaultPanelFactory(Class<? extends Panel> panelType, Class<?> supportedContentType,
			PanelActionType supportedActionType) {
		this(panelType, supportedContentType, supportedActionType, null, null);
	}

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param supportedActionType
	 * @param panelConstructorArgs -
	 *            the arguments to desired constructor of the target class. The
	 *            arguments must be objects that are shareable by all instances
	 *            of the panels that get created, for example singleton type
	 *            objects like entity managers. the arguments must be in the
	 *            order specified by the constructor of the panel.
	 */
	public DefaultPanelFactory(Class<? extends Panel> panelType, Class<?> supportedContentType,
			PanelActionType supportedActionType, List<Object> panelConstructorArgs) {
		this(panelType, supportedContentType, supportedActionType, null, panelConstructorArgs);
	}

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param supportedActionType
	 * @param panelName
	 * @param panelConstructorArgs -
	 *            the arguments to desired constructor of the target class. The
	 *            arguments must be objects that are shareable by all instances
	 *            of the panels that get created, for example singleton type
	 *            objects like entity managers. the arguments must be in the
	 *            order specified by the constructor of the panel.
	 */
	public DefaultPanelFactory(Class<? extends Panel> panelType, Class<?> supportedContentType,
			PanelActionType supportedActionType, String panelName, List<Object> panelConstructorArgs) {
		this.supportedActionType = supportedActionType;
		this.supportedContentType = supportedContentType;
		this.panelName = panelName;
		this.panelType = panelType;
		this.panelConstructorArgs = panelConstructorArgs;
	}

	public PanelActionType getSupportedActionType() {
		return supportedActionType;
	}

	public Class<?> getSupportedContentType() {
		return supportedContentType;
	}

	public String getPanelName() {
		return panelName;
	}

	public Class<? extends Panel> getPanelType() {
		return panelType;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelFactory#dispose()
	 */
	public void dispose() {
		// TODO: cleanup any resources. the default PanelFactory doesn't
		// pool panels so there is nothing to do now.
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelFactory#newInstance()
	 */
	public Panel newInstance() throws NoSuchMethodException, InvocationTargetException,
			InstantiationException, IllegalAccessException {
		if (panelConstructorArgs == null) {
			return getPanelType().newInstance();
		} else {
			return newInstance(panelConstructorArgs);
		}
	}

	/**
	 * @param args
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected Panel newInstance(List<Object> args) throws NoSuchMethodException,
			InvocationTargetException, InstantiationException, IllegalAccessException {
		// TODO: if the panel doesn't have a constructor that matches the
		// supplied parameters, but does
		// have a constructor that matches the supplied parameters plus one or
		// more of the supportedActionType,
		// supportedContentType, or panelName then this method should be able to
		// supply those values directly.

		Constructor<? extends Panel> constructor = getConstructor(getPanelType(), args);
		return constructor.newInstance(args.toArray());
	}

	private Constructor<? extends Panel> getConstructor(Class<? extends Panel> panelType,
			List<Object> args) throws NoSuchMethodException {
		Class<?> parameterTypes[] = new Class<?>[args.size()];
		for (int i = 0; i < args.size(); i++) {
			parameterTypes[i] = args.get(i).getClass();
		}
		for (Constructor<?> constructor : panelType.getConstructors()) {
			if (constructor.getParameterTypes().length == args.size()) {
				boolean match = true;
				for (Class<?> paramType : constructor.getParameterTypes()) {
					if (paramType.isAssignableFrom(args.getClass())) {
						match = false;
						break;
					}
				}
				if (match) {
					return (Constructor) constructor;
				}
			}
		}
		StringBuilder errMsg = new StringBuilder();
		errMsg.append(panelType.getName());
		errMsg.append(".<init>(");
		for (Object arg : args) {
			errMsg.append(arg.getClass().getName());
		}
		errMsg.append(")");
		throw new NoSuchMethodException(errMsg.toString());
	}

	@Override
	public String toString() {
		return "supportedActionType = " + getSupportedActionType() + " supportedContentType = "
				+ getSupportedContentType() + " panelName = " + getPanelName() + " panelType = "
				+ getPanelType();
	}
}
