/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.user.command;

import edu.harvard.fas.rregan.command.CommandFactory;

/**
 * @author ron
 */
public interface UserCommandFactory extends CommandFactory {

	/**
	 * @return a new LoginCommand for authenticating a user of the system.
	 */
	public LoginCommand newLoginCommand();

	/**
	 * @return a new EditUserCommand for creating or editing a user of the
	 *         system.
	 */
	public EditUserCommand newEditUserCommand();

}
