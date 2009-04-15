/*
 * $Id: StoryEditorPanel.java,v 1.19 2009/03/23 11:02:55 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.ActorContainer;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.GoalContainer;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.StoryType;
import edu.harvard.fas.rregan.requel.project.command.CopyStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.EditStoryCommand;
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
public class StoryEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(StoryEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the StoryEditorPanel.properties file to set the label
	 * of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the StoryEditorPanel.properties file to set the label
	 * of the story type field. If the property is undefined "Story Type" is
	 * used.
	 */
	public static final String PROP_LABEL_STORY_TYPE = "StoryType.Label";

	/**
	 * The name to use in the StoryEditorPanel.properties file to set the label
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
	public StoryEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(StoryEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public StoryEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, Story.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing Story the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "Story: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - Story name<br>
	 * {1} - project/domain name<br>
	 * For new Story it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New Story"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getStory() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "Story: {0}"));
			return MessageFormat.format(msgPattern, getStory().getName(), getProjectOrDomain()
					.getName());
		} else {
			String msg = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE, "New Story"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		Story story = getStory();
		if (story != null) {
			addInput(EditStoryCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(story.getName()));
			addInput("storyType", PROP_LABEL_STORY_TYPE, "Type", new SelectField(),
					new CombinedListModel(getStoryTypeNames(), story.getStoryType().toString(),
							true));
			addInput(EditStoryCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx(story.getText()));
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), story);
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), story);
			addMultiRowInput("actors", ActorsTable.PROP_LABEL_ACTORS, "Actors", new ActorsTable(
					this, getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), story);
			addMultiRowInput("storyContainers", StoryContainersTable.PROP_LABEL_STORY_CONTAINERS,
					"Referring Entities", new StoryContainersTable(this,
							getResourceBundleHelper(getLocale())), story);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), story);
			copyButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_COPY_BUTTON, "Copy")));
			copyButton.addActionListener(new CopyListener(this));
			copyButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput(EditStoryCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput("storyType", PROP_LABEL_STORY_TYPE, "Type", new SelectField(),
					new CombinedListModel(getStoryTypeNames(), "", true));
			addInput(EditStoryCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx());
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), story);
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addMultiRowInput("actors", ActorsTable.PROP_LABEL_ACTORS, "Actors", new ActorsTable(
					this, getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addMultiRowInput("storyContainers", StoryContainersTable.PROP_LABEL_STORY_CONTAINERS,
					"Referring Entities", new StoryContainersTable(this,
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
			EditStoryCommand command = getProjectCommandFactory().newEditStoryCommand();
			command.setStory(getStory());
			command.setStoryContainer(getStoryContainer());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditStoryCommand.FIELD_NAME, String.class));

			command.setText(getInputValue(EditStoryCommand.FIELD_TEXT, String.class));
			command.setStoryTypeName(getInputValue("storyType", String.class));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
				// TODO: remove other listeners?
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getStory()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditStoryCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditStoryCommand.FIELD_TEXT, String.class);
				Story newStory = getProjectRepository().get(getStory());

				setTargetObject(newStory);
				if (!newName.equals(newStory.getName()) || !newText.equals(newStory.getText())) {
					setGeneralMessage("The story was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newStory.getName())) {
						setValidationMessage(EditStoryCommand.FIELD_NAME, "Your input '" + newName
								+ "'");
						setInputValue(EditStoryCommand.FIELD_NAME, newStory.getName());
					}
					if (!newText.equals(newStory.getText())) {
						setValidationMessage(EditStoryCommand.FIELD_TEXT, "Your input '" + newText
								+ "'");
						setInputValue(EditStoryCommand.FIELD_TEXT, newStory.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, newStory));
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
			log.error("could not save the story: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteStoryCommand deleteStoryCommand = getProjectCommandFactory()
					.newDeleteStoryCommand();
			deleteStoryCommand.setEditedBy(getCurrentUser());
			deleteStoryCommand.setStory(getStory());
			deleteStoryCommand = getCommandHandler().execute(deleteStoryCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getStory()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Set<String> getStoryTypeNames() {
		Set<String> storyTypeNames = new TreeSet<String>();
		for (StoryType storyType : StoryType.values()) {
			storyTypeNames.add(storyType.toString());
		}
		return storyTypeNames;
	}

	private ProjectOrDomain getProjectOrDomain() {
		if (getTargetObject() instanceof ProjectOrDomain) {
			return (ProjectOrDomain) getTargetObject();
		} else if (getTargetObject() instanceof ProjectOrDomainEntity) {
			return ((ProjectOrDomainEntity) getTargetObject()).getProjectOrDomain();
		}
		return null;
	}

	private StoryContainer getStoryContainer() {
		if (getTargetObject() instanceof StoryContainer) {
			return (StoryContainer) getTargetObject();
		}
		return null;
	}

	private Story getStory() {
		if (getTargetObject() instanceof Story) {
			return (Story) getTargetObject();
		}
		return null;
	}

	private static class CopyListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final StoryEditorPanel panel;

		private CopyListener(StoryEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				CopyStoryCommand copyStoryCommand = panel.getProjectCommandFactory()
						.newCopyStoryCommand();
				copyStoryCommand.setEditedBy(panel.getCurrentUser());
				copyStoryCommand.setOriginalStory(panel.getStory());
				copyStoryCommand = panel.getCommandHandler().execute(copyStoryCommand);
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, copyStoryCommand.getNewStory()));
				panel.getEventDispatcher().dispatchEvent(
						new OpenPanelEvent(this, PanelActionType.Editor, copyStoryCommand
								.getNewStory(), Story.class, null));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not copy entity: " + e);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final StoryEditorPanel panel;

		private UpdateListener(StoryEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			Story existingStory = panel.getStory();
			if ((e instanceof UpdateEntityEvent) && (existingStory != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				Story updatedStory = null;
				if (event.getObject() instanceof Story) {
					updatedStory = (Story) event.getObject();
					if ((event instanceof DeletedEntityEvent) && existingStory.equals(updatedStory)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingStory));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					Goal updatedGoal = (Goal) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingStory.getReferers().contains(updatedGoal)) {
							existingStory.getReferers().remove(updatedGoal);
						}
						updatedStory = existingStory;
					} else if (updatedGoal.getReferers().contains(existingStory)) {
						for (GoalContainer gc : updatedGoal.getReferers()) {
							if (gc.equals(existingStory)) {
								updatedStory = (Story) gc;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Actor) {
					Actor updatedActor = (Actor) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingStory.getReferers().contains(updatedActor)) {
							existingStory.getReferers().remove(updatedActor);
						}
						updatedStory = existingStory;
					} else if (updatedActor.getReferers().contains(existingStory)) {
						for (ActorContainer ac : updatedActor.getReferers()) {
							if (ac.equals(existingStory)) {
								updatedStory = (Story) ac;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingStory.getAnnotations().contains(updatedAnnotation)) {
							existingStory.getAnnotations().remove(updatedAnnotation);
						}
						updatedStory = existingStory;
					} else if (updatedAnnotation.getAnnotatables().contains(existingStory)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingStory)) {
								updatedStory = (Story) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedStory != null) && updatedStory.equals(existingStory)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditStoryCommand.FIELD_NAME, updatedStory.getName());
					panel.setInputValue("storyType", updatedStory.getStoryType().toString());
					panel.setInputValue(EditStoryCommand.FIELD_TEXT, updatedStory.getText());
					panel.setInputValue("glossaryTerms", updatedStory);
					panel.setInputValue("goals", updatedStory);
					panel.setInputValue("actors", updatedStory);
					panel.setInputValue("storyContainers", updatedStory);
					panel.setInputValue("annotations", updatedStory);
					panel.setTargetObject(updatedStory);
				}
			}
		}
	}
}
