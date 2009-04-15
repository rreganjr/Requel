/*
 * $Id: ScenarioContainer.java,v 1.1 2008/10/08 21:55:05 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

import java.util.Comparator;
import java.util.Set;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

/**
 * A thing that can contain/refer to scenarios.
 * 
 * @author ron
 */
public interface ScenarioContainer extends Describable, CreatedEntity {
	/**
	 * The scenarios referenced.
	 * 
	 * @return
	 */
	public Set<Scenario> getScenarios();

	/**
	 * Compare the objects that contain Scenarios by the description.
	 */
	public static final Comparator<ScenarioContainer> COMPARATOR = new ScenarioContainerComparator();

	/**
	 * A Comparator for collections of Scenario containers.
	 */
	public static class ScenarioContainerComparator implements Comparator<ScenarioContainer> {
		@Override
		public int compare(ScenarioContainer o1, ScenarioContainer o2) {
			return o1.getDescription().compareTo(o2.getDescription());
		}
	}
}
