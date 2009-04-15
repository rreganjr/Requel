/*
 * $Id: EditorComponents.java,v 1.3 2009/02/21 10:32:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.HashMap;
import java.util.Map;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * UI component management for an editor based on named fields.
 * 
 * @author ron
 */
public class EditorComponents implements EditMode {
	static final long serialVersionUID = 0L;

	private final Map<String, FieldComponents> fieldNameToComponents = new HashMap<String, FieldComponents>();
	private final Map<Component, String> inputComponentToFieldName = new HashMap<Component, String>();
	private boolean valid;
	private String generalErrorMessage = "";
	private final EditMode editMode;

	/**
	 * @param editMode
	 */
	public EditorComponents(EditMode editMode) {
		this.editMode = editMode;
	}

	/**
	 * This method should be overloaded to check permissions of the user making
	 * the edits and return true if the user only has read permissions.<br>
	 * use getApp().getUser() to get the user.
	 * 
	 * @return
	 */
	public boolean isReadOnlyMode() {
		return editMode.isReadOnlyMode();
	}

	@Override
	public boolean isStateEdited() {
		return editMode.isStateEdited();
	}

	@Override
	public void setStateEdited(boolean stateEdited) {
		editMode.setStateEdited(stateEdited);
	}

	/**
	 * @param generalErrorMessage
	 */
	public void setGeneralErrorMessage(String generalErrorMessage) {
		this.generalErrorMessage = generalErrorMessage;
	}

	/**
	 * @return
	 */
	public String getGeneralErrorMessage() {
		return generalErrorMessage;
	}

	/**
	 * @param <T>
	 * @param fieldName
	 * @param label
	 * @param inputComponent
	 * @param model
	 * @return
	 */
	public <T extends Component> T addInput(String fieldName, Label label, T inputComponent,
			Object model) {
		if (fieldComponentsExists(fieldName)) {
			throw new IllegalArgumentException("the specified fieldName '" + fieldName
					+ "' already exists.");
		}
		FieldComponents fieldComponents = new FieldComponents();
		fieldNameToComponents.put(fieldName, fieldComponents);
		inputComponentToFieldName.put(inputComponent, fieldName);
		if (isReadOnlyMode()) {
			inputComponent.setEnabled(false);
		}

		Component help = new Label();

		Label message = new Label();

		fieldComponents.label = label;
		fieldComponents.inputComponent = inputComponent;
		fieldComponents.help = help;
		fieldComponents.message = message;
		fieldComponents.model = model;

		ComponentManipulator componentManipulator = ComponentManipulators
				.getManipulator(inputComponent);
		componentManipulator.setModel(inputComponent, model);
		// do this after setting the model
		addListenerToDetectChangesToInput(inputComponent);
		return inputComponent;
	}

	/**
	 * @param fieldName
	 * @return The input component for the given input field name.
	 */
	public Component getInput(String fieldName) {
		return getFieldComponents(fieldName).inputComponent;
	}

	/**
	 * @param fieldName
	 * @return
	 */
	public Label getLabel(String fieldName) {
		return getFieldComponents(fieldName).label;
	}

	/**
	 * @param fieldName
	 * @return
	 */
	public Component getHelp(String fieldName) {
		return getFieldComponents(fieldName).help;
	}

	/**
	 * @param fieldName
	 * @return
	 */
	public Label getMessage(String fieldName) {
		return getFieldComponents(fieldName).message;
	}

	/**
	 * Replace the model of the named input with the new model.
	 * 
	 * @param fieldName -
	 *            the name of the field
	 * @param model -
	 *            the new model.
	 */
	public void setInputModel(String fieldName, Object model) {
		Component inputComponent = getInput(fieldName);
		ComponentManipulator componentManipulator = ComponentManipulators
				.getManipulator(inputComponent);
		componentManipulator.setModel(inputComponent, model);
	}

	/**
	 * @param <T>
	 * @param fieldName
	 * @param type
	 * @return
	 */
	public <T> T getInputModel(String fieldName, Class<T> type) {
		Component inputComponent = getInput(fieldName);
		ComponentManipulator componentManipulator = ComponentManipulators
				.getManipulator(inputComponent);
		return type.cast(componentManipulator.getModel(inputComponent));
	}

	/**
	 * Set the value of the named input field.
	 * 
	 * @param fieldName -
	 *            the name of the input field.
	 * @param value -
	 *            the new value to set in the field.
	 */
	public void setInputValue(String fieldName, Object value) {
		Component inputComponent = getInput(fieldName);
		ComponentManipulator componentManipulator = ComponentManipulators
				.getManipulator(inputComponent);
		componentManipulator.setValue(inputComponent, value);
	}

	/**
	 * @param <T>
	 * @param fieldName
	 * @param type
	 * @return
	 */
	public <T> T getInputValue(String fieldName, Class<T> type) {
		Component inputComponent = getInput(fieldName);
		ComponentManipulator componentManipulator = ComponentManipulators
				.getManipulator(inputComponent);
		return componentManipulator.getValue(inputComponent, type);
	}

	/**
	 * Clear the message text for all input fields.
	 */
	public void clearValidationMessages() {
		for (FieldComponents fieldComponents : fieldNameToComponents.values()) {
			fieldComponents.message.setText("");
		}
		setGeneralErrorMessage("");
	}

	/**
	 * Set the message text of the specified input field.
	 * 
	 * @param fieldName
	 * @param message
	 */
	public void setValidationMessage(String fieldName, String message) {
		getMessage(fieldName).setText(message);
	}

	/**
	 * 
	 */
	public void validateContent() {
		clearValidationMessages();
		// subclasses should call super.validateContent() at the start
		// of all sub-classes.
	}

	/**
	 * Implement saving of the target object here. If the user has invalid input
	 * throw a ValidationException. Overriding implementations should call
	 * super.save() so that in the future when controls are self validating that
	 * code can be put here.
	 */
	public void save() {
		validateContent();
	}

	/**
	 * 
	 */
	public void cancel() {
		clearValidationMessages();
	}

	/**
	 * 
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

	protected void addListenerToDetectChangesToInput(Component inputComponent) {
		ComponentManipulators.getManipulator(inputComponent).addListenerToDetectChangesToInput(
				this, inputComponent);
	}

	protected FieldComponents getFieldComponents(String fieldName) {
		if (fieldComponentsExists(fieldName)) {
			return fieldNameToComponents.get(fieldName);
		}
		throw new IllegalArgumentException("the specified fieldName '" + fieldName
				+ "' does not match a field in the editor.");
	}

	protected boolean fieldComponentsExists(String fieldName) {
		return fieldNameToComponents.containsKey(fieldName);
	}

	private static class FieldComponents {
		private Label label;
		private Component inputComponent;
		private Object model;
		private Component help;
		private Label message;
	}
}
