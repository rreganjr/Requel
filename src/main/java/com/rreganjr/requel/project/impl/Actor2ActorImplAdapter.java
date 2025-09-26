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
package com.rreganjr.requel.project.impl;

import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import com.rreganjr.requel.project.Actor;

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