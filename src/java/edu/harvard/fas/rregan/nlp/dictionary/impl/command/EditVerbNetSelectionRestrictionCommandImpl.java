/*
 * $Id: EditVerbNetSelectionRestrictionCommandImpl.java,v 1.1 2009/02/09 10:12:31 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
