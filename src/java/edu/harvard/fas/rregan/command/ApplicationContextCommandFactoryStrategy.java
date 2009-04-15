/*
 * $Id: ApplicationContextCommandFactoryStrategy.java,v 1.1 2008/12/13 00:40:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.command;

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
	 * @see edu.harvard.fas.rregan.command.CommandFactoryStrategy#newInstance(java.lang.Class)
	 */
	@Override
	public Command newInstance(Class<? extends Command> commandType) {
		return (Command) getApplicationContext().getAutowireCapableBeanFactory().createBean(
				commandType);
	}

}
