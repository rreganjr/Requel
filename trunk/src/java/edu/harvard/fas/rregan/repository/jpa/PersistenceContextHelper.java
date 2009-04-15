/*
 * $Id: PersistenceContextHelper.java,v 1.2 2009/01/22 10:36:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.repository.jpa;

import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ron
 */
@Component("persistenceContextHelper")
@Scope("singleton")
public class PersistenceContextHelper {
	private static final Logger log = Logger.getLogger(PersistenceContextHelper.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * 
	 */
	public PersistenceContextHelper() {
	}

	/**
	 * @param entityHolder
	 * @param domainObjectWrapper
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Object invokeInTransaction(EntityProxyInterceptor entityHolder,
			DomainObjectWrapper domainObjectWrapper, Method method, Object... args)
			throws Throwable {
		Object cachedEntity = entityHolder.getEntity();
		Object refreshedEntity = AbstractJpaRepository.attach(entityManager, cachedEntity);
		entityHolder.setEntity(refreshedEntity);
		StringBuffer sb = null;
		if (log.isDebugEnabled()) {
			sb = new StringBuffer();
			sb.append(method.getName());
			sb.append("(");
			for (int i = 0; (args != null) && (i < args.length); i++) {
				if (i != 0) {
					sb.append(", ");
				}
				sb.append(args[i]);
			}
			sb.append(")");
		}
		Object rVal = method.invoke(refreshedEntity, args);
		if (log.isDebugEnabled() && (sb != null)) {
			if (!method.getReturnType().equals(void.class)) {
				sb.append(" -> ");
				sb.append(rVal);
			}
			log.debug(sb.toString());
		}
		return domainObjectWrapper.wrapPersistentEntities(rVal);
	}
}