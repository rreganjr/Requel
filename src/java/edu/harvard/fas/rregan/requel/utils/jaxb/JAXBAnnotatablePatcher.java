/*
 * $Id: JAXBAnnotatablePatcher.java,v 1.2 2009/01/07 09:50:37 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;

/**
 * This is used to add an annotatable to all the annotations after import.
 * 
 * @author ron
 */
public class JAXBAnnotatablePatcher implements Patcher {

	private final Annotatable annotatable;

	/**
	 * @param annotation
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
			throw new SAXException2(e);
		}
	}
}
