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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;
import echopointng.tree.MutableTreeNode;
import echopointng.tree.TreePath;
import net.sf.echopm.ResourceBundleHelper;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.ConvertStepToScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.ui.AbstractRequelComponent;
import com.rreganjr.requel.ui.AbstractRequelController;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.EventDispatcher;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.SelectEntityEvent;
import net.sf.echopm.panel.Panel;
import net.sf.echopm.panel.PanelActionType;
import net.sf.echopm.panel.editor.EditMode;
import net.sf.echopm.panel.editor.manipulators.AbstractComponentManipulator;
import net.sf.echopm.panel.editor.manipulators.ComponentManipulator;
import net.sf.echopm.panel.editor.manipulators.ComponentManipulators;
import net.sf.echopm.panel.editor.tree.EditorTree;
import net.sf.echopm.panel.editor.tree.EditorTreeNode;
import net.sf.echopm.panel.editor.tree.EditorTreeNodeFactory;

/**
 * A table of the scenarios that use the supplied scenario.
 * 
 * @author ron
 */
public class ScenarioStepsEditor extends AbstractRequelComponent {
	static final long serialVersionUID = 0L;

	static {
		ComponentManipulators.setManipulator(ScenarioStepsEditor.class,
				new ScenarioStepsEditorManipulator());
	}

	/**
	 * The name to use in the properties file of the panel that includes the
	 * ScenarioStepsEditor to define the label of the editor. If the property is
	 * undefined the panel should use a sensible default such as "Steps Editor".
	 */
	public static final String PROP_LABEL_SCENARIO_STEPS = "ScenarioSteps.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the add new step button in the scenario step editor. If the property
	 * is undefined "Add Step" is used.
	 */
	public static final String PROP_ADD_STEP_BUTTON_LABEL = "AddStep.Label";

	/**
	 * The name to use in the containing panels properties file to set the label
	 * of the add scenario button in the scenario step editor. If the property
	 * is undefined "Add Scenario" is used.
	 */
	public static final String PROP_ADD_SCENARIO_BUTTON_LABEL = "AddScenario.Label";

	/**
	 * TreeNodeFactories for creating scenario and step nodes in the steps
	 * editor.<br>
	 * TODO: these should be injected through Spring
	 */
	public static final Set<EditorTreeNodeFactory> scenarioTreeNodeFactories;
	static {
		Set<EditorTreeNodeFactory> treeNodeFactories = new HashSet<EditorTreeNodeFactory>();
		treeNodeFactories.add(new ScenarioEditorTreeNodeFactory());
		treeNodeFactories.add(new ScenarioStepEditorTreeNodeFactory());
		scenarioTreeNodeFactories = Collections.unmodifiableSet(treeNodeFactories);
	}

	private Scenario scenario;
	private final EditorTree editorTree;
	private final Button addStepButton;
	private final NavigatorButton addScenarioButton;
	private final AddScenarioController addScenarioController;
	private final Label messages;

	/**
	 * @param projectOrDomain
	 * @param editMode
	 * @param resourceBundleHelper
	 * @param projectCommandFactory
	 * @param commandHandler
	 */
	public ScenarioStepsEditor(ProjectOrDomain projectOrDomain, EditMode editMode,
			ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		editorTree = new EditorTree(this, getEventDispatcher(), scenarioTreeNodeFactories, true,
				false, !editMode.isReadOnlyMode());
		add(editorTree);
		RowLayoutData buttonLayoutData = new RowLayoutData();
		buttonLayoutData.setAlignment(Alignment.ALIGN_LEFT);
		Row buttonLayout = new Row();
		buttonLayout.setCellSpacing(new Extent(5));
		buttonLayout.setInsets(new Insets(0, 5));
		addStepButton = new Button(getResourceBundleHelper(getLocale()).getString(
				PROP_ADD_STEP_BUTTON_LABEL, "Add Step"));
		addStepButton.setStyleName(Panel.STYLE_NAME_DEFAULT);

		// when the add button gets clicked, add a new step editor node to the
		// bottom of the root of the tree.
		addStepButton.addActionListener(new ActionListener() {
			static final long serialVersionUID = 0L;

			@Override
			public void actionPerformed(ActionEvent e) {
				messages.setText("");
				MutableTreeNode rootNode = editorTree.getRootNode();
				EditorTreeNodeFactory factory = editorTree.getEditorTreeNodeFactory(Step.class);
				MutableTreeNode newNode = factory.createTreeNode(getEventDispatcher(), editorTree,
						null);
				editorTree.getModel().insertNodeInto(newNode, rootNode, rootNode.getChildCount());
				editorTree.expandPath(new TreePath(editorTree.getModel().getPathToRoot(rootNode)));
			}
		});
		buttonLayout.add(addStepButton);

		String buttonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_ADD_SCENARIO_BUTTON_LABEL, "Add Scenario");

		addScenarioButton = new NavigatorButton(buttonLabel, getEventDispatcher());
		addScenarioButton.setStyleName(Panel.STYLE_NAME_DEFAULT);
		addScenarioButton.setVisible(false);

		NavigationEvent openScenarioSelectorEvent = new OpenPanelEvent(this,
				PanelActionType.Selector, projectOrDomain, Project.class,
				ProjectManagementPanelNames.PROJECT_SCENARIO_SELECTOR_PANEL_NAME,
				WorkflowDisposition.ContinueFlow);

		addScenarioButton.setEventToFire(openScenarioSelectorEvent);
		addScenarioButton.setVisible(true);
		addScenarioController = new AddScenarioController(getEventDispatcher(), this, editorTree);
		getEventDispatcher().addEventTypeActionListener(SelectEntityEvent.class,
				addScenarioController, this);
		buttonLayout.add(addScenarioButton);
		add(buttonLayout);
		messages = new Label();
		messages.setStyleName(Panel.STYLE_NAME_VALIDATION_LABEL);
		add(messages);
	}

	protected Scenario getScenario() {
		return scenario;
	}

	protected void setScenario(Scenario scenario) {
		this.scenario = scenario;
		editorTree.setRootObject(scenario);
		addScenarioController.setRootScenario(scenario);
	}

	/**
	 * iterate over the scenario steps from the given node generating commands
	 * for editing the step or scenario.
	 * 
	 * @param projectCommandFactory
	 * @param projectOrDomain
	 * @param stepEditorModel
	 * @param rootNode
	 * @return
	 */
	public List<EditScenarioStepCommand> generateStepEditCommands(
			ProjectCommandFactory projectCommandFactory, ProjectOrDomain projectOrDomain,
			MutableTreeNode rootNode) {
		List<EditScenarioStepCommand> stepEditCommands = new ArrayList<EditScenarioStepCommand>();
		if (rootNode != null) {
			Enumeration<MutableTreeNode> enm = rootNode.children();
			while (enm.hasMoreElements()) {
				MutableTreeNode node = enm.nextElement();
				if (node instanceof EditorTreeNode) {
					EditorTreeNode editorNode = (EditorTreeNode) node;
					Component editor = editorNode.getEditor();
					ComponentManipulator man = ComponentManipulators.getManipulator(editor);
					ScenarioStepEditorModel stepModel = (ScenarioStepEditorModel) man
							.getModel(editor);
					Step step = man.getValue(editor, Step.class);
					if (node.getChildCount() > 0) {
						// if there are children then this is a scenario
						if (step instanceof Scenario) {
							EditScenarioCommand command = projectCommandFactory
									.newEditScenarioCommand();
							command.setProjectOrDomain(projectOrDomain);
							command.setEditedBy(getCurrentUser());
							command.setName(stepModel.getName());
							command.setScenarioTypeName(stepModel.getScenarioTypeName());
							command.setScenario((Scenario) step);
							command.setStepCommands(generateStepEditCommands(projectCommandFactory,
									projectOrDomain, node));
							stepEditCommands.add(command);
						} else {
							ConvertStepToScenarioCommand command = projectCommandFactory
									.newConvertStepToScenarioCommand();
							command.setProjectOrDomain(projectOrDomain);
							command.setOriginalScenarioStep(step);
							command.setEditedBy(getCurrentUser());
							command.setName(stepModel.getName());
							command.setScenarioTypeName(stepModel.getScenarioTypeName());
							command.setStepCommands(generateStepEditCommands(projectCommandFactory,
									projectOrDomain, node));
							stepEditCommands.add(command);
						}
					} else {
						// a step
						EditScenarioStepCommand command = projectCommandFactory
								.newEditScenarioStepCommand();
						command.setProjectOrDomain(projectOrDomain);
						command.setEditedBy(getCurrentUser());
						command.setName(stepModel.getName());
						command.setScenarioTypeName(stepModel.getScenarioTypeName());
						command.setStep(step);
						stepEditCommands.add(command);
					}
				}
			}
		}
		return stepEditCommands;
	}

	/**
	 * Controller to add an existing scenario from the scenario selector,
	 * initiated from the addScenario button.
	 * 
	 * @author ron
	 */
	private static class AddScenarioController extends AbstractRequelController {
		static final long serialVersionUID = 0L;

		private final ScenarioStepsEditor editor;
		private final EditorTree editorTree;
		private Scenario rootScenario;

		/**
		 * @param eventDispatcher
		 * @param editorTree
		 * @param rootScenario
		 */
		public AddScenarioController(EventDispatcher eventDispatcher, ScenarioStepsEditor editor,
				EditorTree editorTree) {
			super(eventDispatcher);
			this.editorTree = editorTree;
			this.editor = editor;
		}

		public void setRootScenario(Scenario rootScenario) {
			this.rootScenario = rootScenario;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			editor.messages.setText("");
			if (event instanceof SelectEntityEvent) {
				SelectEntityEvent selectEntityEvent = (SelectEntityEvent) event;
				if (selectEntityEvent.getObject() instanceof Scenario) {
					Scenario scenario = (Scenario) selectEntityEvent.getObject();
					// make sure the selected scenario or its steps don't use
					// the
					// original root scenario, NOTE: the rootScenario may be
					// null
					// if this is a new scenario. In that case no need to check.
					if ((rootScenario != null) && scenario.usesStep(rootScenario)) {
						editor.messages
								.setText("This scenario is a sub-step of the selected scenario "
										+ "and would add a loop in the steps, so it can't be add.");
						return;
					}

					MutableTreeNode rootNode = editorTree.getRootNode();
					EditorTreeNodeFactory factory = editorTree
							.getEditorTreeNodeFactory(Scenario.class);
					MutableTreeNode newNode = factory.createTreeNode(getEventDispatcher(),
							editorTree, scenario);
					editorTree.getModel().insertNodeInto(newNode, rootNode,
							rootNode.getChildCount());
					editorTree.expandPath(new TreePath(editorTree.getModel()
							.getPathToRoot(rootNode)));
				}
			}
		}
	}

	private static class ScenarioStepsEditorManipulator extends AbstractComponentManipulator {

		protected ScenarioStepsEditorManipulator() {
			super();
		}

		@Override
		public Object getModel(Component component) {
			return getComponent(component).editorTree.getModel();
		}

		@Override
		public void setModel(Component component, Object valueModel) {
			setValue(component, valueModel);
		}

		@Override
		public void addListenerToDetectChangesToInput(EditMode editMode, Component component) {
			// this gets handled by the individual step editors.
		}

		@Override
		public <T> T getValue(Component component, Class<T> type) {
			return type.cast(getComponent(component).getScenario());
		}

		@Override
		public void setValue(Component component, Object value) {
			getComponent(component).setScenario((Scenario) value);
		}

		private ScenarioStepsEditor getComponent(Component component) {
			return (ScenarioStepsEditor) component;
		}
	}
}
