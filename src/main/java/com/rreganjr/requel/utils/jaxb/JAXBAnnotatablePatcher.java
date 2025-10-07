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

import org.xml.sax.SAXException;

import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.Patcher;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;

/**
 * This is used to add an annotatable to all the annotations after import.
 * 
 * @author ron
 */
public class JAXBAnnotatablePatcher implements Patcher {

	private final Annotatable annotatable;

	/**
	 * @param annotatable
	 */
	public JAXBAnnotatablePatcher(Annotatable annotatable) {
		this.annotatable = annotatable;
	}

	@Override
	public void run() throws SAXException {
		try {
			for (Annotation annotation : annotatable.getAnnotations()) {
				annotation.getAnnotatables().add(annotatable);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}
}
