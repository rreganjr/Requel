/*
 * $Id: CombinedListModel.java,v 1.7 2008/10/13 22:58:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.uiframework.panel.editor;

import java.util.Collections;
import java.util.Set;

import nextapp.echo2.app.event.ChangeListener;
import nextapp.echo2.app.event.ListDataListener;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.app.list.DefaultListSelectionModel;
import nextapp.echo2.app.list.ListModel;
import nextapp.echo2.app.list.ListSelectionModel;

/**
 * @author ron
 */
public class CombinedListModel implements ListModel, ListSelectionModel {
	static final long serialVersionUID = 0L;

	final private DefaultListSelectionModel listSelectionModel;
	final private DefaultListModel listModel;

	/**
	 * @param options
	 * @param initialSelection
	 * @param singleSelection
	 */
	public CombinedListModel(Set<?> options, Object initialSelection, boolean singleSelection) {
		this(options, Collections.singleton(initialSelection), singleSelection);
	}

	/**
	 * @param options
	 * @param initialSelection
	 * @param singleSelection
	 */
	public CombinedListModel(Set<?> options, Set<?> initialSelection, boolean singleSelection) {
		if (singleSelection && (initialSelection.size() > 1)) {
			throw new IllegalArgumentException(
					"single selection was specified, but more than one initial selected item was supplied.");
		}
		listModel = new DefaultListModel(options.toArray());
		listSelectionModel = new DefaultListSelectionModel();
		listSelectionModel.setSelectionMode(singleSelection ? ListSelectionModel.SINGLE_SELECTION
				: ListSelectionModel.MULTIPLE_SELECTION);
		for (Object item : initialSelection) {
			int index = listModel.indexOf(item);
			if (index > -1) {
				listSelectionModel.setSelectedIndex(index, true);
			}
		}
	}

	/**
	 * @param listSelectionModel
	 * @param listModel
	 */
	public CombinedListModel(DefaultListSelectionModel listSelectionModel,
			DefaultListModel listModel) {
		this.listSelectionModel = listSelectionModel;
		this.listModel = listModel;
	}

	public void addListDataListener(ListDataListener l) {
		listModel.addListDataListener(l);
	}

	public int indexOf(Object item) {
		return listModel.indexOf(item);
	}

	public Object get(int index) {
		if ((index > -1) && (index < listModel.size())) {
			return listModel.get(index);
		}
		return null;
	}

	public void removeListDataListener(ListDataListener l) {
		listModel.removeListDataListener(l);
	}

	public int size() {
		return listModel.size();
	}

	public void addChangeListener(ChangeListener l) {
		listSelectionModel.addChangeListener(l);
	}

	public void clearSelection() {
		listSelectionModel.clearSelection();
	}

	public int getMaxSelectedIndex() {
		return listSelectionModel.getMaxSelectedIndex();
	}

	public int getMinSelectedIndex() {
		return listSelectionModel.getMaxSelectedIndex();
	}

	public int getSelectionMode() {
		return listSelectionModel.getSelectionMode();
	}

	public boolean isSelectedIndex(int index) {
		return listSelectionModel.isSelectedIndex(index);
	}

	public boolean isSelectionEmpty() {
		return listSelectionModel.isSelectionEmpty();
	}

	public void removeChangeListener(ChangeListener l) {
		listSelectionModel.removeChangeListener(l);
	}

	public void setSelectedItem(Object item) {
		if (getSelectionMode() == ListSelectionModel.SINGLE_SELECTION) {
			clearSelection();
		}
		setSelectedIndex(indexOf(item), true);
	}
	
	public void setSelectedIndex(int index, boolean selected) {
		listSelectionModel.setSelectedIndex(index, selected);
	}

	public void setSelectionMode(int selectionMode) {
		listSelectionModel.setSelectionMode(selectionMode);
	}
}
