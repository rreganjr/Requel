/*
 * $Id: Story2StoryImplAdapter.java,v 1.1 2008/09/06 09:31:57 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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