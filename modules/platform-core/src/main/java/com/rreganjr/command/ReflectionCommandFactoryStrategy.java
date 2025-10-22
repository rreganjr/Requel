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
package com.rreganjr.command;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author ron
 */
public class ReflectionCommandFactoryStrategy implements CommandFactoryStrategy {

	/**
	 * TODO: add parameters to pass into the command through the constructor
	 */
	public ReflectionCommandFactoryStrategy() {
	}

	/**
	 * @see com.rreganjr.command.CommandFactoryStrategy#newInstance(java.lang.Class)
	 */
	public Command newInstance(Class<? extends Command> commandType) {
		try {
			return newInstance(commandType, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param args
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	protected Command newInstance(Class<? extends Command> commandType, List<Object> args)
			throws NoSuchMethodException, InvocationTargetException, InstantiationException,
			IllegalAccessException {
		Class<?> parameterTypes[] = new Class<?>[args.size()];
		for (int i = 0; i < args.size(); i++) {
			parameterTypes[i] = args.get(i).getClass();
		}
		Constructor<? extends Command> constructor = commandType.getConstructor(parameterTypes);
		return constructor.newInstance(args.toArray());
	}

}
