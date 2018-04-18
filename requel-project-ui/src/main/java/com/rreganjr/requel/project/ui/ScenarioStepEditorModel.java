/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
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
package com.rreganjr.requel.project.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import echopointng.text.StringDocumentEx;
import com.rreganjr.requel.project.ScenarioType;
import com.rreganjr.requel.project.Step;
import net.sf.echopm.panel.editor.CombinedListModel;

/**
 * A composite model of the models used to edit a step in the step editor.
 * 
 * @author ron
 */
public class ScenarioStepEditorModel {

	private Step step;
	private CombinedListModel scenarioTypeModel;
	private StringDocumentEx nameModel;
	private final Set<String> scenarioTypeNames = new TreeSet<String>();
	private final String defaultScenarioTypeName = ScenarioType.Primary.name();

	public ScenarioStepEditorModel(Step step) {
		setStep(step);
	}

	public ScenarioStepEditorModel(Collection<String> scenarioTypeNames) {
		setScenarioTypeNames(scenarioTypeNames);
		scenarioTypeModel = new CombinedListModel(getScenarioTypeNames(), defaultScenarioTypeName,
				true);
		nameModel = new StringDocumentEx("");
	}

	public ScenarioStepEditorModel(Collection<String> scenarioTypeNames, Step step) {
		setScenarioTypeNames(scenarioTypeNames);
		scenarioTypeModel = new CombinedListModel(getScenarioTypeNames(), defaultScenarioTypeName,
				true);
		nameModel = new StringDocumentEx("");
		setStep(step);
	}

	public Step getStep() {
		return step;
	}

	public void setStep(Step step) {
		this.step = step;
		if (step != null) {
			scenarioTypeModel.setSelectedItem(step.getType().name());
			nameModel.setText(step.getName());
		} else {
			scenarioTypeModel.clearSelection();
			scenarioTypeModel.setSelectedItem(defaultScenarioTypeName);
			nameModel.setText("");
		}
	}

	public String getName() {
		return nameModel.getText();
	}

	public String getScenarioTypeName() {
		return (String) scenarioTypeModel.get(scenarioTypeModel.getMinSelectedIndex());
	}

	public StringDocumentEx getNameModel() {
		return nameModel;
	}

	public CombinedListModel getScenarioTypeModel() {
		return scenarioTypeModel;
	}

	public void setScenarioTypeNames(String[] scenarioTypeNames) {
		setScenarioTypeNames(Arrays.asList(scenarioTypeNames));
	}

	protected void setScenarioTypeNames(Collection<String> scenarioTypeNames) {
		this.scenarioTypeNames.clear();
		for (String scenarioTypeName : scenarioTypeNames) {
			this.scenarioTypeNames.add(scenarioTypeName);
		}
	}

	protected Set<String> getScenarioTypeNames() {
		return scenarioTypeNames;
	}
}
