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
package com.rreganjr.requel.annotation.ui;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
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
import net.sf.echopm.panel.editor.ToggleButtonModelEx;

/**
 * @author ron
 */
public class IssueEditorPanel extends AbstractRequelAnnotationEditorPanel {
	private static final Log log = LogFactory.getLog(IssueEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the issue text field. If the property is undefined "Issue" is used.
	 */
	public static final String PROP_LABEL_ISSUE = "Issue.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the issue must be resolved field. If the property is undefined "Must
	 * Be Resolved?" is used.
	 */
	public static final String PROP_LABEL_MUST_BE_RESOLVED = "MustBeResolved.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the resolved by field. If the property is undefined "Resolved By" is
	 * used.
	 */
	public static final String PROP_LABEL_RESOLVED_BY = "ResolvedBy.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the positions field. If the property is undefined "Positions" is used.
	 */
	public static final String PROP_LABEL_POSITIONS = "Positions.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the view button in the position edit table column. If the property is
	 * undefined "View" is used.
	 */
	public static final String PROP_VIEW_POSITION_BUTTON_LABEL = "ViewPosition.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the edit button in the position edit table column. If the property is
	 * undefined "Edit" is used.
	 */
	public static final String PROP_EDIT_POSITION_BUTTON_LABEL = "EditPosition.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the button used to resolve the issue with the position. If the
	 * property is undefined "Resolve" is used.
	 */
	public static final String PROP_RESOLVE_WITH_POSITION_BUTTON_LABEL = "ResolvePosition.Label";

	/**
	 * The name to use in the IssueEditorPanel.properties file to set the label
	 * of the add button in the positions editor. If the property is undefined
	 * "Add" is used.
	 */
	public static final String PROP_ADD_POSITION_BUTTON_LABEL = "AddPosition.Label";

	private final AnnotationCommandFactory annotationCommandFactory;
	private UpdateListener updateListener;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted = false;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public IssueEditorPanel(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		this(IssueEditorPanel.class.getName(), commandHandler, annotationCommandFactory,
				annotationRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param annotationRepository
	 */
	public IssueEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			AnnotationRepository annotationRepository) {
		super(resourceBundleName, Issue.class, commandHandler, annotationRepository);
		this.annotationCommandFactory = annotationCommandFactory;
	}

	/**
	 * If the editor is editing an existing issue the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Edit Issue"<br>
	 * For a new issue it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Issue"<br>
	 * 
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.editor.AbstractEditorPanel#PROP_NEW_OBJECT_PANEL_TITLE
	 * @see net.sf.echopm.panel.Panel#PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getIssue() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Edit Issue"));
			return MessageFormat.format(msgPattern, getIssue().toString());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Issue"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		String resolvedByText = "Unresolved";
		Issue issue = getIssue();
		if (issue != null) {
			addInput("text", PROP_LABEL_ISSUE, "Issue", new TextArea(), new StringDocumentEx(issue
					.getText()));
			addInput("mustBeResolved", PROP_LABEL_MUST_BE_RESOLVED, "Must Be Resolved?",
					new CheckBox(), new ToggleButtonModelEx(issue.isMustBeResolved()));
			if (issue.getResolvedByPosition() != null) {
				resolvedByText = issue.getResolvedByPosition().getText();
			}
			addInput("resolvedBy", PROP_LABEL_RESOLVED_BY, "Resolved By", new Label(),
					resolvedByText);
			addMultiRowInput("positions", PROP_LABEL_POSITIONS, "Positions", getPositionsTable(),
					new NavigatorTableModel((Collection) issue.getPositions()));
			addMultiRowInput("annotatables", AnnotationRefererTable.PROP_ANNOTATABLES_LABEL,
					"Referring Entities", new AnnotationRefererTable(this,
							getResourceBundleHelper(getLocale())), issue);
		} else {
			addInput("text", PROP_LABEL_ISSUE, "Issue", new TextArea(), new StringDocumentEx());
			addInput("mustBeResolved", PROP_LABEL_MUST_BE_RESOLVED, "Must Be Resolved?",
					new CheckBox(), new ToggleButtonModelEx(true));
			addInput("resolvedBy", PROP_LABEL_RESOLVED_BY, "Resolved By", new Label(),
					resolvedByText);
			addMultiRowInput("positions", PROP_LABEL_POSITIONS, "Positions", getPositionsTable(),
					new NavigatorTableModel(new TreeSet<Object>()));
			addMultiRowInput("annotatables", AnnotationRefererTable.PROP_ANNOTATABLES_LABEL,
					"Referring Entities", new AnnotationRefererTable(this,
							getResourceBundleHelper(getLocale())), null);
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

	private Component getPositionsTable() {
		ColumnLayoutData layoutData = new ColumnLayoutData();
		layoutData.setAlignment(Alignment.ALIGN_CENTER);
		Column col = new Column();
		NavigatorTable relationTable = new NavigatorTable(getPositionsTableConfig());
		relationTable.setLayoutData(layoutData);
		col.add(relationTable);
		if ((getIssue() != null) && !isReadOnlyMode()) {
			String buttonLabel = null;
			buttonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_ADD_POSITION_BUTTON_LABEL, "Add");
			NavigationEvent openEditorEvent = new OpenPanelEvent(this, PanelActionType.Editor,
					getIssue(), Position.class, null, WorkflowDisposition.NewFlow);
			NavigatorButton openEditorButton = new NavigatorButton(buttonLabel,
					getEventDispatcher(), openEditorEvent);
			openEditorButton.setStyleName(STYLE_NAME_DEFAULT);
			openEditorButton.setLayoutData(layoutData);
			col.add(openEditorButton);
		}
		return col;
	}

	private NavigatorTableConfig getPositionsTableConfig() {
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Position position = (Position) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_POSITION_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_POSITION_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, position, Position.class, null,
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

		if (!isReadOnlyMode()) {
			tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
					new NavigatorTableCellValueFactory() {
						@Override
						public Object getValueAt(NavigatorTableModel model, int column, int row) {
							final Position position = (Position) model.getBackingObject(row);
							String buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_RESOLVE_WITH_POSITION_BUTTON_LABEL, "Resolve");
							Button resolveButton = new Button(buttonLabel);
							resolveButton.addActionListener(new ResolveListener(
									IssueEditorPanel.this, position));
							resolveButton.setStyleName(STYLE_NAME_PLAIN);
							RowLayoutData rld = new RowLayoutData();
							rld.setAlignment(Alignment.ALIGN_CENTER);
							resolveButton.setLayoutData(rld);
							return resolveButton;
						}
					}));
		}

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Text",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Position position = (Position) model.getBackingObject(row);
						return position.getText();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Position position = (Position) model.getBackingObject(row);
						return position.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Position position = (Position) model.getBackingObject(row);
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						return formatter.format(position.getDateCreated());
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
			EditIssueCommand command = getAnnotationCommandFactory().newEditIssueCommand();
			command.setGroupingObject(getGroupingObject());
			command.setIssue(getIssue());
			command.setAnnotatable(getAnnotatable());
			command.setEditedBy(getCurrentUser());
			command.setText(getInputValue("text", String.class));
			command.setMustBeResolved(getInputValue("mustBeResolved", Boolean.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getIssue()));
		} catch (EntityException e) {
			if ((e.getEntityPropertyNames() != null) && (e.getEntityPropertyNames().length > 0)) {
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
		super.delete();
		try {
			Issue issue = getIssue();
			Set<Position> positions = getIssue().getPositions();
			DeleteIssueCommand deleteIssueCommand = getAnnotationCommandFactory()
					.newDeleteIssueCommand();
			deleteIssueCommand.setIssue(issue);
			deleteIssueCommand.setEditedBy(getCurrentUser());
			deleteIssueCommand = getCommandHandler().execute(deleteIssueCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, issue));
			for (Position position : positions) {
				getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, position));
			}
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Issue getIssue() {
		if (getTargetObject() instanceof Issue) {
			return (Issue) getTargetObject();
		}
		return null;
	}

	private AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	private static class ResolveListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final IssueEditorPanel panel;
		private final Position position;

		private ResolveListener(IssueEditorPanel panel, Position position) {
			this.panel = panel;
			this.position = position;
		}

		public void actionPerformed(ActionEvent event) {
			try {
				ResolveIssueCommand command = panel.getAnnotationCommandFactory()
						.newResolveIssueCommand(position);
				command.setIssue(panel.getIssue());
				command.setPosition(position);
				command.setEditedBy(panel.getCurrentUser());
				command = panel.getCommandHandler().execute(command);
				Issue issue = command.getIssue();
				panel.getEventDispatcher().dispatchEvent(new UpdateEntityEvent(panel, issue));
			} catch (Exception e) {
				log.error("Exception resolving issue " + position.getIssues() + " with positition "
						+ position, e);
				panel.setGeneralMessage(e.toString());
			}
		}
	}

	// TODO: it may be better to have different standardized update listeners
	// for different types of updated or deleted objects associated with the
	// input controls like an annotatables table.
	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final IssueEditorPanel panel;

		private UpdateListener(IssueEditorPanel panel) {
			this.panel = panel;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				if (panel.deleted) {
					return;
				}
				Issue existingIssue = panel.getIssue();
				if ((e instanceof UpdateEntityEvent) && (existingIssue != null)) {
					UpdateEntityEvent event = (UpdateEntityEvent) e;
					Issue updatedIssue = null;
					if (event.getObject() instanceof Issue) {
						updatedIssue = (Issue) event.getObject();
						if ((event instanceof DeletedEntityEvent)
								&& existingIssue.equals(updatedIssue)) {
							panel.deleted = true;
							panel.getEventDispatcher().dispatchEvent(
									new DeletedEntityEvent(this, panel, existingIssue));
							return;
						}
					} else if (event.getObject() instanceof Position) {
						Position updatedPosition = (Position) event.getObject();
						if (event instanceof DeletedEntityEvent) {
							existingIssue.getPositions().remove(updatedPosition);
							updatedIssue = existingIssue;
						} else if (updatedPosition.getIssues().contains(panel.getIssue())) {
							for (Issue positionIssue : updatedPosition.getIssues()) {
								if (positionIssue.equals(existingIssue)) {
									updatedIssue = positionIssue;
									break;
								}
							}
						}
					} else if ((event instanceof DeletedEntityEvent)
							&& (event.getObject() instanceof Annotatable)) {
						Annotatable annotatable = (Annotatable) event.getObject();
						if (existingIssue.getAnnotatables().contains(annotatable)) {
							existingIssue.getAnnotatables().remove(annotatable);
						}
						updatedIssue = existingIssue;
					}
					if ((updatedIssue != null) && updatedIssue.equals(existingIssue)) {
						panel.setTargetObject(updatedIssue);
						panel.setInputValue("text", updatedIssue.getText());
						panel.setInputValue("mustBeResolved", updatedIssue.isMustBeResolved());
						if (updatedIssue.getResolvedByPosition() != null) {
							panel.setInputValue("resolvedBy", updatedIssue.getResolvedByPosition()
									.getText());
						}
						panel.setInputValue("annotatables", updatedIssue);
						panel.setInputValue("positions", updatedIssue.getPositions());
					}

				}
			} catch (Exception ex) {
				log.error("Exception processing update.", ex);
			}
		}
	}
}
