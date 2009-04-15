/*
 * $Id: UIMethodDisplayHint.java,v 1.1 2008/02/15 21:42:02 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
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
	 * An explicit property name for methods that don't follow
	 * JavaBean naming conventions.
	 * 
	 * @return
	 */
	String propertyName() default "";
	
	/**
	 * An alternate label to use instead of mangling the method name to generate
	 * a label for the property.
	 * @return
	 */
    String label() default "";
    
    /**
     * Level where this property is appropriate to show:
     * IGNORE - never include this property
     * SHORT - include this property in short displays
     * LONG - include this property in long displays
     * SHORT_OR_LONG - always include this property
     * @return
     */
    int displayLevel() default SHORT_OR_LONG;
    
    /**
     * Set this to true if this property should be used in constraining a search.
     * 
     * @return
     */
    boolean searchable() default false;

    /**
     * Use the supplied pattern to format the value of the
     * target object for the display value.
     * @return
     */
	String targetPattern() default "";
	
	/**
	 * Use the suppled property of the target object as the
	 * display value.
	 * 
	 * @return
	 */
	String targetProperty() default "";

}
