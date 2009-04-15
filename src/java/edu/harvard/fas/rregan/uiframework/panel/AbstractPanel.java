/*
 * $Id: AbstractPanel.java,v 1.15 2008/09/13 00:33:43 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import nextapp.echo2.app.event.ActionListener;
import echopointng.ContentPaneEx;
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * This extends ContentPaneEx so that multiple children can be added to the
 * panel easily. Otherwise a seperate column would need to be added and that
 * would complicate extending this panel to add more children.
 * 
 * @author ron
 */
public abstract class AbstractPanel extends ContentPaneEx implements Panel {
	static final long serialVersionUID = 0L;

	private final PanelActionType supportedActionType;
	private final Class<?> supportedContentType;
	private final String panelName;
	private boolean initialized = false;

	private Object destinationObject;
	private Object targetObject;
	private final ResourceBundleHelper resourceBundleHelper;
	private final Set<ActionListener> actionListeners = new HashSet<ActionListener>();
	private boolean loadingState;

	protected AbstractPanel(String resourceBundleName, PanelActionType supportedActionType,
			Class<?> supportedContentType) {
		this(resourceBundleName, supportedActionType, supportedContentType, null);
	}

	protected AbstractPanel(String resourceBundleName, String panelName,
			Class<?> supportedContentType) {
		this(resourceBundleName, PanelActionType.Unspecified, supportedContentType, panelName);
	}

	protected AbstractPanel(String resourceBundleName, PanelActionType supportedActionType,
			Class<?> supportedContentType, String panelName) {
		super();
		resourceBundleHelper = new ResourceBundleHelper(resourceBundleName);
		this.supportedActionType = supportedActionType;
		this.supportedContentType = supportedContentType;
		this.panelName = panelName;
	}

	protected ResourceBundleHelper getResourceBundleHelper(Locale locale) {
		resourceBundleHelper.setLocale(locale);
		return resourceBundleHelper;
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
		return this.getClass();
	}

	public String getTitle() {
		return getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "");
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setup() {
		initialized = true;
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		initialized = false;
	}

	/**
	 * @return true if the panel is in the loadState() method, false otherwise.
	 */
	public boolean isLoadingState() {
		return loadingState;
	}

	public Object getTargetObject() {
		return targetObject;
	}

	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	public Object getDestinationObject() {
		return destinationObject;
	}

	public void setDestinationObject(Object destinationObject) {
		this.destinationObject = destinationObject;
	}

	/**
	 * @param actionListener
	 */
	public void addActionListener(ActionListener actionListener) {
		actionListeners.add(actionListener);
	}

	/**
	 * @param actionListener
	 */
	public void removeActionListener(ActionListener actionListener) {
		actionListeners.remove(actionListener);
	}

	/**
	 * @return
	 */
	public EventDispatcher getEventDispatcher() {
		return getApp().getEventDispatcher();
	}

	/**
	 * @return
	 */
	public UIFrameworkApp getApp() {
		return UIFrameworkApp.getApp();
	}
}
