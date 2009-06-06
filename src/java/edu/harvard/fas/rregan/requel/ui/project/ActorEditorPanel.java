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

import java.text.MessageFormat;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

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
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.CopyActorCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteActorCommand;
import edu.harvard.fas.rregan.requel.project.command.EditActorCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.panel.PanelActionType;

/**
 * @author ron
 */
public class ActorEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(ActorEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the ActorEditorPanel.properties file to set the label
	 * of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the ActorEditorPanel.properties file to set the label
	 * of the actor type field. If the property is undefined "Actor Type" is
	 * used.
	 */
	public static final String PROP_LABEL_STORY_TYPE = "ActorType.Label";

	/**
	 * The name to use in the ActorEditorPanel.properties file to set the label
	 * of the text field. If the property is undefined "Text" is used.
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
	public ActorEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(ActorEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public ActorEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, Actor.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing Actor the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Actor: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - Actor name<br>
	 * {1} - project/domain name<br>
	 * For new Actor it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Actor"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getActor() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Actor: {0}"));
			return MessageFormat.format(msgPattern, getActor().getName(), getProjectOrDomain()
					.getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Actor"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Actor actor = getActor();
		if (actor != null) {
			addInput(EditActorCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(actor.getName()));
			addInput(EditActorCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Description", new TextArea(),
					new StringDocumentEx(actor.getText()));
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), actor);
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), actor);
			addMultiRowInput("actorContainers", ActorContainersTable.PROP_LABEL_Actor_CONTAINERS,
					"Referring Entities", new ActorContainersTable(this,
							getResourceBundleHelper(getLocale())), actor);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), actor);
			copyButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_COPY_BUTTON, "Copy")));
			copyButton.addActionListener(new CopyListener(this));
			copyButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput(EditActorCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput(EditActorCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Description", new TextArea(),
					new StringDocumentEx());
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), null);
			addMultiRowInput("actorContainers", ActorContainersTable.PROP_LABEL_Actor_CONTAINERS,
					"Referring Entities", new ActorContainersTable(this,
							getResourceBundleHelper(getLocale())), null);
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
					updateListener, this);
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
			EditActorCommand command = getProjectCommandFactory().newEditActorCommand();
			command.setActor(getActor());
			command.setActorContainer(getActorContainer());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditActorCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditActorCommand.FIELD_TEXT, String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener, this);
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getActor()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditActorCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditActorCommand.FIELD_TEXT, String.class);
				Actor newActor = getProjectRepository().get(getActor());

				setTargetObject(newActor);
				if (!newName.equals(newActor.getName()) || !newText.equals(newActor.getText())) {
					setGeneralMessage("The actor was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newActor.getName())) {
						setValidationMessage(EditActorCommand.FIELD_NAME, "Your input '" + newName
								+ "'");
						setInputValue(EditActorCommand.FIELD_NAME, newActor.getName());
					}
					if (!newText.equals(newActor.getText())) {
						setValidationMessage(EditActorCommand.FIELD_TEXT, "Your input '" + newText
								+ "'");
						setInputValue(EditActorCommand.FIELD_TEXT, newActor.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, newActor));
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
			log.error("could not save the actor: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteActorCommand deleteActorCommand = getProjectCommandFactory()
					.newDeleteActorCommand();
			deleteActorCommand.setEditedBy(getCurrentUser());
			deleteActorCommand.setActor(getActor());
			deleteActorCommand = getCommandHandler().execute(deleteActorCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getActor()));
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

	private ActorContainer getActorContainer() {
		if (getTargetObject() instanceof ActorContainer) {
			return (ActorContainer) getTargetObject();
		}
		return null;
	}

	private Actor getActor() {
		if (getTargetObject() instanceof Actor) {
			return (Actor) getTargetObject();
		}
		return null;
	}

	private static class CopyListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ActorEditorPanel panel;

		private CopyListener(ActorEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				CopyActorCommand copyActorCommand = panel.getProjectCommandFactory()
						.newCopyActorCommand();
				copyActorCommand.setEditedBy(panel.getCurrentUser());
				copyActorCommand.setOriginalActor(panel.getActor());
				copyActorCommand = panel.getCommandHandler().execute(copyActorCommand);
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, copyActorCommand.getNewActor()));
				panel.getEventDispatcher().dispatchEvent(
						new OpenPanelEvent(this, PanelActionType.Editor, copyActorCommand
								.getNewActor(), Actor.class, null));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not copy entity: " + e);
			}
		}
	}

	// TODO: it may be better to have different standardized update listeners
	// for different types of updated or deleted objects associated with the
	// input controls like an annotation table or referers.
	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final ActorEditorPanel panel;

		private UpdateListener(ActorEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Actor currentActor = panel.getActor();
			if ((e instanceof UpdateEntityEvent) && (currentActor != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Actor updatedActor = null;
				if (event.getObject() instanceof Actor) {
					updatedActor = (Actor) event.getObject();
					if ((event instanceof DeletedEntityEvent) && currentActor.equals(updatedActor)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, currentActor));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					Goal updatedGoal = (Goal) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (currentActor.getGoals().contains(updatedGoal)) {
							currentActor.getGoals().remove(updatedGoal);
						}
						updatedActor = currentActor;
					} else if (updatedGoal.getReferers().contains(currentActor)) {
						for (GoalContainer gc : updatedGoal.getReferers()) {
							if (gc.equals(currentActor)) {
								updatedActor = (Actor) gc;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof UseCase) {
					// a use case has one-to-many and many-to-many references to
					// actors
					updatedActor = currentActor;
					if (event instanceof DeletedEntityEvent) {
						updatedActor.getReferers().remove(event.getObject());
					} else {
						UseCase useCase = (UseCase) event.getObject();
						if (useCase.getPrimaryActor().equals(updatedActor)) {
							updatedActor = useCase.getPrimaryActor();
						} else if (useCase.getActors().contains(updatedActor)) {
							for (Actor actor : useCase.getActors()) {
								if (actor.equals(updatedActor)) {
									updatedActor = actor;
									break;
								}
							}
						} else if (updatedActor.getReferers().contains(useCase)) {
							updatedActor.getReferers().remove(useCase);
						}
					}
				} else if (event.getObject() instanceof ActorContainer) {
					updatedActor = currentActor;
					if (updatedActor.getReferers().contains(event.getObject())) {
						if (event instanceof DeletedEntityEvent) {
							updatedActor.getReferers().remove(event.getObject());
						} else {
							ActorContainer actorContainer = (ActorContainer) event.getObject();
							for (Actor actor : actorContainer.getActors()) {
								if (currentActor.equals(actor)) {
									updatedActor = actor;
									break;
								}
							}
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (currentActor.getAnnotations().contains(updatedAnnotation)) {
							currentActor.getAnnotations().remove(updatedAnnotation);
						}
					} else if (updatedAnnotation.getAnnotatables().contains(currentActor)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(currentActor)) {
								updatedActor = (Actor) annotatable;
								break;
							}
						}
					}
					updatedActor = currentActor;
				}
				if ((updatedActor != null) && updatedActor.equals(currentActor)) {
					String editedName = panel.getInputValue(EditActorCommand.FIELD_NAME,
							String.class);
					if (!editedName.equals(updatedActor.getName())) {
						panel.setValidationMessage(EditActorCommand.FIELD_NAME,
								"The name has changed.");
					}
					panel.setInputValue(EditActorCommand.FIELD_NAME, updatedActor.getName());

					String editedText = panel.getInputValue(EditActorCommand.FIELD_TEXT,
							String.class);
					if (!editedText.equals(updatedActor.getText())) {
						panel.setValidationMessage(EditActorCommand.FIELD_TEXT,
								"The text has changed.");
					}
					panel.setInputValue(EditActorCommand.FIELD_TEXT, updatedActor.getText());
					panel.setInputValue("glossaryTerms", updatedActor);
					panel.setInputValue("goals", updatedActor);
					panel.setInputValue("actorContainers", updatedActor);
					panel.setInputValue("annotations", updatedActor);
					panel.setTargetObject(updatedActor);
				}
			}
		}
	}
}
