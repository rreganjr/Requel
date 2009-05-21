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

import static edu.harvard.fas.rregan.uiframework.panel.Panel.STYLE_NAME_DEFAULT;

import java.util.Set;
import java.util.TreeSet;

import nextapp.echo2.app.Alignment;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.TextArea;
import nextapp.echo2.app.layout.RowLayoutData;

import org.apache.log4j.Logger;

import edu.harvard.fas.rregan.ResourceBundleHelper;
import edu.harvard.fas.rregan.requel.project.ScenarioType;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.requel.ui.AbstractRequelComponent;
import edu.harvard.fas.rregan.uiframework.panel.editor.EditMode;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.AbstractComponentManipulator;
import edu.harvard.fas.rregan.uiframework.panel.editor.manipulators.ComponentManipulators;

/**
 * An editor component for editing a scenario step's type and name. This is
 * intended to be embedded in a larger component such as a Tree or Table for
 * editing a collection of steps.
 * 
 * @author ron
 */
public class ScenarioStepEditor extends AbstractRequelComponent {
	private static final Logger log = Logger.getLogger(ScenarioStepEditor.class);
	static final long serialVersionUID = 0L;

	/**
	 * The style name for step name text editor.
	 */
	public static final String STYLE_NAME_STEP_NAME_EDITOR = "ScenarioStepEditor.NameEditor";

	static {
		ComponentManipulators.setManipulator(ScenarioStepEditor.class,
				new ScenarioStepEditorManipulator());
	}

	private ScenarioStepEditorModel model;
	private SelectField scenarioTypeEditor;
	private TextArea nameEditor;
	private final Row layoutContainer = new Row();
	private final RowLayoutData rowLayoutTop = new RowLayoutData();

	/**
	 * Create a scenario step editor for creating a new step.
	 * 
	 * @param editMode
	 *            see {@link EditMode}
	 * @param resourceBundleHelper
	 */
	public ScenarioStepEditor(EditMode editMode, ResourceBundleHelper resourceBundleHelper) {
		super(editMode, resourceBundleHelper);
		rowLayoutTop.setAlignment(Alignment.ALIGN_TOP);
		setup();
		setModel(new ScenarioStepEditorModel(getScenarioTypeNames()));
	}

	/**
	 * Create a scenario step editor for editing an existing step.
	 * 
	 * @param editMode
	 * @param resourceBundleHelper
	 * @param step
	 */
	public ScenarioStepEditor(EditMode editMode, ResourceBundleHelper resourceBundleHelper,
			Step step) {
		super(editMode, resourceBundleHelper);
		setup();
		setModel(new ScenarioStepEditorModel(getScenarioTypeNames(), step));
	}

	private void setup() {
		layoutContainer.setCellSpacing(new Extent(5));
		scenarioTypeEditor = new SelectField();
		scenarioTypeEditor.setStyleName(STYLE_NAME_DEFAULT);
		scenarioTypeEditor.setLayoutData(rowLayoutTop);
		layoutContainer.add(scenarioTypeEditor);
		nameEditor = new TextArea();
		nameEditor.setStyleName(STYLE_NAME_STEP_NAME_EDITOR);
		nameEditor.setLayoutData(rowLayoutTop);
		// TODO: don't hard code this - it reflects the name db column size
		nameEditor.setMaximumLength(255);
		layoutContainer.add(nameEditor);
		add(layoutContainer);
	}

	/**
	 * @return
	 */
	public ScenarioStepEditorModel getModel() {
		return model;
	}

	/**
	 * @param model
	 */
	public void setModel(ScenarioStepEditorModel model) {
		this.model = model;
		scenarioTypeEditor.setModel(model.getScenarioTypeModel());
		scenarioTypeEditor.setSelectionModel(model.getScenarioTypeModel());
		nameEditor.setDocument(model.getNameModel());
	}

	private Set<String> getScenarioTypeNames() {
		Set<String> scenarioTypeNames = new TreeSet<String>();
		for (ScenarioType scenarioType : ScenarioType.values()) {
			scenarioTypeNames.add(scenarioType.toString());
		}
		return scenarioTypeNames;
	}

	// TODO: this may not be needed
	private static class ScenarioStepEditorManipulator extends AbstractComponentManipulator {

		protected ScenarioStepEditorManipulator() {
			super();
		}

		@Override
		public ScenarioStepEditorModel getModel(Component component) {
			return getComponent(component).getModel();
		}

		@Override
		public void setModel(Component component, Object valueModel) {
			getComponent(component).setModel((ScenarioStepEditorModel) valueModel);
		}

		@Override
		public void addListenerToDetectChangesToInput(EditMode editMode, Component component) {
			// TODO
		}

		@Override
		public <T> T getValue(Component component, Class<T> type) {
			return type.cast(getModel(component).getStep());
		}

		@Override
		public void setValue(Component component, Object value) {
			getModel(component).setStep((Step) value);
		}

		private ScenarioStepEditor getComponent(Component component) {
			return (ScenarioStepEditor) component;
		}
	}
}
