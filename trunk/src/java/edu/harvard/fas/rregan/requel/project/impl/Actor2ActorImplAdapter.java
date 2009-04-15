/*
 * $Id: Actor2ActorImplAdapter.java,v 1.1 2008/09/06 09:31:58 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import edu.harvard.fas.rregan.requel.project.Actor;

/**
 * Adapter for JAXB to convert interface Actor to class ActorImpl and back.
 * 
 * @author ron
 */
@XmlTransient
public class Actor2ActorImplAdapter extends XmlAdapter<ActorImpl, Actor> {

	@Override
	public ActorImpl marshal(Actor actor) throws Exception {
		return (ActorImpl) actor;
	}

	@Override
	public Actor unmarshal(ActorImpl actor) throws Exception {
		return actor;
	}

}