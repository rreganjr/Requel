/*
 * $Id: EditVerbNetSelectionRestrictionCommand.java,v 1.1 2009/02/09 10:12:31 rregan Exp $
 * Copyright (c) 2009 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.nlp.dictionary.command;

import edu.harvard.fas.rregan.command.Command;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetRoleRef;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestriction;
import edu.harvard.fas.rregan.nlp.dictionary.VerbNetSelectionRestrictionType;

/**
 * @author ron
 */
public interface EditVerbNetSelectionRestrictionCommand extends Command {

	public void setVerbNetRoleRef(VerbNetRoleRef roleRef);

	public void setVerbNetSelectionRestrictionType(VerbNetSelectionRestrictionType selResType);

	public void setInclude(String include);

	public void setVerbNetSelectionRestriction(VerbNetSelectionRestriction selRes);

	public VerbNetSelectionRestriction getVerbNetSelectionRestriction();

	public VerbNetRoleRef getVerbNetRoleRef();
}
