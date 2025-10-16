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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author ron
 */
@Component("commandFactoryStrategy")
@Scope("prototype")
public class ApplicationContextCommandFactoryStrategy implements CommandFactoryStrategy,
		ApplicationContextAware {

	private ApplicationContext applicationContext;

	protected ApplicationContextCommandFactoryStrategy() {
		super();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * @return
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @see com.rreganjr.command.CommandFactoryStrategy#newInstance(java.lang.Class)
	 */
	@Override
	public Command newInstance(Class<? extends Command> commandType) {
		return (Command) getApplicationContext().getAutowireCapableBeanFactory().createBean(
				commandType);
	}

}
