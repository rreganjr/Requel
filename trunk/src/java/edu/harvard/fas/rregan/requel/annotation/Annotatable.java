/*
 * $Id: Annotatable.java,v 1.4 2008/08/08 10:02:00 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.annotation;

import java.util.Set;

/**
 * Represents something that can have an Annotation such as a note, or issue.
 * 
 * @author ron
 */
public interface Annotatable {
	/**
	 * @return THe annotations attached to this object.
	 */
	public Set<Annotation> getAnnotations();
}
