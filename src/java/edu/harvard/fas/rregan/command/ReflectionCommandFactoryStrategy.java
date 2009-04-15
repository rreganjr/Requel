/*
 * $Id: ReflectionCommandFactoryStrategy.java,v 1.1 2008/12/13 00:41:03 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

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
	 * @see edu.harvard.fas.rregan.command.CommandFactoryStrategy#newInstance(java.lang.Class)
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
