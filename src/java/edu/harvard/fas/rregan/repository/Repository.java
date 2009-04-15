package edu.harvard.fas.rregan.repository;


/**
 * A generic set of methods common to all repositories.
 * 
 * @author ron
 */
public interface Repository {

	/**
	 * add an object to the repository.
	 * 
	 * @param <T>
	 * @param entity -
	 *            the object to persist
	 * @return an up to date copy of the entity.
	 * @throws EntityException
	 */
	public <T> T persist(T entity) throws EntityException;

	/**
	 * take a previously persisted object and update its persisted copy in the
	 * repository with the supplied copy and return the latest version.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T merge(T entity) throws EntityException;

	/**
	 * takes a previously persisted object and returns the latest
	 * version from the database.
	 * 
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T get(T entity) throws EntityException;

	/**
	 * takes a persistent object and initializes any lazy loaded properties.
	 * @param <T>
	 * @param entity
	 * @return
	 * @throws EntityException
	 */
	public <T> T initialize(T entity) throws EntityException;

	/**
	 * remove the supplied object from the repository.
	 * 
	 * @param entity
	 * @throws EntityException
	 */
	public void delete(Object entity) throws EntityException;
	
	/**
	 * force the repository to sync up any pending work, without actually ending a transaction.
	 * @throws EntityException
	 */
	public void flush() throws EntityException;
	
}
