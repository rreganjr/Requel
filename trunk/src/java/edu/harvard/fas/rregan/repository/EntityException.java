/*
 * $Id: EntityException.java,v 1.2 2009/02/17 11:50:50 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.repository;

import edu.harvard.fas.rregan.requel.RequelException;

/**
 * @author ron
 */
public class EntityException extends RequelException {
	static final long serialVersionUID = 0;

	private final Class<?> entityType;
	private final Object entity;
	private final String[] entityPropertyNames;
	private final Object[] entityPropertyValues;
	private final EntityExceptionActionType actionType;
	private final boolean staleEntity;

	protected static String MSG_UKNOWN_PROBLEM = "An unexpected problem occured %s the %s concerning %s";
	protected static String MSG_UNIQUENESS_CONFICT = "The %s conflicts with an existing %s";

	/**
	 * Create an exception for an unexpected problem (it doesn't have a specific
	 * exception to describe it.
	 * 
	 * @param cause
	 * @param entityType
	 * @param entity
	 * @param actionType
	 * @return
	 */
	public static EntityException forUnknownProblem(Throwable cause, Class<?> entityType,
			Object entity, EntityExceptionActionType actionType) {
		return forUnknownProblem(cause, entityType, entity, new String[] {}, new Object[] {},
				actionType);
	}

	/**
	 * Create an exception for an unexpected problem (it doesn't have a specific
	 * exception to describe it.
	 * 
	 * @param cause
	 * @param entityType
	 * @param entity
	 * @param entityPropertyName
	 * @param entityPropertyValue
	 * @param actionType
	 * @return
	 */
	public static EntityException forUnknownProblem(Throwable cause, Class<?> entityType,
			Object entity, String entityPropertyName, Object entityPropertyValue,
			EntityExceptionActionType actionType) {
		return forUnknownProblem(cause, entityType, entity, new String[] { entityPropertyName },
				new Object[] { entityPropertyValue }, actionType);
	}

	/**
	 * Create an exception for an unexpected problem (it doesn't have a specific
	 * exception to describe it.
	 * 
	 * @param cause
	 * @param entityType
	 * @param entity
	 * @param entityPropertyNames
	 * @param entityPropertyValues
	 * @param actionType
	 * @return
	 */
	public static EntityException forUnknownProblem(Throwable cause, Class<?> entityType,
			Object entity, String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType) {
		StringBuilder propertyInfo = new StringBuilder();
		if ((entityPropertyNames != null) && (entityPropertyNames.length > 0)) {
			for (int i = 0; i < entityPropertyNames.length; i++) {
				propertyInfo.append(entityPropertyNames[i]);
				propertyInfo.append(" of '");
				propertyInfo.append(entityPropertyNames[i]);
				propertyInfo.append("'");
				if (i < entityPropertyNames.length + 1) {
					propertyInfo.append(" and ");
				}
			}
		} else {
			propertyInfo.append("unknown property values.");
		}
		return new EntityException(cause, entityType, entity, entityPropertyNames,
				entityPropertyValues, actionType, MSG_UKNOWN_PROBLEM, actionType.name(),
				(entityType != null ? entityType.getSimpleName() : "<unknown>"), propertyInfo
						.toString());
	}

	/**
	 * @param entityType
	 * @param entity
	 * @param propertyName
	 * @param actionType
	 * @return
	 */
	public static EntityException uniquenessConflict(Class<?> entityType, Object entity,
			String propertyName, EntityExceptionActionType actionType) {
		return new EntityException(entityType, entity, new String[] { propertyName }, null,
				actionType, MSG_UNIQUENESS_CONFICT, propertyName, (entityType != null ? entityType
						.getSimpleName() : "<unknown>"));
	}

	/**
	 * @param cause
	 * @param msg
	 * @return
	 */
	public static EntityException uniquenessConflict(Throwable cause, String msg) {
		return new EntityException(cause, null, null, new String[] {}, new Object[] {},
				EntityExceptionActionType.Updating, MSG_PRE_GENERATED, msg);
	}

	protected EntityException(Class<?> entityType, Object entity, String[] entityPropertyNames,
			Object[] entityPropertyValues, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		this(entityType, entity, entityPropertyNames, entityPropertyValues, actionType, false,
				format, messageArgs);
	}

	protected EntityException(Class<?> entityType, Object entity, String entityPropertyName,
			Object entityPropertyValue, EntityExceptionActionType actionType, String format,
			Object... messageArgs) {
		this(entityType, entity, new String[] { entityPropertyName },
				new Object[] { entityPropertyValue }, actionType, false, format, messageArgs);
	}

	/**
	 * @param entityType
	 * @param entity
	 * @param entityPropertyName
	 * @param entityPropertyValues
	 * @param actionType
	 * @param staleEntity
	 * @param format
	 * @param messageArgs
	 */
	protected EntityException(Class<?> entityType, Object entity, String[] entityPropertyNames,
			Object[] entityPropertyValues, EntityExceptionActionType actionType,
			boolean staleEntity, String format, Object... messageArgs) {
		super(format, messageArgs);
		this.entityType = entityType;
		this.entity = entity;
		this.entityPropertyNames = entityPropertyNames;
		this.entityPropertyValues = entityPropertyValues;
		this.actionType = actionType;
		this.staleEntity = staleEntity;
	}

	protected EntityException(Throwable cause, Class<?> entityType, Object entity,
			String entityPropertyName, Object entityPropertyValue,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		this(cause, entityType, entity, new String[] { entityPropertyName },
				new Object[] { entityPropertyValue }, actionType, format, messageArgs);
	}

	/**
	 * @param cause
	 * @param format
	 * @param args
	 */
	protected EntityException(Throwable cause, Class<?> entityType, Object entity,
			String[] entityPropertyNames, Object[] entityPropertyValues,
			EntityExceptionActionType actionType, String format, Object... messageArgs) {
		super(cause, format, messageArgs);
		this.entityType = entityType;
		this.entity = entity;
		this.entityPropertyNames = entityPropertyNames;
		this.entityPropertyValues = entityPropertyValues;
		this.actionType = actionType;
		this.staleEntity = false;
	}

	/**
	 * @return the type (class) of entity in getEntity
	 */
	public Class<?> getEntityType() {
		return entityType;
	}

	/**
	 * @return the entity (object) that caused the exception
	 */
	public Object getEntity() {
		return entity;
	}

	/**
	 * @return the name of the property on the entity that caused the exception.
	 */
	public String[] getEntityPropertyNames() {
		return entityPropertyNames;
	}

	/**
	 * @return the value of the property that caused the exception.
	 */
	public Object[] getEntityPropertyValues() {
		return entityPropertyValues;
	}

	/**
	 * @return the type of activity (create, read, update, or delete) on the
	 *         entity when the exception occured.
	 */
	public EntityExceptionActionType getActionType() {
		return actionType;
	}

	/**
	 * @return true if this exception was caused by an out of date entity.
	 */
	public boolean isStaleEntity() {
		return staleEntity;
	}
}
