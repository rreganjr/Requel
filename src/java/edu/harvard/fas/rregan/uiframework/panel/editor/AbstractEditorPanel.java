/*
 * $Id: AbstractEditorPanel.java,v 1.40 2009/02/21 10:32:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.layout.GridLayoutData;

import org.apache.log4j.Logger;

import echopointng.ComboBox;
import edu.harvard.fas.rregan.uiframework.AbstractAppAwareActionListener;
import edu.harvard.fas.rregan.uiframework.MessageHandler;
import edu.harvard.fas.rregan.uiframework.navigation.event.ClosePanelEvent;
import edu.harvard.fas.rregan.uiframework.panel.AbstractPanel;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;

/**
 * @author ron
 */
public abstract class AbstractEditorPanel extends AbstractPanel implements EditMode, MessageHandler {
	private static final Logger log = Logger.getLogger(AbstractEditorPanel.class);
	static final long serialVersionUID = 0L;

	/**
	 * The property to set in the resource bundle for the panel's title if the
	 * entity being edited already exists. If this property does not exist the
	 * Panel.Title is used.
	 */
	public static final String PROP_EXISTING_OBJECT_PANEL_TITLE = "EditorPanel.ExistingObject.Title";

	/**
	 * The property to set in the resource bundle for the panel's title if the
	 * entity being edited is new. If this property does not exist the
	 * Panel.Title is used.
	 */
	public static final String PROP_NEW_OBJECT_PANEL_TITLE = "EditorPanel.NewObject.Title";

	/**
	 * The style name to use in the Echo2 stylesheet to control the grid layout
	 * of the editor's input fields.
	 */
	public static final String STYLE_NAME_FORM_GRID = "EditorPanel.FormGrid";

	/**
	 * The style name to use in the Echo2 stylesheet for controlling the style
	 * of labels on input fields added via addInput, but not addMutliRowInput.
	 */
	public static final String STYLE_NAME_FIELD_LABEL = "EditorPanel.InputField.Label";

	/**
	 * The style name to use in the Echo2 stylesheet for controlling the style
	 * of all types of input fields. Different types of fields will be
	 * distingueshed by the type element in the stylesheet.
	 */
	public static final String STYLE_NAME_INPUT_FIELD = "EditorPanel.InputField";

	/**
	 * The style name to use in the Echo2 stylesheet for controlling the style
	 * of labels on input fields added via addMutliRowInput.
	 */
	public static final String STYLE_NAME_MULTIROW_FIELD_LABEL = "EditorPanel.MultiRowField.Label";

	/**
	 * The name to use in the panel's properties file to set the label of the
	 * cancel button. If the property is undefined "Cancel" is used.
	 */
	public static final String PROP_LABEL_CANCEL_BUTTON = "EditorPanel.CancelButton.Label";

	/**
	 * The name to use in the panel's properties file to set the label of the
	 * save button. If the property is undefined "Save" is used.
	 */
	public static final String PROP_LABEL_SAVE_BUTTON = "EditorPanel.SaveButton.Label";

	/**
	 * The name to use in the panel's properties file to set the label of the
	 * copy button. If the property is undefined "Copy" is used.
	 */
	public static final String PROP_LABEL_COPY_BUTTON = "EditorPanel.CopyButton.Label";

	/**
	 * The name to use in the panel's properties file to set the label of the
	 * delete button. If the property is undefined "Delete" is used.
	 */
	public static final String PROP_LABEL_DELETE_BUTTON = "EditorPanel.DeleteButton.Label";

	private final EditorComponents editorComponents;
	private Grid formGrid;
	private GridLayoutData formLayout;
	private GridLayoutData fullRowLayout;
	private GridLayoutData threeQuarterRowLayout;
	private Row actionButtons;
	private Button saveButton;
	private Button cancelButton;
	private Button deleteButton;
	private Label generalMessage;
	private Label titleLabel;
	private boolean valid;
	private boolean stateEdited;

	protected AbstractEditorPanel(String resourceBundleName, Class<?> supportedContentType) {
		super(resourceBundleName, PanelActionType.Editor, supportedContentType);
		editorComponents = new EditorComponents(this);
	}

	protected AbstractEditorPanel(String resourceBundleName, Class<?> supportedContentType,
			String panelName) {
		super(resourceBundleName, PanelActionType.Editor, supportedContentType, panelName);
		editorComponents = new EditorComponents(this);
	}

	@Override
	public void setup() {
		super.setup();
		fullRowLayout = new GridLayoutData();
		fullRowLayout.setColumnSpan(4);
		fullRowLayout.setAlignment(new Alignment(Alignment.CENTER, Alignment.CENTER));
		threeQuarterRowLayout = new GridLayoutData();
		threeQuarterRowLayout.setColumnSpan(3);

		Row row = null;

		// title
		if (isShowTitle()) {
			row = new Row();
			row.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
			titleLabel = new Label(getTitle());
			titleLabel.setStyleName(STYLE_NAME_PANEL_TITLE);
			row.add(titleLabel);
			add(row);
		}

		// grid
		row = new Row();
		row.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		row.setInsets(new Insets(new Extent(10)));
		formGrid = new Grid(4);
		formGrid.setStyleName(STYLE_NAME_FORM_GRID);
		row.add(formGrid);
		add(row);

		formLayout = new GridLayoutData();
		formLayout.setAlignment(new Alignment(Alignment.RIGHT, Alignment.TOP));

		// buttons
		actionButtons = new Row();
		if (isShowCancel() || isShowDelete() || isShowSave()) {
			actionButtons.setStyleName(STYLE_NAME_DEFAULT);
			actionButtons.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));

			if (isShowCancel()) {
				cancelButton = new Button(getResourceBundleHelper(getLocale()).getString(
						PROP_LABEL_CANCEL_BUTTON, "Cancel"));
				cancelButton.setStyleName(STYLE_NAME_DEFAULT);
				actionButtons.add(cancelButton);
				cancelButton.addActionListener(new AbstractAppAwareActionListener(getApp()) {
					static final long serialVersionUID = 0;

					public void actionPerformed(ActionEvent event) {
						try {
							cancel();
						} catch (Exception e) {
							// TODO: should this be propogated up?
							log.error("Unexpected exception after cancel : " + e, e);
						}
					}
				});
			}

			if (isShowSave()) {
				saveButton = new Button(getResourceBundleHelper(getLocale()).getString(
						PROP_LABEL_SAVE_BUTTON, "Save"));
				saveButton.setStyleName(STYLE_NAME_DEFAULT);
				actionButtons.add(saveButton);
				saveButton.addActionListener(new AbstractAppAwareActionListener(getApp()) {
					static final long serialVersionUID = 0;

					public void actionPerformed(ActionEvent event) {
						try {
							save();
						} catch (Exception e) {
							log.error("Unexpected exception after save: " + e, e);
							setGeneralMessage("Unexpected problem saving: " + e);
							// TODO: should this be propogated up?
						}
					}
				});
			}

			// delete button
			if (isShowDelete()) {
				deleteButton = new Button(getResourceBundleHelper(getLocale()).getString(
						PROP_LABEL_DELETE_BUTTON, "Delete"));
				deleteButton.setStyleName(STYLE_NAME_DEFAULT);
				actionButtons.add(deleteButton);
				deleteButton.addActionListener(new AbstractAppAwareActionListener(getApp()) {
					static final long serialVersionUID = 0;

					public void actionPerformed(ActionEvent event) {
						try {
							delete();
						} catch (Exception e) {
							// TODO: should this be propogated up?
							log.error("Unexpected exception after delete : " + e, e);
						}
					}
				});
			}
			add(actionButtons);
		}

		row = new Row();
		row.setAlignment(new Alignment(Alignment.CENTER, Alignment.DEFAULT));
		row.setInsets(new Insets(new Extent(10)));
		generalMessage = new Label();
		generalMessage.setStyleName(STYLE_NAME_VALIDATION_LABEL);
		row.add(generalMessage);
		add(row);
	}

	@Override
	public boolean isStateEdited() {
		return stateEdited;
	}

	@Override
	public void setStateEdited(boolean stateEdited) {
		this.stateEdited = stateEdited;
	}

	@Override
	public void setGeneralMessage(String message) {
		generalMessage.setText(message);
	}

	protected Button addActionButton(Button button) {
		return addActionButton(button, actionButtons.getComponents().length);
	}

	protected Button addActionButton(Button button, int index) {
		actionButtons.add(button, index);
		if ((button.getStyleName() == null) || "".equals(button.getStyleName().trim())) {
			button.setStyleName(STYLE_NAME_DEFAULT);
		}
		return button;
	}

	protected Button removeActionButton(Button button) {
		actionButtons.remove(button);
		return button;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/**
	 * Add an input control with a label such that the label will be in the
	 * first column and the control will be in the second.
	 * 
	 * @param <T>
	 * @param fieldName
	 * @param labelProperty
	 * @param labelDefault
	 * @param inputComponent
	 * @param model
	 * @return
	 */
	protected <T extends Component> T addInput(String fieldName, String labelProperty,
			String labelDefault, T inputComponent, Object model) {
		String labelText = getResourceBundleHelper(getLocale()).getString(labelProperty,
				labelDefault);
		Label label = new Label((labelText.endsWith(":") ? labelText : labelText + ":"));
		return addInput(fieldName, label, inputComponent, model);
	}

	/**
	 * @param <T>
	 * @param fieldName
	 * @param label
	 * @param inputComponent
	 * @param model
	 * @return
	 */
	protected <T extends Component> T addInput(String fieldName, Label label, T inputComponent,
			Object model) {
		editorComponents.addInput(fieldName, label, inputComponent, model);
		inputComponent.setStyleName(STYLE_NAME_INPUT_FIELD);
		// a hack to get a ComboBox text field to pickup the correct font from
		// the stylesheet.
		if (inputComponent instanceof ComboBox) {
			((ComboBox) inputComponent).getTextField().setStyleName(STYLE_NAME_INPUT_FIELD);
		}
		label.setLayoutData(formLayout);
		label.setStyleName(STYLE_NAME_FIELD_LABEL);
		editorComponents.getHelp(fieldName).setStyleName(STYLE_NAME_HELP_LABEL);
		editorComponents.getMessage(fieldName).setStyleName(STYLE_NAME_VALIDATION_LABEL);
		formGrid.add(label);
		formGrid.add(inputComponent);
		formGrid.add(editorComponents.getHelp(fieldName));
		formGrid.add(editorComponents.getMessage(fieldName));

		// By default expand the tree of a CheckBoxTreeSet
		if (inputComponent instanceof CheckBoxTreeSet) {
			((CheckBoxTreeSet) inputComponent).expandAll();
		}
		return inputComponent;
	}

	/**
	 * Add multirow input field like a text area or composite of components in a
	 * table, etc.
	 * 
	 * @param <T>
	 * @param fieldName
	 * @param labelProperty
	 * @param labelDefault
	 * @param inputComponent
	 * @param model
	 * @return
	 */
	protected <T extends Component> T addMultiRowInput(String fieldName, String labelProperty,
			String labelDefault, T inputComponent, Object model) {
		String labelText = getResourceBundleHelper(getLocale()).getString(labelProperty,
				labelDefault);
		Label label = new Label((labelText.endsWith(":") ? labelText : labelText + ":"));
		label.setStyleName(STYLE_NAME_MULTIROW_FIELD_LABEL);
		return addMultiRowInput(fieldName, label, inputComponent, model);
	}

	/**
	 * Add multirow input field like a text area or composite of components in a
	 * table, etc.
	 * 
	 * @param <T>
	 * @param fieldName
	 * @param label
	 * @param inputComponent
	 * @param model
	 * @return
	 */
	protected <T extends Component> T addMultiRowInput(String fieldName, Label label,
			T inputComponent, Object model) {

		editorComponents.addInput(fieldName, label, inputComponent, model);
		inputComponent.setStyleName(STYLE_NAME_INPUT_FIELD);
		inputComponent.setLayoutData(fullRowLayout);
		label.setLayoutData(fullRowLayout);
		label.setStyleName(STYLE_NAME_FIELD_LABEL);

		editorComponents.getHelp(fieldName).setStyleName(STYLE_NAME_HELP_LABEL);
		editorComponents.getMessage(fieldName).setStyleName(STYLE_NAME_VALIDATION_LABEL);
		editorComponents.getMessage(fieldName).setLayoutData(threeQuarterRowLayout);

		formGrid.add(label);
		formGrid.add(inputComponent);
		formGrid.add(editorComponents.getHelp(fieldName));
		formGrid.add(editorComponents.getMessage(fieldName));
		return inputComponent;
	}

	protected Component getInput(String fieldName) {
		return editorComponents.getInput(fieldName);
	}

	protected <T> T getInputModel(String fieldName, Class<T> type) {
		return editorComponents.getInputModel(fieldName, type);
	}

	/**
	 * Replace the model of the named input with the new model.
	 * 
	 * @param fieldName -
	 *            the name of the field
	 * @param model -
	 *            the new model.
	 */
	protected void setInputModel(String fieldName, Object model) {
		editorComponents.setInputModel(fieldName, model);
	}

	/**
	 * Set the value of the named input field.
	 * 
	 * @param fieldName -
	 *            the name of the input field.
	 * @param value -
	 *            the new value to set in the field.
	 */
	protected void setInputValue(String fieldName, Object value) {
		editorComponents.setInputValue(fieldName, value);
	}

	/**
	 * @param <T> -
	 *            the type of the value to return.
	 * @param fieldName
	 * @param type -
	 *            the class of the type to return. Note: primatives are not
	 *            supported, for example Boolean.class should be used instead of
	 *            boolean.class.
	 * @return The value of the input field in the specified type
	 * @throws ClassCastException -
	 *             If the value can't be converted to the specified type.
	 */
	protected <T> T getInputValue(String fieldName, Class<T> type) {
		return editorComponents.getInputValue(fieldName, type);
	}

	/**
	 * Clear the message text for all input fields.
	 */
	public void clearValidationMessages() {
		editorComponents.clearValidationMessages();
	}

	/**
	 * Set the message text of the specified input field.
	 * 
	 * @param fieldName
	 * @param message
	 */
	public void setValidationMessage(String fieldName, String message) {
		editorComponents.setValidationMessage(fieldName, message);
	}

	/**
	 * 
	 */
	public void validateContent() {
		editorComponents.validateContent();
	}

	/**
	 * Implement saving of the target object by overriding this method. If the
	 * user has invalid input throw a ValidationException. Overriding
	 * implementations should call super.save() so that in the future when
	 * controls are self validating that code can be put here.<br>
	 * The save method should fire one or more UpdateEntityEvents to allow other
	 * UI components to refresh any references to the updated entity.<br>
	 * <code>getEventDispatcher().dispatchEvent(new UpdateEntityEvent(this, entity));</code>
	 */
	public void save() {
		validateContent();
	}

	/**
	 * This method doesn't need to be updated
	 */
	public void cancel() {
		clearValidationMessages();
		getEventDispatcher().dispatchEvent(new ClosePanelEvent(this));
	}

	/**
	 * Implement deleting of the target object by overriding this method.<br>
	 * The delete method should fire one or more DeletedEntityEvent to allow
	 * other UI components to refresh any references to the deleted entity and
	 * close panels of deleted sub-components.<br>
	 * <code>
	 * getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, entity));
	 * for (Entity subEntity : deletedSubEntities) {
	 *    getEventDispatcher().dispatchEvent(new DeletedEntityEvent(this, subEntity));
	 * }
	 * </code>
	 */
	public void delete() {

	}

	/**
	 * @return
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @param valid
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	protected boolean isShowTitle() {
		return true;
	}

	/**
	 * This method should be overloaded to check permissions of the user making
	 * the edits and return true if the user only has read permissions.<br>
	 * use getApp().getUser() to get the user.
	 * 
	 * @return
	 */
	public boolean isReadOnlyMode() {
		return false;
	}

	protected boolean isShowSave() {
		return !isReadOnlyMode();
	}

	protected boolean isShowCancel() {
		return true;
	}

	protected boolean isShowDelete() {
		return true;
	}
}
