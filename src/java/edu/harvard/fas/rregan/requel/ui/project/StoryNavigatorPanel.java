/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.UserStakeholder;
import edu.harvard.fas.rregan.requel.project.impl.AbstractProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.user.User;
import net.sf.echopm.navigation.NavigatorButton;
import net.sf.echopm.navigation.WorkflowDisposition;
import net.sf.echopm.navigation.event.ClosePanelEvent;
import net.sf.echopm.navigation.event.DeletedEntityEvent;
import net.sf.echopm.navigation.event.NavigationEvent;
import net.sf.echopm.navigation.event.OpenPanelEvent;
import net.sf.echopm.navigation.event.UpdateEntityEvent;
import net.sf.echopm.navigation.table.NavigatorTableCellValueFactory;
import net.sf.echopm.navigation.table.NavigatorTableColumnConfig;
import net.sf.echopm.navigation.table.NavigatorTableConfig;
import net.sf.echopm.navigation.table.NavigatorTableModel;
import net.sf.echopm.panel.NavigatorTablePanel;
import net.sf.echopm.panel.PanelActionType;

/**
 * TODO: integrate the StoriesTable with this.
 * 
 * @author ron
 */
public class StoryNavigatorPanel extends NavigatorTablePanel {
	private static final Logger log = Logger.getLogger(StoryNavigatorPanel.class);
	static final long serialVersionUID = 0;

	private UpdateListener updateListener;
	private ProjectOrDomain pod;

	/**
	 * Property name to use in the StoryNavigatorPanel.properties to set the
	 * label on the new Story button.
	 */
	public static final String PROP_NEW_STORY_BUTTON_LABEL = "NewStoryButton.Label";

	/**
	 * Property name to use in the StoryNavigatorPanel.properties to set the
	 * label on the edit Story button in each row of the table.
	 */
	public static final String PROP_EDIT_STORY_BUTTON_LABEL = "EditStoryButton.Label";

	/**
	 * Property name to use in the StoryNavigatorPanel.properties to set the
	 * label on the view Story button in each row of the table when the user
	 * doesn't have edit permission.
	 */
	public static final String PROP_VIEW_STORY_BUTTON_LABEL = "ViewStoryButton.Label";

	/**
	 */
	public StoryNavigatorPanel() {
		super(StoryNavigatorPanel.class.getName(), Project.class,
				ProjectManagementPanelNames.PROJECT_STORIES_NAVIGATOR_PANEL_NAME);
		NavigatorTableConfig tableConfig = new NavigatorTableConfig();

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story story = (Story) model.getBackingObject(row);
						String buttonLabel = null;
						if (isReadOnlyMode()) {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_VIEW_STORY_BUTTON_LABEL, "View");
						} else {
							buttonLabel = getResourceBundleHelper(getLocale()).getString(
									PROP_EDIT_STORY_BUTTON_LABEL, "Edit");
						}
						NavigationEvent openEditorEvent = new OpenPanelEvent(this,
								PanelActionType.Editor, story, Story.class, null,
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

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Name",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story story = (Story) model.getBackingObject(row);
						return story.getName();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Type",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						Story Story = (Story) model.getBackingObject(row);
						return Story.getStoryType().toString();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Created By",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						AbstractProjectOrDomainEntity entity = (AbstractProjectOrDomainEntity) model
								.getBackingObject(row);
						return entity.getCreatedBy().getUsername();
					}
				}));

		tableConfig.addColumnConfig(new NavigatorTableColumnConfig("Date Created",
				new NavigatorTableCellValueFactory() {
					@Override
					public Object getValueAt(NavigatorTableModel model, int column, int row) {
						AbstractProjectOrDomainEntity entity = (AbstractProjectOrDomainEntity) model
								.getBackingObject(row);
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						return format.format(entity.getDateCreated());
					}
				}));

		setTableConfig(tableConfig);
	}

	/**
	 * Create a title for panel with dynamic information from the project or
	 * domain, by default the pattern is "Storys: {0}"<br>
	 * Valid variables are:<br>
	 * {0} - project/domain name<br>
	 * 
	 * @see Panel.PROP_PANEL_TITLE
	 * @see net.sf.echopm.panel.AbstractPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		String name = "";
		String msgPattern = getResourceBundleHelper(getLocale()).getString(PROP_PANEL_TITLE,
				"Stories: {0}");
		ProjectOrDomain pod = getProjectOrDomain();
		if (pod != null) {
			name = pod.getName();
		}
		return MessageFormat.format(msgPattern, name);
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
	public void setup() {
		super.setup();

		Row buttonsWrapper = new Row();
		buttonsWrapper.setInsets(new Insets(10, 5));
		buttonsWrapper.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));

		String closeButtonLabel = getResourceBundleHelper(getLocale()).getString(
				PROP_CANCEL_BUTTON_LABEL, "Close");
		NavigationEvent closeEvent = new ClosePanelEvent(this, this);
		NavigatorButton closeButton = new NavigatorButton(closeButtonLabel, getEventDispatcher(),
				closeEvent);
		closeButton.setStyleName(STYLE_NAME_DEFAULT);
		buttonsWrapper.add(closeButton);

		if (!isReadOnlyMode()) {
			String newStoryButtonLabel = getResourceBundleHelper(getLocale()).getString(
					PROP_NEW_STORY_BUTTON_LABEL, "Add");
			NavigationEvent openStoryEditor = new OpenPanelEvent(this, PanelActionType.Editor,
					getProjectOrDomain(), Story.class, null, WorkflowDisposition.NewFlow);
			NavigatorButton newStoryButton = new NavigatorButton(newStoryButtonLabel,
					getEventDispatcher(), openStoryEditor);
			newStoryButton.setStyleName(STYLE_NAME_DEFAULT);
			buttonsWrapper.add(newStoryButton);
		}

		add(buttonsWrapper);

		if (updateListener != null) {
			getEventDispatcher().removeEventTypeActionListener(UpdateEntityEvent.class,
					updateListener);
		}
		updateListener = new UpdateListener(this);
		getEventDispatcher().addEventTypeActionListener(UpdateEntityEvent.class, updateListener);
	}

	protected boolean isReadOnlyMode() {
		User user = (User) getApp().getUser();
		if (getProjectOrDomain() instanceof Project) {
			Project project = (Project) getProjectOrDomain();
			UserStakeholder stakeholder = project.getUserStakeholder(user);
			if (stakeholder != null) {
				return !stakeholder.hasPermission(Story.class, StakeholderPermissionType.Edit);
			}
		}
		return true;
	}

	@Override
	public void setTargetObject(Object targetObject) {
		if (targetObject instanceof ProjectOrDomain) {
			pod = (ProjectOrDomain) targetObject;
			super.setTargetObject(((ProjectOrDomain) targetObject).getStories());
		} else {
			log.error("unexpected target object " + targetObject);
		}
	}

	protected ProjectOrDomain getProjectOrDomain() {
		return pod;
	}

	private static class UpdateListener implements ActionListener {
		static final long serialVersionUID = 0L;

		private final StoryNavigatorPanel panel;

		private UpdateListener(StoryNavigatorPanel panel) {
			this.panel = panel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e instanceof UpdateEntityEvent) {
				UpdateEntityEvent event = (UpdateEntityEvent) e;
				ProjectOrDomain updatedPod = null;
				if (event.getObject() instanceof ProjectOrDomain) {
					updatedPod = (ProjectOrDomain) event.getObject();
				} else if (event.getObject() instanceof ProjectOrDomainEntity) {
					ProjectOrDomainEntity updatedEntity = (ProjectOrDomainEntity) event.getObject();
					updatedPod = updatedEntity.getProjectOrDomain();
				} else if (event.getObject() instanceof Annotation) {
					if (!(event instanceof DeletedEntityEvent)) {
						Annotation updatedAnnotation = (Annotation) event.getObject();
						for (Annotatable annotatable : updatedAnnotation.getAnnotatables()) {
							if ((annotatable instanceof ProjectOrDomain)
									&& annotatable.equals(panel.getProjectOrDomain())) {
								updatedPod = (ProjectOrDomain) annotatable;
								break;
							} else if ((annotatable instanceof ProjectOrDomainEntity)) {
								ProjectOrDomainEntity entity = (ProjectOrDomainEntity) annotatable;
								if (entity.getProjectOrDomain().equals(panel.getProjectOrDomain())) {
									updatedPod = entity.getProjectOrDomain();
									break;
								}
							}
						}
					}
				}
				if (panel.getProjectOrDomain().equals(updatedPod)) {
					panel.setTargetObject(updatedPod);
				}
			}
		}
	}
}
