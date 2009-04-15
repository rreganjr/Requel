/*
 * $Id: ScenarioStepsEditor.java,v 1.16 2009/03/26 08:17:34 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

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
import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.project.command.ConvertStepToScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioStepCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelComponent;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelController;
import edu.harvard.fas.rregan.uiframework.navigation.NavigatorButton;
import edu.harvard.fas.rregan.uiframework.navigation.WorkflowDisposition;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;
import edu.harvard.fas.rregan.uiframework.navigation.event.NavigationEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.Panel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.AbstractComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTree;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNode;
import edu.harvard.fas.rregan.uiframework.panel.editor.tree.EditorTreeNodeFactory;

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
