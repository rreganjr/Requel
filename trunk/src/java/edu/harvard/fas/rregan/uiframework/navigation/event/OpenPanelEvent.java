/*
 * $Id: OpenPanelEvent.java,v 1.5 2008/03/01 02:30:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.navigation.event;

import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * An event to be handled by a PanelContainer to open an appropriate Panel.
 * 
 * @author ron
 */
public class OpenPanelEvent extends NavigationEvent {
	static final long serialVersionUID = 0;

	private final Panel panel;
	private final PanelActionType panelActionType;
	private final Object targetObject;
	private final Class<?> targetType;
	private final String panelName;
	private final WorkflowDisposition disposition;

	/**
	 * An event for a PanelContainer to open a panel appropriate for the
	 * parameters with a WorkflowDisposition of ContinueFlow, meaning the panel
	 * will be opened in a manner appropriate for returning to the previous
	 * window when it is closed, possibly returning some data.
	 * 
	 * @param source -
	 *            the controller fireing the event.
	 * @param panelActionType -
	 *            the action type (edit, select, navigate) of the window to
	 *            open.
	 * @param targetObject -
	 *            an object the panel will act on. it may be a different type
	 *            from the supplied targetType.
	 * @param targetType -
	 *            the type of object(s) the action is appropriate for.
	 * @param panelName -
	 *            an explicit name (or role) of the panel to open.
	 */
	public OpenPanelEvent(Object source, PanelActionType panelActionType, Object targetObject,
			Class<?> targetType, String panelName) {
		this(source, panelActionType, targetObject, targetType, panelName,
				WorkflowDisposition.ContinueFlow);
	}

	/**
	 * An event for a PanelContainer to open a panel appropriate for the
	 * parameters.
	 * 
	 * @param source -
	 *            the controller fireing the event.
	 * @param panelActionType -
	 *            the action type (edit, select, navigate) of the window to
	 *            open.
	 * @param targetObject -
	 *            an object the panel will act on. it may be a different type
	 *            from the supplied targetType.
	 * @param targetType -
	 *            the type of object(s) the action is appropriate for.
	 * @param panelName -
	 *            an explicit name (or role) of the panel to open.
	 * @param disposition -
	 *            a WorkflowDisposition indicating if the panel is supporting
	 *            the work of the previous panel (ContinueFlow) or if the panel
	 *            is starting an independent task (NewFlow).
	 */
	public OpenPanelEvent(Object source, PanelActionType panelActionType, Object targetObject,
			Class<?> targetType, String panelName, WorkflowDisposition disposition) {
		super(source, "OpenPanel:" + panelActionType.name());
		this.panel = null;
		this.panelActionType = (panelActionType==null?PanelActionType.Unspecified:panelActionType);
		this.panelName = panelName;
		this.disposition = disposition;
		this.targetType = targetType;
		this.targetObject = targetObject;
	}

	/**
	 * Tell a panel container to display an existing panel. This event is
	 * primarily for a PanelContainer to send to its PanelManager. For example
	 * when a TabbedPanelContainer detects the user clicking on a tab button.
	 * 
	 * @param source
	 * @param panel
	 */
	public OpenPanelEvent(Object source, Panel panel) {
		super(source, null);
		this.panel = panel;
		this.panelActionType = null;
		this.panelName = null;
		this.disposition = null;
		this.targetType = null;
		this.targetObject = null;
	}

	public PanelActionType getPanelActionType() {
		return panelActionType;
	}

	public Panel getPanel() {
		return panel;
	}

	public String getPanelName() {
		return panelName;
	}

	public Object getTargetObject() {
		return targetObject;
	}

	public Class<?> getTargetType() {
		return targetType;
	}

	public WorkflowDisposition getWorkflowDisposition() {
		return disposition;
	}

	public void configurePanelWithEventData(Panel panelToInit) {
		panelToInit.setTargetObject(getTargetObject());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		sb.append("[source = ");
		sb.append(getSource().getClass().getSimpleName());
		if (panelActionType != null) {
			sb.append(", panelActionType = ");
			sb.append(panelActionType.name());
		}
		if (panel != null) {
			sb.append(", panel = ");
			sb.append(panel);
		}
		if (panelName != null) {
			sb.append(", panelName = ");
			sb.append(panelName);
		}
		if (targetObject != null) {
			sb.append(", targetObject = ");
			sb.append(targetObject);
		}
		if (targetType != null) {
			sb.append(", targetType = ");
			sb.append(targetType);
		}
		if (disposition != null) {
			sb.append(", disposition = ");
			sb.append(disposition.name());
		}
		sb.append("]");
		return sb.toString();
	}
}
