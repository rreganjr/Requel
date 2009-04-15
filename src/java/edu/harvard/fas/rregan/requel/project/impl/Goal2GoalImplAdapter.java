/*
 * $Id: Goal2GoalImplAdapter.java,v 1.2 2008/06/25 09:58:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import edu.harvard.fas.rregan.requel.project.Goal;

/**
 * Adapter for JAXB to convert interface Goal to class GoalImpl and back.
 * 
 * @author ron
 */
@XmlTransient
public class Goal2GoalImplAdapter extends XmlAdapter<GoalImpl, Goal> {

	@Override
	public GoalImpl marshal(Goal goal) throws Exception {
		return (GoalImpl) goal;
	}

	@Override
	public Goal unmarshal(GoalImpl goal) throws Exception {
		return goal;
	}

}