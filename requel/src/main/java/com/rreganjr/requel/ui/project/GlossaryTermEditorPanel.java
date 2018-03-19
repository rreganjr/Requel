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

import java.text.MessageFormat;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.text.StringDocumentEx;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.DeleteGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.ReplaceGlossaryTermCommand;
import com.rreganjr.requel.ui.annotation.AnnotationsTable;
import net.sf.echopm.navigation.SelectorButton;
import net.sf.echopm.navigation.SelectorButton.SelectionIndicatorLabelAdapter;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;

/**
 * Editor for Glossary Terms. Allow editing the definition, select a canonical
 * term and change all uses of this term in project entities to the cannonical
 * term.
 * 
 * @author ron
 */
public class GlossaryTermEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Log log = LogFactory.getLog(GlossaryTermEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the GlossaryTermEditorPanel.properties file to set the
	 * label of the name field. If the property is undefined "Term" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the GlossaryTermEditorPanel.properties file to set the
	 * label of the text field. If the property is undefined "Description" is
	 * used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	/**
	 * The name to use in the GlossaryTermEditorPanel.properties file to set the
	 * label of the canonical term field. If the property is undefined
	 * "Canonical Term" is used.
	 */
	public static final String PROP_LABEL_CANONICAL_TERM = "CanonicalTerm.Label";

	/**
	 * The name to use in the GlossaryTermEditorPanel.properties file to set the
	 * label of the replace term function button. If the property is undefined
	 * "Replace Term" is used.
	 */
	public static final String PROP_LABEL_REPLACE_TERM_BUTTON = "ReplaceTermButton.Label";

	private UpdateListener updateListener;
	private Button replaceTermButton;
	private SelectorButton canonicalTermSelectorButton;

	// this is set by the DeleteListener so that the UpdateListener can ignore
	// events between when the object was deleted and the panel goes away.
	private boolean deleted;

	/**
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public GlossaryTermEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(GlossaryTermEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public GlossaryTermEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, GlossaryTerm.class, commandHandler, projectCommandFactory,
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
		if (getGlossaryTerm() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Term: {0}"));
			return MessageFormat.format(msgPattern, getGlossaryTerm().getName(),
					getProjectOrDomain().getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Term"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		GlossaryTerm glossaryTerm = getGlossaryTerm();
		if (glossaryTerm != null) {
			TextField nameInput = addInput(EditGlossaryTermCommand.FIELD_NAME, PROP_LABEL_NAME,
					"Term", new TextField(), new StringDocumentEx(glossaryTerm.getName()));
			nameInput.setEnabled(false);
			addInput(EditGlossaryTermCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Description",
					new TextArea(), new StringDocumentEx(glossaryTerm.getText()));
			canonicalTermSelectorButton = addInput("canonicalTerm", PROP_LABEL_CANONICAL_TERM,
					"Canonical Term", new SelectorButton(
							new CanonicalTermSelectionIndicatorLabelAdapter(), GlossaryTerm.class,
							glossaryTerm.getCanonicalTerm(), Project.class, glossaryTerm
									.getProjectOrDomain(),
							ProjectManagementPanelNames.PROJECT_GLOSSARY_TERM_SELECTOR_PANEL_NAME),
					glossaryTerm.getCanonicalTerm());
			addMultiRowInput("termContainers",
					GlossaryTermRefererTable.PROP_LABEL_GLOSSARY_TERM_CONTAINERS,
					"Referring Entities", new GlossaryTermRefererTable(this,
							getResourceBundleHelper(getLocale())), glossaryTerm);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), glossaryTerm);
		} else {
			addInput(EditGlossaryTermCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput(EditGlossaryTermCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Description",
					new TextArea(), new StringDocumentEx());
			canonicalTermSelectorButton = addInput("canonicalTerm", PROP_LABEL_CANONICAL_TERM,
					"Canonical Term", new SelectorButton(
							new CanonicalTermSelectionIndicatorLabelAdapter(), GlossaryTerm.class,
							null, Project.class, getProjectOrDomain(),
							ProjectManagementPanelNames.PROJECT_GLOSSARY_TERM_SELECTOR_PANEL_NAME),
					null);
			addMultiRowInput("termContainers",
					GlossaryTermRefererTable.PROP_LABEL_GLOSSARY_TERM_CONTAINERS,
					"Referring Entities", new GlossaryTermRefererTable(this,
							getResourceBundleHelper(getLocale())), null);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), null);
		}

		replaceTermButton = addActionButton(new Button(getResourceBundleHelper(getLocale())
				.getString(PROP_LABEL_REPLACE_TERM_BUTTON, "Replace Term")));
		replaceTermButton.addActionListener(new ReplaceTermButtonListener(this));
		enableReplaceTermButton();

		canonicalTermSelectorButton.addChangeListener(new CanonicalTermChangeListener(this));

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
			EditGlossaryTermCommand command = getProjectCommandFactory()
					.newEditGlossaryTermCommand();
			command.setProjectOrDomain(getProjectOrDomain());
			command.setGlossaryTerm(getGlossaryTerm());
			command.setCanonicalTerm(getInputValue("canonicalTerm", GlossaryTerm.class));
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditGlossaryTermCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditGlossaryTermCommand.FIELD_TEXT, String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
			}
			getEventDispatcher().dispatchEvent(
					new UpdateEntityEvent(this, command.getGlossaryTerm()));
			enableReplaceTermButton();
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditGlossaryTermCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditGlossaryTermCommand.FIELD_TEXT, String.class);
				GlossaryTerm newGlossaryTerm = getProjectRepository().get(getGlossaryTerm());

				setTargetObject(newGlossaryTerm);
				if (!newName.equals(newGlossaryTerm.getName())
						|| !newText.equals(newGlossaryTerm.getText())) {
					setGeneralMessage("The term was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newGlossaryTerm.getName())) {
						setValidationMessage(EditGlossaryTermCommand.FIELD_NAME, "Your input '"
								+ newName + "'");
						setInputValue(EditGlossaryTermCommand.FIELD_NAME, newGlossaryTerm.getName());
					}
					if (!newText.equals(newGlossaryTerm.getText())) {
						setValidationMessage(EditGlossaryTermCommand.FIELD_TEXT, "Your input '"
								+ newText + "'");
						setInputValue(EditGlossaryTermCommand.FIELD_TEXT, newGlossaryTerm.getText());
					}
				} else {
					getEventDispatcher()
							.dispatchEvent(new UpdateEntityEvent(this, newGlossaryTerm));
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
			DeleteGlossaryTermCommand deleteGlossaryTermCommand = getProjectCommandFactory()
					.newDeleteGlossaryTermCommand();
			deleteGlossaryTermCommand.setEditedBy(getCurrentUser());
			deleteGlossaryTermCommand.setGlossaryTerm(getGlossaryTerm());
			deleteGlossaryTermCommand = getCommandHandler().execute(deleteGlossaryTermCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getGlossaryTerm()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private void enableReplaceTermButton() {
		if ((getGlossaryTerm() != null)
				&& (((getGlossaryTerm().getCanonicalTerm() != null) || (getInputValue(
						"canonicalTerm", GlossaryTerm.class) != null)) && (getGlossaryTerm()
						.getReferers().size() > 0))) {
			replaceTermButton.setEnabled(true);
		} else {
			replaceTermButton.setEnabled(false);
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

	private GlossaryTerm getGlossaryTerm() {
		if (getTargetObject() instanceof GlossaryTerm) {
			return (GlossaryTerm) getTargetObject();
		}
		return null;
	}

	private static class CanonicalTermSelectionIndicatorLabelAdapter implements
			SelectionIndicatorLabelAdapter {

		@Override
		public String getLabelString(Object selectedObject) {
			if (selectedObject != null) {
				return ((GlossaryTerm) selectedObject).getName();
			}
			return "<no term selected>";
		}
	}

	private static class CanonicalTermChangeListener implements ChangeListener {
		static final long serialVersionUID = 0L;

		private final GlossaryTermEditorPanel panel;

		private CanonicalTermChangeListener(GlossaryTermEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			panel.enableReplaceTermButton();
		}
	}

	private static class ReplaceTermButtonListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GlossaryTermEditorPanel panel;

		private ReplaceTermButtonListener(GlossaryTermEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// TODO: should this be done in a separate thread?
				ReplaceGlossaryTermCommand command = panel.getProjectCommandFactory()
						.newReplaceGlossaryTermCommand();
				command.setEditedBy(panel.getCurrentUser());
				command.setGlossaryTerm(panel.getGlossaryTerm());
				if (panel.getGlossaryTerm().getCanonicalTerm() == null) {
					command.setCanonicalTerm(panel.getInputValue("canonicalTerm",
							GlossaryTerm.class));
				}
				command = panel.getCommandHandler().execute(command);
				GlossaryTerm glossaryTerm = command.getGlossaryTerm();
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, glossaryTerm));
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, glossaryTerm.getCanonicalTerm()));
				for (ProjectOrDomainEntity entity : command.getUpdatedEntities()) {
					panel.getEventDispatcher().dispatchEvent(
							new UpdateEntityEvent(this, null, entity));
				}
			} catch (Exception ex) {
				panel.setGeneralMessage(ex.toString());
				log.warn(ex, ex);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final GlossaryTermEditorPanel panel;

		private UpdateListener(GlossaryTermEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			GlossaryTerm currentTerm = panel.getGlossaryTerm();
			if ((e instanceof UpdateEntityEvent) && (currentTerm != null)) {
				// TODO: the simple thing to do is just update the UI with the
				// existing object. It should be a proxy so the lastest values
				// will automatically get loaded. Although the cases such as
				// the ReplaceTermButtonListener above may send lots of messages
				// and this may refresh unnecessarily many times.

				UpdateEntityEvent event = (UpdateEntityEvent) e;
				GlossaryTerm updatedTerm = null;
				if (event.getObject() instanceof GlossaryTerm) {
					updatedTerm = (GlossaryTerm) event.getObject();
					if ((event instanceof DeletedEntityEvent) && currentTerm.equals(updatedTerm)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, currentTerm));
						return;
					}
				} else if (event.getObject() instanceof ProjectOrDomainEntity) {
					// the entity may now refer to the term, so reload the term
					updatedTerm = currentTerm;
				} else if (event.getObject() instanceof Annotation) {
					// an issue related to the term may have changed
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (currentTerm.getAnnotations().contains(updatedAnnotation)) {
						currentTerm.getAnnotations().remove(updatedAnnotation);
					}
					if (!(event instanceof DeletedEntityEvent)) {
						currentTerm.getAnnotations().add(updatedAnnotation);
					}
					updatedTerm = currentTerm;
				}

				if ((updatedTerm != null) && updatedTerm.equals(currentTerm)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditGlossaryTermCommand.FIELD_NAME, updatedTerm.getName());
					panel.setInputValue(EditGlossaryTermCommand.FIELD_TEXT, updatedTerm.getText());
					panel.setInputValue("canonicalTerm", updatedTerm.getCanonicalTerm());
					panel.setInputValue("termContainers", updatedTerm);
					panel.setInputValue("annotations", updatedTerm);

					panel.setTargetObject(updatedTerm);
					panel.enableReplaceTermButton();
				}
			}
		}
	}
}
