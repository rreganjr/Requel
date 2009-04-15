package edu.harvard.fas.rregan.requel.ui;

import edu.harvard.fas.rregan.command.CommandFactory;
import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.uiframework.navigation.event.EventDispatcher;

/**
 * @author ron
 */
public abstract class AbstractRequelCommandController extends AbstractRequelController {

	private CommandFactory commandFactory;
	private CommandHandler commandHandler;

	/**
	 * @param eventDispatcher
	 * @param commandFactory
	 * @param commandHandler
	 */
	protected AbstractRequelCommandController(EventDispatcher eventDispatcher,
			CommandFactory commandFactory, CommandHandler commandHandler) {
		super(eventDispatcher);
		setCommandFactory(commandFactory);
		setCommandHandler(commandHandler);
	}

	/**
	 * @param commandFactory
	 * @param commandHandler
	 */
	protected AbstractRequelCommandController(CommandFactory commandFactory,
			CommandHandler commandHandler) {
		super();
		setCommandFactory(commandFactory);
		setCommandHandler(commandHandler);
	}

	protected <T> T getCommandFactory() {
		return (T) commandFactory;
	}

	protected void setCommandFactory(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}
}
