/*
 * $Id: UITypeDisplayHint.java,v 1.1 2008/02/15 21:41:59 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.uiframework.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UITypeDisplayHint {
	/**
	 * The name of the property to display when representing an
	 * object as a single cell view, such as a table cell or tree
	 * cell.
	 * 
	 * @return
	 */
	String targetProperty() default "";

}
