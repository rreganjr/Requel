/*
 * $Id: TestPanelDescriptor.java,v 1.1 2008/03/09 08:29:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework;

import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor;

/**
 * A descriptor for testing
 * 
 * @author ron
 */
public class TestPanelDescriptor implements PanelDescriptor {

	private final PanelActionType supportedActionType;
	private final Class<?> supportedContentType;
	private final String panelName;
	private final Class<? extends Panel> panelType;

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param panelName
	 */
	public TestPanelDescriptor(Class<? extends Panel> panelType, Class<?> supportedContentType,
			String panelName) {
		this(panelType, supportedContentType, PanelActionType.Unspecified, panelName);
	}

	/**
	 * @param panelType
	 * @param supportedContentType
	 * @param supportedActionType
	 */
	public TestPanelDescriptor(Class<? extends Panel> panelType, Class<?> supportedContentType,
			PanelActionType supportedActionType) {
		this(panelType, supportedContentType, supportedActionType, null);
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
	public TestPanelDescriptor(Class<? extends Panel> panelType, Class<?> supportedContentType,
			PanelActionType supportedActionType, String panelName) {
		this.supportedActionType = supportedActionType;
		this.supportedContentType = supportedContentType;
		this.panelName = panelName;
		this.panelType = panelType;
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
	}
}