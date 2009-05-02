/*
 * $Id: Goal2GoalImplAdapter.java,v 1.2 2008/06/25 09:58:49 rregan Exp $
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