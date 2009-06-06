/*
 * $Id$
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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.TreeSet;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.GoalRelation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.CopyGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.EditGoalCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTable;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
public class GoalEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(GoalEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the text field. If the property is undefined "Text" is used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the goal relations field. If the property is undefined "Relations" is
	 * used.
	 */
	public static final String PROP_LABEL_RELATIONS = "Relations.Label";

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the view button in the goal relation edit table column. If the
	 * property is undefined "View" is used.
	 */
	public static final String PROP_VIEW_GOAL_RELATION_BUTTON_LABEL = "ViewGoalRelation.Label";

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the edit button in the goal relation edit table column. If the
	 * property is undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_GOAL_RELATION_BUTTON_LABEL = "EditGoalRelation.Label";

	/**
	 * The name to use in the GoalEditorPanel.properties file to set the label
	 * of the add button in the goal relation editor. If the property is
	 * undefined "Add" is used.
	 */
	public static final String PROP_ADD_GOAL_RELATION_BUTTON_LABEL = "AddGoalRelation.Label";

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
	public GoalEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(GoalEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public GoalEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, Goal.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing goal the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Goal: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - goal name<br>
	 * {1} - project/domain name<br>
	 * For new goal it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Goal"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getGoal() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Goal: {0}"));
			return MessageFormat.format(msgPattern, getGoal().getName(), getProjectOrDomain()
					.getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Goal"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Goal goal = getGoal();
		if (goal != null) {
			addInput(EditGoalCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(goal.getName()));
			addInput(EditGoalCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx(goal.getText()));
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), goal);
			addMultiRowInput("goalContainers", GoalContainersTable.PROP_LABEL_GOAL_CONTAINERS,
					"Referring Entities", new GoalContainersTable(this,
							getResourceBundleHelper(getLocale())), goal);
			addMultiRowInput("relations", PROP_LABEL_RELATIONS, "Relations To Other Goals",
					getRelationsTable(), new NavigatorTableModel((Collection) goal
							.getRelationsFromThisGoal()));
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), goal);

			copyButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_COPY_BUTTON, "Copy")));
			copyButton.addActionListener(new CopyListener(this));
			copyButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput(EditGoalCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput(EditGoalCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx());
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), goal);
			addMultiRowInput("goalContainers", GoalContainersTable.PROP_LABEL_GOAL_CONTAINERS,
					"Referring Entities", new GoalContainersTable(this,
							getResourceBundleHelper(getLocale())), null);
			addMultiRowInput("relations", PROP_LABEL_RELATIONS, "Relations", getRelationsTable(),
					new NavigatorTableModel(new TreeSet<Object>()));
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

	private Component getRelationsTable() {
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		Column col = new Column();
		NavigatorTable relationTable = new NavigatorTable(getGoalRelationTableConfig());
		relationTable.setLayoutData(layoutData);
		col.add(relationTable);
		if ((getGoal() != null) && !isReadOnlyMode()) {
			String buttonLabel = null;
			buttonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_ADD_GOAL_RELATION_BUTTON_LABEL, "Add");
			NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
					getGoal(), GoalRelation.class, null, WorkflowDisposition.NewFlow);
			NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
					getEventDispatcher(), openEditorEvent);
			openEditorButton.setStyleName(STYLE_NAME_DEFAULT);
			openEditorButton.setLayoutData(layoutData);
			col.add(openEditorButton);
		}
		return col;
	}

	private NavigatorTableConfig getGoalRelationTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalRelation goalRelation = (GoalRelation) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_GOAL_RELATION_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_GOAL_RELATION_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, goalRelation, GoalRelation.class, null,
								WorkflowDisposition.NewFlow);
						NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
								getEventDispatcher(), openEditorEvent);
						openEditorButton.setStyleName(STYLE_NAME_PLAIN);
						RowLayoutData rld = new RowLayoutData();
						rld.setAlignment(Alignment.ALIGN_CENTER);
						openEditorButton.setLayoutData(rld);
						return openEditorButton;
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Relation Type",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalRelation goalRelation = (GoalRelation) model.getBackingObject(row);
						return goalRelation.getRelationType().toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("To Goal",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalRelation goalRelation = (GoalRelation) model.getBackingObject(row);
						return goalRelation.getToGoal().getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalRelation goalRelation = (GoalRelation) model.getBackingObject(row);
						return goalRelation.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						GoalRelation goalRelation = (GoalRelation) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(goalRelation.getDateCreated());
					}
				}));

		return tableConfig;
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
			EditGoalCommand command = getProjectCommandFactory().newEditGoalCommand();
			command.setGoal(getGoal());
			command.setGoalContainer(getGoalContainer());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditGoalCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditGoalCommand.FIELD_TEXT, String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getGoal()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditGoalCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditGoalCommand.FIELD_TEXT, String.class);
				Goal newGoal = getProjectRepository().get(getGoal());

				setTargetObject(newGoal);
				if (!newName.equals(newGoal.getName()) || !newText.equals(newGoal.getText())) {
					setGeneralMessage("The goal was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newGoal.getName())) {
						setValidationMessage(EditGoalCommand.FIELD_NAME, "Your input '" + newName
								+ "'");
						setInputValue(EditGoalCommand.FIELD_NAME, newGoal.getName());
					}
					if (!newText.equals(newGoal.getText())) {
						setValidationMessage(EditGoalCommand.FIELD_TEXT, "Your input '" + newText
								+ "'");
						setInputValue(EditGoalCommand.FIELD_TEXT, newGoal.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, newGoal));
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
			log.error("could not save the goal: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteGoalCommand deleteGoalCommand = getProjectCommandFactory().newDeleteGoalCommand();
			deleteGoalCommand.setEditedBy(getCurrentUser());
			deleteGoalCommand.setGoal(getGoal());
			deleteGoalCommand = getCommandHandler().execute(deleteGoalCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getGoal()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private ProjectOrDomain getProjectOrDomain() {
		if (getTargetObject() instanceof ProjectOrDomain) {
			return (ProjectOrDomain) getTargetObject();
		} else if (getTargetObject() instanceof ProjectOrDomainEntity) {
			return ((ProjectOrDomainEntity) getTargetObject()).getProjectOrDomain();
		}
		return null;
	}

	private GoalContainer getGoalContainer() {
		if (getTargetObject() instanceof GoalContainer) {
			return (GoalContainer) getTargetObject();
		}
		return null;
	}

	private Goal getGoal() {
		if (getTargetObject() instanceof Goal) {
			return (Goal) getTargetObject();
		}
		return null;
	}

	private static class CopyListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GoalEditorPanel panel;

		private CopyListener(GoalEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				CopyGoalCommand copyGoalCommand = panel.getProjectCommandFactory()
						.newCopyGoalCommand();
				copyGoalCommand.setEditedBy(panel.getCurrentUser());
				copyGoalCommand.setOriginalGoal(panel.getGoal());
				copyGoalCommand = panel.getCommandHandler().execute(copyGoalCommand);
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, copyGoalCommand.getNewGoal()));
				panel.getEventDispatcher().dispatchEvent(
						new OpenPanelEvent(this, PanelActionType.Editor, copyGoalCommand
								.getNewGoal(), Goal.class, null));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not copy entity: " + e);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GoalEditorPanel panel;

		private UpdateListener(GoalEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Goal existingGoal = panel.getGoal();
			if ((e instanceof UpdateEntityEvent) && (existingGoal != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Goal updatedGoal = null;
				if (event.getObject() instanceof Goal) {
					updatedGoal = (Goal) event.getObject();
					if ((event instanceof DeletedEntityEvent) && existingGoal.equals(updatedGoal)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingGoal));
						return;
					}
				} else if (event.getObject() instanceof GoalRelation) {
					GoalRelation updatedGoalRelation = (GoalRelation) event.getObject();
					updatedGoal = updatedGoalRelation.getFromGoal();
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingGoal.getAnnotations().contains(updatedAnnotation)) {
							existingGoal.getAnnotations().remove(updatedAnnotation);
							updatedGoal = existingGoal;
						}
					} else if (updatedAnnotation.getAnnotatables().contains(existingGoal)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingGoal)) {
								updatedGoal = (Goal) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedGoal != null) && updatedGoal.equals(existingGoal)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditGoalCommand.FIELD_NAME, updatedGoal.getName());
					panel.setInputValue(EditGoalCommand.FIELD_TEXT, updatedGoal.getText());
					panel.setInputValue("glossaryTerms", updatedGoal);
					panel.setInputValue("goalContainers", updatedGoal);
					panel.setInputValue("relations", updatedGoal.getRelationsFromThisGoal());
					panel.setInputValue("annotations", updatedGoal);
					panel.setTargetObject(updatedGoal);
				}
			}
		}
	}
}
