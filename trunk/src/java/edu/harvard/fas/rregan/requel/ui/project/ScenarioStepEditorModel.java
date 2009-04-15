/*
 * $Id: ScenarioStepEditorModel.java,v 1.2 2009/01/20 10:26:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.ui.project;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import echopointng.text.StringDocumentEx;
import edu.harvard.fas.rregan.requel.project.ScenarioType;
import edu.harvard.fas.rregan.requel.project.Step;
import edu.harvard.fas.rregan.uiframework.panel.editor.CombinedListModel;

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
