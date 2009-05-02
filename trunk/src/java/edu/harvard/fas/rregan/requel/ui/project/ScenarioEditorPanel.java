/*
 * $Id: ScenarioEditorPanel.java,v 1.28 2009/03/26 08:17:34 rregan Exp $
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
package edu.harvard.fas.rregan.requel.ui.project;

import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import echopointng.tree.DefaultTreeModel;
import echopointng.tree.MutableTreeNode;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ScenarioType;
import edu.harvard.fas.rregan.requel.project.command.CopyScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.EditScenarioCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;

/**
 * @author ron
 */
public class ScenarioEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(ScenarioEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the ScenarioEditorPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the ScenarioEditorPanel.properties file to set the
	 * label of the scenario type field. If the property is undefined "Scenario
	 * Type" is used.
	 */
	public static final String PROP_LABEL_SCENARIO_TYPE = "ScenarioType.Label";

	/**
	 * The name to use in the ScenarioEditorPanel.properties file to set the
	 * label of the text field. If the property is undefined "Text" is used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	private UpdateListener updateListener;
	private Button copyButton;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ScenarioEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(ScenarioEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ScenarioEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, Scenario.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing Scenario the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Scenario: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - Scenario name<br>
	 * {1} - project/domain name<br>
	 * For new Scenario it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Scenario"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getScenario() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
							"Scenario: {0}"));
			return MessageFormat.format(msgPattern, getScenario().getName(), getProjectOrDomain()
					.getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale())
							.getString(PROP_PANEL_TITLE, "New Scenario"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Scenario scenario = getScenario();
		if (scenario != null) {
			addInput(EditScenarioCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(scenario.getName()));
			addInput("scenarioType", PROP_LABEL_SCENARIO_TYPE, "Type", new SelectField(),
					new CombinedListModel(getScenarioTypeNames(), scenario.getType().toString(),
							true));
			addInput(EditScenarioCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx(scenario.getText()));
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioSteps", ScenarioStepsEditor.PROP_LABEL_SCENARIO_STEPS,
					"Scenario Steps", new ScenarioStepsEditor(getProjectOrDomain(), this,
							getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioUseCases",
					ScenarioUseCasesTable.PROP_LABEL_SCENARIO_USECASES, "Referring Use Cases",
					new ScenarioUseCasesTable(this, getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioScenarios",
					ScenarioScenariosTable.PROP_LABEL_SCENARIO_SCENARIOS, "Referring Scenarios",
					new ScenarioScenariosTable(this, getResourceBundleHelper(getLocale())),
					scenario);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), scenario);
			copyButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_COPY_BUTTON, "Copy")));
			copyButton.addActionListener(new CopyListener(this));
			copyButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput(EditScenarioCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput(
					"scenarioType",
					PROP_LABEL_SCENARIO_TYPE,
					"Type",
					new SelectField(),
					new CombinedListModel(getScenarioTypeNames(), ScenarioType.Primary.name(), true));
			addInput(EditScenarioCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx());
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioSteps", ScenarioStepsEditor.PROP_LABEL_SCENARIO_STEPS,
					"Scenario Steps", new ScenarioStepsEditor(getProjectOrDomain(), this,
							getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioUseCases",
					ScenarioUseCasesTable.PROP_LABEL_SCENARIO_USECASES, "Referring Use Cases",
					new ScenarioUseCasesTable(this, getResourceBundleHelper(getLocale())), scenario);
			addMultiRowInput("scenarioScenarios",
					ScenarioScenariosTable.PROP_LABEL_SCENARIO_SCENARIOS, "Referring Scenarios",
					new ScenarioScenariosTable(this, getResourceBundleHelper(getLocale())),
					scenario);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), null);
		}

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		removeAll();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
			updateListener = null;
		}
	}

	@Override
	public void cancel() {
		super.cancel();
		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
	}

	@Override
	public void save() {
		try {
			super.save();
			ScenarioStepsEditor scenarioEditor = (ScenarioStepsEditor) getInput("scenarioSteps");
			DefaultTreeModel treeModel = getInputModel("scenarioSteps", DefaultTreeModel.class);
			MutableTreeNode rootNode = (MutableTreeNode) treeModel.getRoot();

			EditScenarioCommand command = getProjectCommandFactory().newEditScenarioCommand();
			command.setProjectOrDomain(getProjectOrDomain());
			command.setScenario(getScenario());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditScenarioCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditScenarioCommand.FIELD_TEXT, String.class));
			command.setScenarioTypeName(getInputValue("scenarioType", String.class));
			command.setStepCommands(scenarioEditor.generateStepEditCommands(
					getProjectCommandFactory(), getProjectOrDomain(), rootNode));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
				// TODO: remove other listeners?
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getScenario()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditScenarioCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditScenarioCommand.FIELD_TEXT, String.class);
				Scenario newScenario = getProjectRepository().get(getScenario());

				setTargetObject(newScenario);
				if (!newName.equals(newScenario.getName())
						|| !newText.equals(newScenario.getText())) {
					setGeneralMessage("The scenario was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newScenario.getName())) {
						setValidationMessage(EditScenarioCommand.FIELD_NAME, "Your input '"
								+ newName + "'");
						setInputValue(EditScenarioCommand.FIELD_NAME, newScenario.getName());
					}
					if (!newText.equals(newScenario.getText())) {
						setValidationMessage(EditScenarioCommand.FIELD_TEXT, "Your input '"
								+ newText + "'");
						setInputValue(EditScenarioCommand.FIELD_TEXT, newScenario.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, newScenario));
				}

			} else if ((e.getEntityPropertyNames() != null)
					&& (e.getEntityPropertyNames().length > 0)) {
				for (String propertyName : e.getEntityPropertyNames()) {
					setValidationMessage(propertyName, e.getMessage());
				}
			} else if ((e.getCause() != null) && (e.getCause() instanceof InvalidStateException)) {
				InvalidStateException ise = (InvalidStateException) e.getCause();
				for (InvalidValue invalidValue : ise.getInvalidValues()) {
					String propertyName = invalidValue.getPropertyName();
					setValidationMessage(propertyName, invalidValue.getMessage());
				}
			} else {
				setGeneralMessage(e.toString());
			}

		} catch (Exception e) {
			log.error("could not save the scenario: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteScenarioCommand deleteScenarioCommand = getProjectCommandFactory()
					.newDeleteScenarioCommand();
			deleteScenarioCommand.setEditedBy(getCurrentUser());
			deleteScenarioCommand.setScenario(getScenario());
			deleteScenarioCommand = getCommandHandler().execute(deleteScenarioCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getScenario()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Set<String> getScenarioTypeNames() {
		Set<String> scenarioTypeNames = new TreeSet<String>();
		for (ScenarioType scenarioType : ScenarioType.values()) {
			scenarioTypeNames.add(scenarioType.toString());
		}
		return scenarioTypeNames;
	}

	private ProjectOrDomain getProjectOrDomain() {
		if (getTargetObject() instanceof ProjectOrDomain) {
			return (ProjectOrDomain) getTargetObject();
		} else if (getTargetObject() instanceof ProjectOrDomainEntity) {
			return ((ProjectOrDomainEntity) getTargetObject()).getProjectOrDomain();
		}
		return null;
	}

	private Scenario getScenario() {
		if (getTargetObject() instanceof Scenario) {
			return (Scenario) getTargetObject();
		}
		return null;
	}

	private static class CopyListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ScenarioEditorPanel panel;

		private CopyListener(ScenarioEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				CopyScenarioCommand copyScenarioCommand = panel.getProjectCommandFactory()
						.newCopyScenarioCommand();
				copyScenarioCommand.setEditedBy(panel.getCurrentUser());
				copyScenarioCommand.setOriginalScenario(panel.getScenario());
				copyScenarioCommand = panel.getCommandHandler().execute(copyScenarioCommand);
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, copyScenarioCommand.getNewScenario()));
				panel.getEventDispatcher().dispatchEvent(
						new OpenPanelEvent(this, PanelActionType.Editor, copyScenarioCommand
								.getNewScenario(), Scenario.class, null));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not copy entity: " + e);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ScenarioEditorPanel panel;

		private UpdateListener(ScenarioEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Scenario existingScenario = panel.getScenario();
			if ((e instanceof UpdateEntityEvent) && (existingScenario != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Scenario updatedScenario = null;
				if (event.getObject() instanceof Scenario) {
					updatedScenario = (Scenario) event.getObject();
					if ((event instanceof DeletedEntityEvent)
							&& existingScenario.equals(updatedScenario)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingScenario));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					Goal updatedGoal = (Goal) event.getObject();
					if (updatedGoal.getReferers().contains(existingScenario)) {
						for (GoalContainer gc : updatedGoal.getReferers()) {
							if (gc.equals(existingScenario)) {
								updatedScenario = (Scenario) gc;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Actor) {
					Actor updatedActor = (Actor) event.getObject();
					if (updatedActor.getReferers().contains(existingScenario)) {
						for (ActorContainer ac : updatedActor.getReferers()) {
							if (ac.equals(existingScenario)) {
								updatedScenario = (Scenario) ac;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingScenario.getAnnotations().contains(updatedAnnotation)) {
							existingScenario.getAnnotations().remove(updatedAnnotation);
						}
						updatedScenario = existingScenario;
					} else if (updatedAnnotation.getAnnotatables().contains(existingScenario)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingScenario)) {
								updatedScenario = (Scenario) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedScenario != null) && updatedScenario.equals(existingScenario)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditScenarioCommand.FIELD_NAME, updatedScenario.getName());
					panel.setInputValue("scenarioType", updatedScenario.getType().toString());
					panel.setInputValue(EditScenarioCommand.FIELD_TEXT, updatedScenario.getText());
					panel.setInputValue("glossaryTerms", updatedScenario);
					panel.setInputValue("scenarioUseCases", updatedScenario);
					panel.setInputValue("scenarioScenarios", updatedScenario);
					panel.setInputValue("scenarioSteps", updatedScenario);
					panel.setInputValue("annotations", updatedScenario);
					panel.setTargetObject(updatedScenario);
				}
			}
		}
	}
}
