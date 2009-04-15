/*
 * $Id: AddGlossaryTermPosition.java,v 1.7 2009/01/19 09:32:25 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.harvard.fas.rregan.requel.annotation.impl.PositionImpl;
import edu.harvard.fas.rregan.requel.annotation.impl.command.AnnotationCommandFactoryImpl;
import edu.harvard.fas.rregan.requel.project.impl.command.ResolveIssueWithAddGlossaryTermPositionCommandImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * This is a special position such that when it is chosen as the resolution of
 * an issue via resolveIssue() the word is added to the project glossary.
 * 
 * @author ron
 */
@Entity
@DiscriminatorValue(value = "edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition")
@XmlRootElement(name = "addGlossaryTermPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "addGlossaryTermPosition", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class AddGlossaryTermPosition extends PositionImpl {
	static final long serialVersionUID = 0L;

	// TODO: use spring to register the command for the positions, or make the
	// position class implement the command
	static {
		AnnotationCommandFactoryImpl.addPositionResolverCommand(AddGlossaryTermPosition.class,
				ResolveIssueWithAddGlossaryTermPositionCommandImpl.class);
	}

	/**
	 * @param text
	 * @param createdBy
	 */
	public AddGlossaryTermPosition(String text, User createdBy) {
		super(AddGlossaryTermPosition.class.getName(), text, createdBy);
	}

	protected AddGlossaryTermPosition() {
		super();
		// for hibernate
	}
}
