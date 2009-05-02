/*
 * $Id: EditVerbNetSelectionRestrictionCommand.java,v 1.1 2009/02/09 10:12:31 rregan Exp $
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
