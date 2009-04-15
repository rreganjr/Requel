/*
 * $Id: TextComponentManipulator.java,v 1.8 2008/10/13 22:58:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.panel.editor.manipulators;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.event.DocumentEvent;
import nextapp.echo2.app.event.DocumentListener;
import nextapp.echo2.app.text.Document;
import nextapp.echo2.app.text.TextComponent;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;

/**
 * @author ron
 */
public class TextComponentManipulator extends AbstractComponentManipulator {

	public <T> T getValue(Component component, Class<T> type) {
		return type.cast(getComponent(component).getText());
	}

	public void setValue(Component component, Object value) {
		getComponent(component).setText((String) value);
	}

	public void addListenerToDetectChangesToInput(final EditMode editMode, Component component) {
		final TextComponent text = (TextComponent) component;
		text.getDocument().addDocumentListener(new DocumentListener() {
			static final long serialVersionUID = 0L;

			public void documentUpdate(DocumentEvent e) {
				editMode.setStateEdited(true);
			}
		});
	}

	@Override
	public Document getModel(Component component) {
		return getComponent(component).getDocument();
	}

	@Override
	public void setModel(Component component, Object valueModel) {
		getComponent(component).setDocument((Document) valueModel);
	}

	private TextComponent getComponent(Component component) {
		return (TextComponent) component;
	}
}
