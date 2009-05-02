/*
 * $Id: Story2StoryImplAdapter.java,v 1.1 2008/09/06 09:31:57 rregan Exp $
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

import edu.harvard.fas.rregan.requel.project.Story;

/**
 * Adapter for JAXB to convert interface Story to class StoryImpl and back.
 * 
 * @author ron
 */
@XmlTransient
public class Story2StoryImplAdapter extends XmlAdapter<StoryImpl, Story> {

	@Override
	public StoryImpl marshal(Story story) throws Exception {
		return (StoryImpl) story;
	}

	@Override
	public Story unmarshal(StoryImpl story) throws Exception {
		return story;
	}

}