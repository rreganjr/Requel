/*
 * $Id: TestPanel.java,v 1.4 2008/09/13 00:33:43 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework;

import nextapp.echo2.app.LayoutData;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * @author ron
 */
public class TestPanel implements Panel {

	boolean disposeCalled = false;
	boolean initialized = false;
	PanelActionType supportActionType;
	Object targetObject;
	Object destinationObject;
	Class<?> supportedContentType;
	String title;
	String panelName;

	public TestPanel(String panelName, PanelActionType supportActionType, Class<?> supportedContentType) {
		setPanelName(panelName);
		setSupportedActionType(supportActionType);
		
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#dispose()
	 */
	public void dispose() {
		disposeCalled = true;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#getTargetObject()
	 */
	public Object getTargetObject() {
		return targetObject;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#getTitle()
	 */
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#isInitialized()
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#setLayoutData(nextapp.echo2.app.LayoutData)
	 */
	public void setLayoutData(LayoutData layoutData) {
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#setStyleName(java.lang.String)
	 */
	public void setStyleName(String styleName) {
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#setTargetObject(java.lang.Object)
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.Panel#setup()
	 */
	public void setup() {
		initialized = true;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor#getPanelName()
	 */
	public String getPanelName() {
		return null;
	}
	
	public void setPanelName(String panelName) {
		this.panelName = panelName;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor#getPanelType()
	 */
	public Class<? extends Panel> getPanelType() {
		return this.getClass();
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor#getSupportedActionType()
	 */
	public PanelActionType getSupportedActionType() {
		return supportActionType;
	}
	
	public void setSupportedActionType(PanelActionType supportActionType) {
		this.supportActionType = supportActionType;
	}

	/**
	 * @see edu.harvard.fas.rregan.uiframework.panel.PanelDescriptor#getSupportedContentType()
	 */
	public Class<?> getSupportedContentType() {
		return supportedContentType;
	}

	public void setSupportedContentType(Class<?> supportedContentType) {
		this.supportedContentType = supportedContentType;
	}

	public Object getDestinationObject() {
		return destinationObject;
	}

	public void setDestinationObject(Object destinationObject) {
		this.destinationObject = destinationObject;
	}
}
