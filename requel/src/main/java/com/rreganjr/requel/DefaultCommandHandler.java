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
package com.rreganjr.requel;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The default handler executes the command in a transaction locally to the
 * process that supplied the command.
 * 
 * @author ron
 */
public class DefaultCommandHandler implements CommandHandler {
	@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = {NoSuchEntityException.class})
	public <T extends Command> T execute(T command) throws Exception {
		command.execute();
		return command;
	}
}
