/*
 * $Id: TabbedPanelContainer.java,v 1.15 2008/03/10 23:57:20 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel;

import java.util.Stack;

import nextapp.echo2.app.Component;
import nextapp.echo2.extras.app.TabPane;
import nextapp.echo2.extras.app.layout.TabPaneLayoutData;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;

/**
 * A PanelContainer that displays panels in multiple tabs that can be navigated
 * independently.
 * 
 * @author ron
 */
public class TabbedPanelContainer extends AbstractPanelContainerPanel {
	static final long serialVersionUID = 0L;

	private TabPane tabs;
	private final Stack<Integer> tabDisplayOrder = new Stack<Integer>();

	/**
	 * Create a TabbedPanelContainer with the default resource bundle name
	 * (using the full class name of this class) and a DefaultPanelManager.
	 * 
	 * @param panelManager -
	 *            the panel manager with the set of panels available for display
	 *            already attached.
	 */
	@Autowired
	public TabbedPanelContainer(PanelManager panelManager) {
		this(TabbedPanelContainer.class.getName(), panelManager);
	}

	/**
	 * Create a TabbedPanelContainer with the specified resource bundle name and
	 * PanelManager.
	 * 
	 * @param resourceBundleName
	 * @param panelManager
	 */
	public TabbedPanelContainer(String resourceBundleName, PanelManager panelManager) {
		super(resourceBundleName, panelManager);
	}

	@Override
	public void setup() {
		super.setup();
		tabs = new TabPane();
		tabs.setStyleName(STYLE_NAME_DEFAULT);
		add(tabs);
	}

	@Override
	public void dispose() {
		tabs.removeAll();
		tabs.dispose();
		getPanelManager().dispose();
		super.dispose();
		tabs = null;
	}

	@Override
	public void setStyleName(String newValue) {
		super.setStyleName(newValue);
		tabs.setStyleName(newValue);
	}

	public void displayPanel(Panel panel, WorkflowDisposition disposition) {
		// TODO: stack panels in the same flow on top of the panel
		// in a window pane

		// add a new tab if the panel isn't displayed yet, otherwise
		// reset the selection to the existing tab
		int tabIndex = tabs.getActiveTabIndex();
		tabIndex = getTabIndexForPanel(panel);
		if (tabIndex == -1) {
			TabPaneLayoutData layoutData = new TabPaneLayoutData();
			layoutData.setTitle(panel.getTitle());
			panel.setLayoutData(layoutData);

			tabs.add((Component) panel);
			tabs.setActiveTabIndex(tabs.getComponentCount() - 1);
			tabDisplayOrder.push(new Integer(tabs.getComponentCount() - 1));
		} else {
			tabs.setActiveTabIndex(tabIndex);
			// move the index from where ever it is in the stack to the top
			tabDisplayOrder.remove(new Integer(tabIndex));
			tabDisplayOrder.push(new Integer(tabIndex));
		}
	}

	private int getTabIndexForPanel(Panel panel) {
		int index = -1;
		for (int i = 0; i < tabs.getComponentCount(); i++) {
			if (panel.equals(tabs.getComponent(i))) {
				index = i;
				break;
			}
		}
		return index;
	}

	public void undisplayPanel(Panel panel) {
		int tabIndex = getTabIndexForPanel(panel);
		if (tabIndex > -1) {
			tabs.remove(tabIndex);
			tabDisplayOrder.remove(new Integer(tabIndex));
			if (!tabDisplayOrder.empty()) {
				tabs.setActiveTabIndex(tabDisplayOrder.peek());
			}
		}
	}
}
