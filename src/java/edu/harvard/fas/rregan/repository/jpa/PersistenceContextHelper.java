/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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