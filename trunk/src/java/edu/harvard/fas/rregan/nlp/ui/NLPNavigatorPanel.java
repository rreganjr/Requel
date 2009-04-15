/*
 * $Id: NLPNavigatorPanel.java,v 1.3 2008/12/19 09:20:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.ui;

import java.util.Set;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.navigation.tree.NavigatorTreeNodeFactory;
import edu.harvard.fas.rregan.uiframework.panel.NavigatorTreePanel;

/**
 * @author ron
 */
public class NLPNavigatorPanel extends NavigatorTreePanel {
	private static final Logger log = Logger.getLogger(NLPNavigatorPanel.class);
	static final long serialVersionUID = 0;

	/**
	 * @param treeNodeFactories
	 */
	public NLPNavigatorPanel(Set<NavigatorTreeNodeFactory> treeNodeFactories) {
		super(NLPNavigatorPanel.class.getName(), treeNodeFactories,
				NLPPanelNames.NLP_NAVIGATOR_PANEL_NAME);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
	}

	@Override
	public void setTargetObject(Object targetObject) {
		// this panel doesn't support a target class
	}

	@Override
	public Object getTargetObject() {
		return this.getClass();
	}
}
