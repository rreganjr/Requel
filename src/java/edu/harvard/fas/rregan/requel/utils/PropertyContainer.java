/*
 * $Id: PropertyContainer.java,v 1.2 2008/02/15 21:59:08 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils;

import java.util.Map;

public interface PropertyContainer {

    public Map<String, Object> getProperties();

    public void setProperties(Map<String, Object> properties);

    public void addProperties(Map<String, Object> properties);

    public Object getProperty(String key);

    public Object getProperty(String key, Object defaultValue);

    public void setProperty(String key, Object value);

}
