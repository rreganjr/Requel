/*
 * $Id: AddActorPosition.java,v 1.4 2009/01/19 09:32:25 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ResolveIssueWithAddActorPositionCommandImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the word is added as an actor to the project.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.impl.AddActorPosition")
@XmlRootElement(name = "addActorPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "addActorPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class AddActorPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	// TODO: use spring to register the command for the positions, or make the
	// position class implement the command
	static {
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddActorPosition.class,
				ResolveIssueWithAddActorPositionCommandImpl.class);
	}

	/**
	 * @param text
	 * @param createdBy
	 */
	public AddActorPosition(String text, User createdBy) {
		super(AddActorPosition.class.getName(), text, createdBy);
	}

	protected AddActorPosition() {
		super();
		// for hibernate
	}
}
