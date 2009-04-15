/*
 * $Id: CombinedTextListModel.java,v 1.1 2008/03/18 10:25:44 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.Set;

import nextapp.echo2.app.event.DocumentListener;
import nextapp.echo2.app.event.ListDataListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.app.list.ListModel;
import nextapp.echo2.app.text.Document;
import nextapp.echo2.app.text.StringDocument;
import echopointng.text.StringDocumentEx;

/**
 * @author ron
 */
public class CombinedTextListModel implements Document, ListModel {
	static final long serialVersionUID = 0L;

	private final StringDocument document;
	private final DefaultListModel listModel;

	/**
	 * @param defaultText
	 * @param options
	 */
	public CombinedTextListModel(Set<?> options, String defaultText) {
		this(new DefaultListModel(options.toArray()), new StringDocumentEx(defaultText));
	}

	/**
	 * @param document
	 * @param listModel
	 */
	public CombinedTextListModel(DefaultListModel listModel, StringDocument document) {
		super();
		this.document = document;
		this.listModel = listModel;
	}

	/**
	 * @see nextapp.echo2.app.text.Document#addDocumentListener(nextapp.echo2.app.event.DocumentListener)
	 */
	@Override
	public void addDocumentListener(DocumentListener l) {
		document.addDocumentListener(l);
	}

	/**
	 * @see nextapp.echo2.app.text.Document#getText()
	 */
	@Override
	public String getText() {
		return document.getText();
	}

	/**
	 * @see nextapp.echo2.app.text.Document#removeDocumentListener(nextapp.echo2.app.event.DocumentListener)
	 */
	@Override
	public void removeDocumentListener(DocumentListener l) {
		document.removeDocumentListener(l);

	}

	/**
	 * @see nextapp.echo2.app.text.Document#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		document.setText(text);

	}

	/**
	 * @see nextapp.echo2.app.list.ListModel#addListDataListener(nextapp.echo2.app.event.ListDataListener)
	 */
	@Override
	public void addListDataListener(ListDataListener l) {
		listModel.addListDataListener(l);

	}

	/**
	 * @see nextapp.echo2.app.list.ListModel#get(int)
	 */
	@Override
	public Object get(int index) {
		return listModel.get(index);
	}

	/**
	 * @see nextapp.echo2.app.list.ListModel#removeListDataListener(nextapp.echo2.app.event.ListDataListener)
	 */
	@Override
	public void removeListDataListener(ListDataListener l) {
		listModel.removeListDataListener(l);

	}

	/**
	 * @see nextapp.echo2.app.list.ListModel#size()
	 */
	@Override
	public int size() {
		return listModel.size();
	}

}
