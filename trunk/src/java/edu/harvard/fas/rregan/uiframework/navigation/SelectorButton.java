/*
 * $Id: SelectorButton.java,v 1.10 2008/10/11 08:22:31 rregan Exp $
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
package edu.harvard.fas.rregan.uiframework.navigation;

import java.util.HashSet;
import java.util.Set;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.event.ChangeEvent;
import nextapp.echo2.app.event.ChangeListener;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.uiframework.AbstractAppAwareActionListener;
import edu.harvard.fas.rregan.uiframework.UIFrameworkApp;
import edu.harvard.fas.rregan.uiframework.navigation.event.OpenPanelEvent;
import edu.harvard.fas.rregan.uiframework.navigation.event.SelectEntityEvent;
import edu.harvard.fas.rregan.uiframework.panel.PanelActionType;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.SelectorButtonManipulator;

/**
 * @author ron
 */
public class SelectorButton extends Column {
	private static final Logger log = Logger.getLogger(SelectorButton.class);
	static final long serialVersionUID = 0;

	static {
		ComponentManipulators.setManipulator(SelectorButton.class, new SelectorButtonManipulator());
	}

	private final Set<ChangeListener> changeListeners = new HashSet<ChangeListener>();

	private Label selectedItemIndicator;
	private final SelectionIndicatorLabelAdapter selectionIndicatorLabelAdapter;
	private NavigatorButton openSelectorButton;
	private ActionListener selectionListener;
	private final Class<?> selectionType;
	private Object selectedObject;
	private final String labelForNoSelectedItem = "<nothing selected>";

	/**
	 * @param selectionIndicatorLabelAdapter
	 * @param selectionType
	 */
	public SelectorButton(SelectionIndicatorLabelAdapter selectionIndicatorLabelAdapter,
			Class<?> selectionType) {
		super();
		this.selectionIndicatorLabelAdapter = selectionIndicatorLabelAdapter;
		this.selectionType = selectionType;
		createComponents();
		setSelectedObject(null);
		setSelectionCriteria(null, null, null);
	}

	/**
	 * @param selectionIndicatorLabelAdapter
	 * @param selectionType
	 * @param selectedObject
	 */
	public SelectorButton(SelectionIndicatorLabelAdapter selectionIndicatorLabelAdapter,
			Class<?> selectionType, Object selectedObject) {
		super();
		this.selectionIndicatorLabelAdapter = selectionIndicatorLabelAdapter;
		this.selectionType = selectionType;
		createComponents();
		setSelectedObject(selectedObject);
		setSelectionCriteria(null, null, null);
	}

	/**
	 * @param selectionIndicatorLabelAdapter
	 * @param selectionType
	 * @param selectedObject
	 * @param selectionCriteriaType
	 * @param selectionCriteria
	 * @param panelName
	 */
	public SelectorButton(SelectionIndicatorLabelAdapter selectionIndicatorLabelAdapter,
			Class<?> selectionType, Object selectedObject, Class<?> selectionCriteriaType,
			Object selectionCriteria, String panelName) {
		super();
		this.selectionIndicatorLabelAdapter = selectionIndicatorLabelAdapter;
		this.selectionType = selectionType;
		createComponents();
		setSelectedObject(selectedObject);
		setSelectionCriteria(selectionCriteriaType, selectionCriteria, panelName);
	}

	/**
	 * @return
	 */
	public Object getSelectedObject() {
		return selectedObject;
	}

	/**
	 * @param selectedObject
	 */
	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
		if (selectedObject != null) {
			selectedItemIndicator.setText(selectionIndicatorLabelAdapter
					.getLabelString(selectedObject));
		} else {
			selectedItemIndicator.setText((labelForNoSelectedItem == null ? ""
					: labelForNoSelectedItem));
		}
		fireChangeEvent();
	}

	/**
	 * @param selectionCriteriaType
	 * @param selectionCriteria
	 * @param panelName
	 */
	public void setSelectionCriteria(Class<?> selectionCriteriaType, Object selectionCriteria,
			String panelName) {
		OpenPanelEvent opeSelectorEvent;

		// the "source" of the event will be set as the destination of the
		// SelectEntityEvent
		// fired by the selector panel. The SelectObjectListener created in
		// createComponents()
		// will catch the select entity event and set the selected object on the
		// button.
		if (selectionCriteria != null) {
			opeSelectorEvent = new OpenPanelEvent(this, PanelActionType.Selector,
					selectionCriteria, selectionCriteriaType, panelName,
					WorkflowDisposition.ContinueFlow);
		} else {
			opeSelectorEvent = new OpenPanelEvent(this, PanelActionType.Selector, null,
					selectionType, null, WorkflowDisposition.ContinueFlow);
		}
		setOpenSelectorEvent(opeSelectorEvent);
	}

	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}

	@Override
	public void dispose() {
		changeListeners.clear();
		if (selectionListener != null) {
			getApp().getEventDispatcher().removeEventTypeActionListener(SelectEntityEvent.class,
					selectionListener, this);
			selectionListener = null;
		}
		super.dispose();
	}

	protected void fireChangeEvent() {
		if (!changeListeners.isEmpty()) {
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : changeListeners) {
				l.stateChanged(e);
			}
		}
	}

	private void createComponents() {
		RowLayoutData rld = new RowLayoutData();
		rld.setInsets(new Insets(new Extent(0), new Extent(0), new Extent(5), new Extent(0)));
		selectedItemIndicator = new Label("");
		selectedItemIndicator.setStyleName("Default");
		selectedItemIndicator.setLayoutData(rld);

		openSelectorButton = new NavigatorButton("Select", getApp().getEventDispatcher());
		openSelectorButton.setStyleName("Default");
		Row row = new Row();
		row.add(selectedItemIndicator);
		row.add(openSelectorButton);
		add(row);
		// The SelectObjectListener will catch the select entity event and set
		// the selected
		// object on the button. The listener is registered explicitly for
		// events with the
		// destination of this button
		selectionListener = getApp().getEventDispatcher().addEventTypeActionListener(
				SelectEntityEvent.class, new SelectObjectListener(selectionType, this, getApp()),
				this);
	}

	private void setOpenSelectorEvent(OpenPanelEvent eventToFire) {
		openSelectorButton.setEventToFire(eventToFire);
	}

	private UIFrameworkApp getApp() {
		return UIFrameworkApp.getApp();
	}

	/**
	 * An interface that needs to be implemented to return a string that
	 * describes the selected object attached to the button.
	 * 
	 * @author ron
	 */
	public static interface SelectionIndicatorLabelAdapter {
		/**
		 * @param selectedObject
		 * @return a string describing the supplied selected object to a user.
		 */
		public String getLabelString(Object selectedObject);
	}

	/**
	 * This listens for a select object event from a selection panel and sets
	 * the selected object on the button.
	 * 
	 * @author ron
	 */
	private static class SelectObjectListener extends AbstractAppAwareActionListener {
		static final long serialVersionUID = 0;

		private final Class<?> selectionType;
		private final SelectorButton button;

		public SelectObjectListener(Class<?> selectionType, SelectorButton button,
				UIFrameworkApp app) {
			super(app);
			this.selectionType = selectionType;
			this.button = button;
		}

		public void actionPerformed(ActionEvent e) {
			if (e instanceof SelectEntityEvent) {
				SelectEntityEvent event = (SelectEntityEvent) e;
				if (this.button.equals(event.getDestinationObject()) && (event.getObject() != null)) {
					if (selectionType.isAssignableFrom(event.getObject().getClass())) {
						button.setSelectedObject(event.getObject());
					}
				}
			}
		}
	}
}
