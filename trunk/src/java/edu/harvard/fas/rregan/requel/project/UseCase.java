/*
 * $Id: UseCase.java,v 1.8 2008/10/10 07:02:32 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project;

/**
 * @author ron
 */
public interface UseCase extends TextEntity, GoalContainer, ActorContainer, StoryContainer,
		Comparable<UseCase> {

	/**
	 * @return
	 */
	public Actor getPrimaryActor();

	/**
	 * @return
	 */
	public Scenario getScenario();
}
