/*
 * $Id: JAXBAnnotationGroupedByPatcher.java,v 1.3 2009/01/07 09:50:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.annotation.impl.AbstractAnnotation;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

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
			throw new SAXException2(e);
		}
	}
}
