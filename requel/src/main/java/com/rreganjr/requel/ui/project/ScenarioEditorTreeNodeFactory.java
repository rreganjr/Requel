/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.ui.project;

import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import echopointng.tree.MutableTreeNode;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.panel.editor.tree.DefaultEditorTreeNode;
import net.sf.echopm.panel.editor.tree.EditorTree;
import net.sf.echopm.panel.editor.tree.EditorTreeNode;
import net.sf.echopm.panel.editor.tree.EditorTreeNodeFactory;

/**
 * A factory for created EditorTree nodes for editing scenarios. It extends
 * ScenarioStepEditorTreeNodeFactory and uses its createTreeNode() to create the
 * actual node for the scenario.
 * 
 * @author ron
 */
public class ScenarioEditorTreeNodeFactory extends ScenarioStepEditorTreeNodeFactory {

	/**
	 * 
	 */
	public ScenarioEditorTreeNodeFactory() {
		super(ScenarioEditorTreeNodeFactory.class.getName(), Scenario.class);
	}

	/**
	 * Create a tree node for editing the scenario's and adding a sub-node for
	 * editing each step in the scenario.
	 * 
	 * @see net.sf.echopm.panel.editor.tree.EditorTreeNodeFactory#createTreeNode(net.sf.echopm.navigation.event.EventDispatcher,
	 *      net.sf.echopm.panel.editor.tree.EditorTree,
	 *      java.lang.Object)
	 */
	@Override
	public MutableTreeNode createTreeNode(EventDispatcher eventDispatcher, EditorTree tree,
			Object object) {
		Scenario scenario = (Scenario) object;
		EditorTreeNode scenarioNode = null;
		if (scenario.equals(tree.getRootObject())) {
			Row wrap = new Row();
			wrap.setInsets(new Insets(10, 5, 10, 0));
			wrap.add(new Label(scenario.getName()));
			scenarioNode = new DefaultEditorTreeNode(eventDispatcher, wrap);
		} else {
			scenarioNode = (EditorTreeNode) super.createTreeNode(eventDispatcher, tree, scenario);
		}
		for (Step step : scenario.getSteps()) {
			EditorTreeNodeFactory factory = tree.getEditorTreeNodeFactory(step);
			scenarioNode.add(factory.createTreeNode(eventDispatcher, tree, step));
		}
		return scenarioNode;
	}
}
