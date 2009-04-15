/*
 * $Id: RetryOnLockFailuresCommandHandler.java,v 1.4 2009/03/22 11:08:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.command;

import java.lang.reflect.Method;

import javax.persistence.OptimisticLockException;

import org.apache.log4j.Logger;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.Repository;
import edu.harvard.fas.rregan.requel.EntityLockException;
import edu.harvard.fas.rregan.requel.EntityValidationException;

/**
 * A command handler that wraps another command handler and traps exceptions
 * related to locking and re-executes the original command in the wrapped
 * handler.
 * 
 * @author ron
 */
public class RetryOnLockFailuresCommandHandler implements CommandHandler {
	private static final Logger log = Logger.getLogger(RetryOnLockFailuresCommandHandler.class);

	private final CommandHandler commandHandler;
	private final Repository repository;

	/**
	 * @param commandHandler
	 * @param repository
	 */
	public RetryOnLockFailuresCommandHandler(CommandHandler commandHandler, Repository repository) {
		this.commandHandler = commandHandler;
		this.repository = repository;
	}

	public <T extends Command> T execute(T command) throws Exception {
		int retries = 0;
		Throwable[] thrown = new Throwable[3];
		while (true) {
			try {
				T returnCommand = commandHandler.execute(command);
				if ((retries > 0) && log.isInfoEnabled()) {
					StringBuilder sb = new StringBuilder();
					sb.append(getCommandInterfaceName(command.getClass()));
					sb.append(" succeeded after retry: ");
					for (int i = 0; i < retries; i++) {
						sb.append("thrown[");
						sb.append(i);
						sb.append("]: ");
						sb.append(thrown[i]);
						sb.append(" ");
					}
					log.info(sb.toString());
				}
				return returnCommand;
			} catch (EntityValidationException e) {
				// the command failed during valdation, don't retry
				throw e;
			} catch (EntityLockException e) {
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					log.info(getCommandInterfaceName(command.getClass())
							+ " failed due to a failure to get a lock, retrying attempt: "
							+ retries);
				}
			} catch (EntityException e) {
				// the command failed during valdation, don't retry
				throw e;
			} catch (LockAcquisitionException e) {
				// must catch hibernate and/or spring exceptions
				// because they are thrown from the commit and not
				// wrapped by the exception adapters on the repositories
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					log.info(getCommandInterfaceName(command.getClass())
							+ " failed due to a failure to get a lock: "
							+ e.getClass().getSimpleName() + ", retrying attempt: " + retries);
				}
			} catch (CannotAcquireLockException e) {
				// must catch hibernate and/or spring exceptions
				// because they are thrown from the commit and not
				// wrapped by the exception adapters on the repositories
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					log.info(getCommandInterfaceName(command.getClass())
							+ " failed due to a failure to get a lock: "
							+ e.getClass().getSimpleName() + ", retrying attempt: " + retries);
				}
			} catch (OptimisticLockException e) {
				// must catch hibernate and/or spring exceptions
				// because they are thrown from the commit and not
				// wrapped by the exception adapters on the repositories

				// TODO: this is mostly likely caused by the object being edited
				// by the command as the command reloads indirect objects.
				// Ideally we could check to see if the object properties being
				// changed were changed in another transaction, and if not
				// reload the object and rerun the command, otherwise fail
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					Object entity = e.getEntity();
					if (entity != null) {
						log.info(getCommandInterfaceName(command.getClass())
								+ " failed due to a stale object: " + entity
								+ ", retrying attempt: " + retries);

						Object updatedEntity = repository.get(entity);
						// figure out what changed

						// get the setter on the command and pass the updated
						// object
						for (Method method : getCommandInterface(command.getClass())
								.getDeclaredMethods()) {
							log.info(method.toGenericString());
							if ((method.getParameterTypes().length == 1)
									&& method.getParameterTypes()[0].isAssignableFrom(entity
											.getClass())) {
								method.setAccessible(true);
								method.invoke(command, updatedEntity);
								break;
							}
						}
					} else {
						log.error(e + " the entity was null in the exception and could "
								+ "not be recovered.", e);
					}
				}
			} catch (StaleObjectStateException e) {
				// must catch hibernate and/or spring exceptions
				// because they are thrown from the commit and not
				// wrapped by the exception adapters on the repositories

				// TODO: this is mostly likely caused by the object being edited
				// by the command as the command reloads indirect objects.
				// Ideally we could check to see if the object properties being
				// changed were changed in another transaction, and if not
				// reload the object and rerun the command, otherwise fail
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					log.info(getCommandInterfaceName(command.getClass())
							+ " failed due to a stale object: " + e.getEntityName() + "#"
							+ e.getIdentifier() + ", retrying attempt: " + retries);
				}
			} catch (HibernateOptimisticLockingFailureException e) {
				// must catch hibernate and/or spring exceptions
				// because they are thrown from the commit and not
				// wrapped by the exception adapters on the repositories

				// TODO: this is mostly likely caused by the object being edited
				// by the command as the command reloads indirect objects.
				// Ideally we could check to see if the object properties being
				// changed were changed in another transaction, and if not
				// reload the object and rerun the command, otherwise fail
				thrown[retries] = e;
				retries++;
				if (retries > 2) {
					throw e;
				} else {
					log.info(getCommandInterfaceName(command.getClass())
							+ " failed due to a stale object: " + e.getPersistentClassName() + "#"
							+ e.getIdentifier() + ", retrying attempt: " + retries);
				}
			} catch (Exception e) {
				log.error("unhandled exception in command handler: " + e, e);
				throw e;
			}
		}
	}

	private Class<?> getCommandInterface(Class<?> commandClass) {
		while (Command.class.isAssignableFrom(commandClass)) {
			for (Class<?> face : commandClass.getInterfaces()) {
				if (Command.class.isAssignableFrom(face)) {
					return face;
				}
			}
			commandClass = commandClass.getSuperclass();
		}
		return null;
	}

	private String getCommandInterfaceName(Class<?> commandClass) {
		Class<?> interfaceClass = getCommandInterface(commandClass);
		if (interfaceClass != null) {
			return interfaceClass.getSimpleName();
		}
		return "<non-command>";
	}
}
