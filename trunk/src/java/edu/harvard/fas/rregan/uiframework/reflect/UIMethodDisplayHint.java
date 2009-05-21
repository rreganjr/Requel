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
package edu.harvard.fas.rregan.uiframework.reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ron
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIMethodDisplayHint {
	public static final int IGNORE = 0;
	public static final int SHORT = 1;
	public static final int LONG = 2;
	public static final int SHORT_OR_LONG = (SHORT | LONG);

	/**
	 * An explicit property name for methods that don't follow JavaBean naming
	 * conventions.
	 * 
	 * @return
	 */
	String propertyName() default "";

	/**
	 * An alternate label to use instead of mangling the method name to generate
	 * a label for the property.
	 * 
	 * @return
	 */
	String label() default "";

	/**
	 * Level where this property is appropriate to show: IGNORE - never include
	 * this property SHORT - include this property in short displays LONG -
	 * include this property in long displays SHORT_OR_LONG - always include
	 * this property
	 * 
	 * @return
	 */
	int displayLevel() default SHORT_OR_LONG;

	/**
	 * Set this to true if this property should be used in constraining a
	 * search.
	 * 
	 * @return
	 */
	boolean searchable() default false;

	/**
	 * Use the supplied pattern to format the value of the target object for the
	 * display value.
	 * 
	 * @return
	 */
	String targetPattern() default "";

	/**
	 * Use the suppled property of the target object as the display value.
	 * 
	 * @return
	 */
	String targetProperty() default "";

}
