/*
 * $Id: UseCaseEditorPanel.java,v 1.27 2009/03/05 08:50:46 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import java.text.MessageFormat;
import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Button;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import echopointng.ComboBox;
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
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.StoryContainer;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.command.CopyUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.DeleteUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.EditStoryCommand;
import edu.harvard.fas.rregan.requel.project.command.EditUseCaseCommand;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.ui.annotation.AnnotationsTable;
import edu.harvard.fas.rregan.uiframework.navigation.event.DeletedEntityEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.UpdateEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedTextListModel;

/**
 * @author ron
 */
public class UseCaseEditorPanel extends AbstractRequelProjectEditorPanel {
	private static final Logger log = Logger.getLogger(UseCaseEditorPanel.class);

	static final long serialVersionUID = 0L;

	/**
	 * The name to use in the UseCaseEditorPanel.properties file to set the
	 * label of the name field. If the property is undefined "Name" is used.
	 */
	public static final String PROP_LABEL_NAME = "Name.Label";

	/**
	 * The name to use in the UseCaseEditorPanel.properties file to set the
	 * label of the text field. If the property is undefined "Text" is used.
	 */
	public static final String PROP_LABEL_TEXT = "Text.Label";

	/**
	 * The name to use in the UseCaseEditorPanel.properties file to set the
	 * label of the usecase type field. If the property is undefined "UseCase
	 * Type" is used.
	 */
	public static final String PROP_LABEL_STORY_TYPE = "UseCaseType.Label";

	/**
	 * The name to use in the UseCaseEditorPanel.properties file to set the
	 * label of the primary actor field. If the property is undefined "Primary
	 * Actor" is used.
	 */
	public static final String PROP_LABEL_PRIMARY_ACTOR = "PrimaryActor.Label";

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
	public UseCaseEditorPanel(CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		this(UseCaseEditorPanel.class.getName(), commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * @param resourceBundleName
	 * @param commandHandler
	 * @param projectCommandFactory
	 * @param projectRepository
	 */
	public UseCaseEditorPanel(String resourceBundleName, CommandHandler commandHandler,
			ProjectCommandFactory projectCommandFactory, ProjectRepository projectRepository) {
		super(resourceBundleName, UseCase.class, commandHandler, projectCommandFactory,
				projectRepository);
	}

	/**
	 * If the editor is editing an existing UseCase the title specified in the
	 * properties file as PROP_EXISTING_OBJECT_PANEL_TITLE If that property is
	 * not set it then tries the standard PROP_PANEL_TITLE and if that does not
	 * exist it defaults to:<br>
	 * "UseCase: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - UseCase name<br>
	 * {1} - project/domain name<br>
	 * For new UseCase it first tries PROP_NEW_OBJECT_PANEL_TITLE, then
	 * PROP_PANEL_TITLE and finally defaults to:<br>
	 * "New UseCase"<br>
	 * 
	 * @see AbstractEditorPanel.PROP_EXISTING_OBJECT_PANEL_TITLE
	 * @see AbstractEditorPanel.PROP_NEW_OBJECT_PANEL_TITLE
	 * @see Panel.PROP_PANEL_TITLE
	 * @see edu.harvard.fas.rregan.uiframework.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		if (getUseCase() != null) {
			String msgPattern = getResourceBundleHelper(getLocale()).getString(
					PROP_EXISTING_OBJECT_PANEL_TITLE,
					getResourceBundleHelper(getLocale())
							.getString(PROP_PANEL_TITLE, "UseCase: {0}"));
			return MessageFormat.format(msgPattern, getUseCase().getName(), getProjectOrDomain()
					.getName());
		} else {
			String msg = getResourceBundleHelper(getLocale())
					.getString(
							PROP_NEW_OBJECT_PANEL_TITLE,
							getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
									"New UseCase"));
			return msg;
		}
	}

	@Override
	public void setup() {
		super.setup();
		UseCase usecase = getUseCase();
		if (usecase != null) {
			addInput(EditUseCaseCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx(usecase.getName()));
			addInput(EditUseCaseCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx(usecase.getText()));
			addInput("primaryActor", PROP_LABEL_PRIMARY_ACTOR, "Primary Actor", new ComboBox(),
					new CombinedTextListModel(getActorNames(), usecase.getPrimaryActor().getName()));
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), usecase);
			addMultiRowInput("scenarioSteps", ScenarioStepsEditor.PROP_LABEL_SCENARIO_STEPS,
					"Scenario", new ScenarioStepsEditor(getProjectOrDomain(), this,
							getResourceBundleHelper(getLocale())), usecase.getScenario());
			setInputValue("scenarioSteps", usecase.getScenario());
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), usecase);
			addMultiRowInput("actors", ActorsTable.PROP_LABEL_ACTORS, "Actors", new ActorsTable(
					this, getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), usecase);
			addMultiRowInput("stories", StoriesTable.PROP_LABEL_STORIES, "Stories",
					new StoriesTable(this, getResourceBundleHelper(getLocale()),
							getProjectCommandFactory(), getCommandHandler()), usecase);
			addMultiRowInput("annotations", AnnotationsTable.PROP_LABEL_ANNOTATIONS, "Annotations",
					new AnnotationsTable(this, getResourceBundleHelper(getLocale())), usecase);
			copyButton = addActionButton(new Button(getResourceBundleHelper(getLocale()).getString(
					PROP_LABEL_COPY_BUTTON, "Copy")));
			copyButton.addActionListener(new CopyListener(this));
			copyButton.setEnabled(!isReadOnlyMode());
		} else {
			addInput(EditUseCaseCommand.FIELD_NAME, PROP_LABEL_NAME, "Name", new TextField(),
					new StringDocumentEx());
			addInput(EditUseCaseCommand.FIELD_TEXT, PROP_LABEL_TEXT, "Text", new TextArea(),
					new StringDocumentEx());
			addInput("primaryActor", PROP_LABEL_PRIMARY_ACTOR, "Primary Actor", new ComboBox(),
					new CombinedTextListModel(getActorNames(), ""));
			addMultiRowInput("glossaryTerms", GlossaryTermsTable.PROP_LABEL_GLOSSARY_TERM,
					"Glossary Terms", new GlossaryTermsTable(this,
							getResourceBundleHelper(getLocale())), usecase);
			addMultiRowInput("scenarioSteps", ScenarioStepsEditor.PROP_LABEL_SCENARIO_STEPS,
					"Scenario", new ScenarioStepsEditor(getProjectOrDomain(), this,
							getResourceBundleHelper(getLocale())), null);
			addMultiRowInput("goals", GoalsTable.PROP_LABEL_GOALS, "Goals", new GoalsTable(this,
					getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addMultiRowInput("actors", ActorsTable.PROP_LABEL_ACTORS, "Actors", new ActorsTable(
					this, getResourceBundleHelper(getLocale()), getProjectCommandFactory(),
					getCommandHandler()), null);
			addMultiRowInput("stories", StoriesTable.PROP_LABEL_STORIES, "Stories",
					new StoriesTable(this, getResourceBundleHelper(getLocale()),
							getProjectCommandFactory(), getCommandHandler()), null);
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
		// TODO: should this call dispose() or will dispose be called already
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

			EditUseCaseCommand command = getProjectCommandFactory().newEditUseCaseCommand();
			command.setUseCase(getUseCase());
			command.setProjectOrDomain(getProjectOrDomain());
			command.setEditedBy(getCurrentUser());
			command.setName(getInputValue(EditUseCaseCommand.FIELD_NAME, String.class));
			command.setText(getInputValue(EditUseCaseCommand.FIELD_TEXT, String.class));
			command.setPrimaryActorName(getInputValue("primaryActor", String.class));
			command.setStepCommands(scenarioEditor.generateStepEditCommands(
					getProjectCommandFactory(), getProjectOrDomain(), rootNode));
			command = getCommandHandler().execute(command);
			setValid(true);
			if (updateListener != null) {
				getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
						updateListener);
				// TODO: remove other listeners?
			}
			getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, command.getUseCase()));
		} catch (EntityException e) {
			if (e.isStaleEntity()) {
				// TODO: compare the original values before the user edited
				// to the current revisions values and if they are the same
				// then update the new revision with the user's changes and
				// continue, otherwise show the new changed value vs. the users
				// new values.
				String newName = getInputValue(EditUseCaseCommand.FIELD_NAME, String.class);
				String newText = getInputValue(EditStoryCommand.FIELD_TEXT, String.class);
				UseCase newUseCase = getProjectRepository().get(getUseCase());

				setTargetObject(newUseCase);
				if (!newName.equals(newUseCase.getName())) {
					setGeneralMessage("The usecase was changed by another user and the value conflicts with your input.");
					if (!newName.equals(newUseCase.getName())) {
						setValidationMessage(EditUseCaseCommand.FIELD_NAME, "Your input '"
								+ newName + "'");
						setInputValue(EditUseCaseCommand.FIELD_NAME, newUseCase.getName());
					}
					if (!newText.equals(newUseCase.getText())) {
						setValidationMessage(EditUseCaseCommand.FIELD_TEXT, "Your input '"
								+ newText + "'");
						setInputValue(EditUseCaseCommand.FIELD_TEXT, newUseCase.getText());
					}
				} else {
					getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, newUseCase));
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
			log.error("could not save the usecase: " + e, e);
			setGeneralMessage("Could not save: " + e);
		}
	}

	@Override
	public void delete() {
		try {
			DeleteUseCaseCommand deleteUseCaseCommand = getProjectCommandFactory()
					.newDeleteUseCaseCommand();
			deleteUseCaseCommand.setEditedBy(getCurrentUser());
			deleteUseCaseCommand.setUseCase(getUseCase());
			deleteUseCaseCommand = getCommandHandler().execute(deleteUseCaseCommand);
			deleted = true;
			getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, getUseCase()));
		} catch (Exception e) {
			setGeneralMessage("Could not delete entity: " + e);
		}
	}

	private Set<String> getActorNames() {
		Set<String> actorNames = new TreeSet<String>();
		for (Actor actor : getProjectOrDomain().getActors()) {
			actorNames.add(actor.getName());
		}
		return actorNames;
	}

	private ProjectOrDomain getProjectOrDomain() {
		if (getTargetObject() instanceof ProjectOrDomain) {
			return (ProjectOrDomain) getTargetObject();
		} else if (getTargetObject() instanceof ProjectOrDomainEntity) {
			return ((ProjectOrDomainEntity) getTargetObject()).getProjectOrDomain();
		}
		return null;
	}

	private UseCase getUseCase() {
		if (getTargetObject() instanceof UseCase) {
			return (UseCase) getTargetObject();
		}
		return null;
	}

	private static class CopyListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final UseCaseEditorPanel panel;

		private CopyListener(UseCaseEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			try {
				CopyUseCaseCommand copyUseCaseCommand = panel.getProjectCommandFactory()
						.newCopyUseCaseCommand();
				copyUseCaseCommand.setEditedBy(panel.getCurrentUser());
				copyUseCaseCommand.setOriginalUseCase(panel.getUseCase());
				copyUseCaseCommand = panel.getCommandHandler().execute(copyUseCaseCommand);
				panel.getEventDispatcher().dispatchEvent(
						new UpdateEntityEvent(this, null, copyUseCaseCommand.getNewUseCase()));
				panel.getEventDispatcher().dispatchEvent(
						new OpenPanelEvent(this, PanelActionType.Editor, copyUseCaseCommand
								.getNewUseCase(), UseCase.class, null));
			} catch (Exception e) {
				panel.setGeneralMessage("Could not copy entity: " + e);
			}
		}
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final UseCaseEditorPanel panel;

		private UpdateListener(UseCaseEditorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (panel.deleted) {
				return;
			}
			UseCase existingUseCase = panel.getUseCase();
			if ((e instanceof UpdateEntityEvent) && (existingUseCase != null)) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				UseCase updatedUseCase = null;
				if (event.getObject() instanceof UseCase) {
					updatedUseCase = (UseCase) event.getObject();
					if ((event instanceof DeletedEntityEvent)
							&& existingUseCase.equals(updatedUseCase)) {
						panel.deleted = true;
						panel.getEventDispatcher().dispatchEvent(
								new DeletedEntityEvent(this, panel, existingUseCase));
						return;
					}
				} else if (event.getObject() instanceof Goal) {
					Goal updatedGoal = (Goal) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingUseCase.getGoals().contains(updatedGoal)) {
							existingUseCase.getGoals().remove(updatedGoal);
						}
						updatedUseCase = existingUseCase;
					} else if (updatedGoal.getReferers().contains(existingUseCase)) {
						for (GoalContainer gc : updatedGoal.getReferers()) {
							if (gc.equals(existingUseCase)) {
								updatedUseCase = (UseCase) gc;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Actor) {
					Actor updatedActor = (Actor) event.getObject();
					panel.setInputModel("primaryActor", new CombinedListModel(
							panel.getActorNames(), panel
									.getInputValue("primaryActor", String.class), true));
					if (event instanceof DeletedEntityEvent) {
						if (existingUseCase.getActors().contains(updatedActor)) {
							existingUseCase.getActors().remove(updatedActor);
						}
						updatedUseCase = existingUseCase;
					} else if (updatedActor.getReferers().contains(existingUseCase)) {
						for (ActorContainer ac : updatedActor.getReferers()) {
							if (ac.equals(existingUseCase)) {
								updatedUseCase = (UseCase) ac;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Story) {
					Story updatedStory = (Story) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingUseCase.getStories().contains(updatedStory)) {
							existingUseCase.getStories().remove(updatedStory);
						}
						updatedUseCase = existingUseCase;
					} else if (updatedStory.getReferers().contains(existingUseCase)) {
						for (StoryContainer ac : updatedStory.getReferers()) {
							if (ac.equals(existingUseCase)) {
								updatedUseCase = (UseCase) ac;
								break;
							}
						}
					}
				} else if (event.getObject() instanceof Annotation) {
					Annotation updatedAnnotation = (Annotation) event.getObject();
					if (event instanceof DeletedEntityEvent) {
						if (existingUseCase.getAnnotations().contains(updatedAnnotation)) {
							existingUseCase.getAnnotations().remove(updatedAnnotation);
						}
						updatedUseCase = existingUseCase;
					} else if (updatedAnnotation.getAnnotatables().contains(existingUseCase)) {
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if (annotatable.equals(existingUseCase)) {
								updatedUseCase = (UseCase) annotatable;
								break;
							}
						}
					}
				}
				if ((updatedUseCase != null) && updatedUseCase.equals(existingUseCase)) {
					// TODO: check the input fields to see if the user has made
					// a change before reseting the object and updating the
					// input fields.
					panel.setInputValue(EditUseCaseCommand.FIELD_NAME, updatedUseCase.getName());
					panel.setInputValue("primaryActor", updatedUseCase.getPrimaryActor().getName());
					panel.setInputValue(EditUseCaseCommand.FIELD_TEXT, updatedUseCase.getText());
					panel.setInputValue("glossaryTerms", updatedUseCase);
					panel.setInputValue("goals", updatedUseCase);
					panel.setInputValue("actors", updatedUseCase);
					panel.setInputValue("stories", updatedUseCase);
					panel.setInputValue("annotations", updatedUseCase);
					panel.setTargetObject(updatedUseCase);
				}
			}
		}
	}
}
