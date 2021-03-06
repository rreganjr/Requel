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
package com.rreganjr.requel.utils.jaxb;

import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import org.xml.sax.SAXException;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * This is used to set the grouping object
 * 
 * @author ron
 */
public class JAXBAnnotationGroupedByPatcher implements Patcher {

	private final Annotation annotation;
	private final Annotatable annotatable;

	/**
	 * @param annotation
	 * @param annotatable
	 */
	public JAXBAnnotationGroupedByPatcher(Annotation annotation, Annotatable annotatable) {
		this.annotatable = annotatable;
		this.annotation = annotation;
	}

	@Override
	public void run() throws SAXException {
		try {
			// annotations expect the annotatable object and
			// the group object, which should be the
			// project.
			if (annotatable instanceof ProjectOrDomain) {
				((AbstractAnnotation) annotation).setGroupingObject(annotatable);
			} else if (annotatable instanceof ProjectOrDomainEntity) {
				((AbstractAnnotation) annotation)
						.setGroupingObject(((ProjectOrDomainEntity) annotatable)
								.getProjectOrDomain());
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}
}
