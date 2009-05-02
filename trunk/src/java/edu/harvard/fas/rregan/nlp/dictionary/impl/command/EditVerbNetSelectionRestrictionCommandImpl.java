/*
 * $Id: EditVerbNetSelectionRestrictionCommandImpl.java,v 1.1 2009/02/09 10:12:31 rregan Exp $
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
package edu.harvard.fas.rregan.nlp.dictionary.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetRoleRef;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestriction;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestrictionType;
import edu.harvard.fas.rregan.nlp.dictionary.command.EditVerbNetSelectionRestrictionCommand;

/**
 * @author ron
 */
@Controller("editVerbNetSelectionRestrictionCommand")
@Scope("prototype")
public class EditVerbNetSelectionRestrictionCommandImpl extends AbstractDictionaryCommand implements
		EditVerbNetSelectionRestrictionCommand {

	private VerbNetRoleRef roleRef;
	private VerbNetSelectionRestrictionType selResType;
	private String include;
	private VerbNetSelectionRestriction selRes;

	/**
	 * @param dictionaryRepository
	 */
	@Autowired
	public EditVerbNetSelectionRestrictionCommandImpl(DictionaryRepository dictionaryRepository) {
		super(dictionaryRepository);
	}

	@Override
	public VerbNetRoleRef getVerbNetRoleRef() {
		return roleRef;
	}

	@Override
	public void setVerbNetRoleRef(VerbNetRoleRef roleRef) {
		this.roleRef = roleRef;
	}

	protected String getInclude() {
		return include;
	}

	@Override
	public void setInclude(String include) {
		this.include = include;
	}

	@Override
	public VerbNetSelectionRestriction getVerbNetSelectionRestriction() {
		return selRes;
	}

	@Override
	public void setVerbNetSelectionRestriction(VerbNetSelectionRestriction selRes) {
		this.selRes = selRes;
	}

	@Override
	public void setVerbNetSelectionRestrictionType(VerbNetSelectionRestrictionType selResType) {
		this.selResType = selResType;
	}

	protected VerbNetSelectionRestrictionType getVerbNetSelectionRestrictionType() {
		return this.selResType;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() {
		try {
			VerbNetRoleRef roleRef = getDictionaryRepository().get(getVerbNetRoleRef());
			VerbNetSelectionRestrictionType type = getDictionaryRepository().get(
					getVerbNetSelectionRestrictionType());
			VerbNetSelectionRestriction restriction = getVerbNetSelectionRestriction();
			if (restriction == null) {
				restriction = new VerbNetSelectionRestriction(roleRef, type, getInclude());
				getDictionaryRepository().persist(restriction);
			} else {
				// TODO: edit an existing?
			}
			roleRef.getSelectionalRestrictions().add(restriction);
			setVerbNetRoleRef(getDictionaryRepository().merge(roleRef));
			setVerbNetSelectionRestriction(restriction);
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
