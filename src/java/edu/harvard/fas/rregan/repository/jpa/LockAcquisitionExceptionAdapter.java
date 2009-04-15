/*
 * $Id: LockAcquisitionExceptionAdapter.java,v 1.1 2008/12/13 00:41:13 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.repository.jpa;

import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.EntityExceptionAdapter;
import edu.harvard.fas.rregan.requel.EntityLockException;

/**
 * @author ron
 */
public class LockAcquisitionExceptionAdapter implements EntityExceptionAdapter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.fas.rregan.requel.EntityExceptionAdapter#convert(java.lang.Throwable,
	 *      java.lang.Class, java.lang.Object,
	 *      edu.harvard.fas.rregan.requel.EntityExceptionActionType)
	 */
	@Override
	public EntityLockException convert(Throwable original, Class<?> entityType, Object entity,
			EntityExceptionActionType actionType) {
		return EntityLockException.deadlock(entityType, entity, actionType);
	}

}
